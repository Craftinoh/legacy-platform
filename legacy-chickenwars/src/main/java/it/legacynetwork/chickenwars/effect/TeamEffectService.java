package it.legacynetwork.chickenwars.effect;

import it.legacynetwork.chickenwars.upgrade.TeamUpgradeDefinition;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeState;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeType;
import it.legacynetwork.chickenwars.upgrade.UpgradeLevel;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Effetti di squadra permanenti, oggi il solo Haste.
 *
 * <p>Il livello autorevole resta in {@link TeamUpgradeState}: qui si conserva
 * unicamente il marcatore di cio' che e' stato realmente applicato, per poterlo
 * rimuovere a fine partita e per non riapplicarlo inutilmente.</p>
 *
 * <p>Non esiste alcun task per giocatore: l'effetto viene rinnovato dal ciclo
 * gia' esistente della partita tramite {@link #refresh(String, String,
 * Collection)}, con una durata piu' lunga dell'intervallo di rinnovo.</p>
 */
public final class TeamEffectService {

    /**
     * Durata usata quando la configurazione dichiara l'effetto permanente.
     *
     * <p>Venti secondi: abbondantemente superiori all'intervallo di rinnovo,
     * quindi il giocatore non vede mai l'effetto spegnersi, ma non cosi' lunghi
     * da sopravvivere a una partita conclusa in modo anomalo.</p>
     */
    public static final int SUSTAINED_DURATION_TICKS = 400;

    /**
     * Cio' che risulta applicato a un giocatore in un dato momento.
     */
    private static final class Applied {

        private final String teamKey;
        private final PotionEffectType type;
        private final int amplifier;

        Applied(String teamKey, PotionEffectType type, int amplifier) {
            this.teamKey = teamKey;
            this.type = type;
            this.amplifier = amplifier;
        }

        boolean matches(String otherKey, PotionEffectType otherType,
                        int otherAmplifier) {
            return teamKey.equals(otherKey) && type.equals(otherType)
                    && amplifier == otherAmplifier;
        }
    }

    private final TeamUpgradeService upgrades;
    private final EffectAdapter effects;
    private final Map<UUID, Applied> applied = new LinkedHashMap<UUID, Applied>();

    public TeamEffectService(TeamUpgradeService upgrades,
                             EffectAdapter effects) {
        if (upgrades == null || effects == null) {
            throw new IllegalArgumentException("Servizio effetti incompleto");
        }
        this.upgrades = upgrades;
        this.effects = effects;
    }

    private static String key(String arenaId, String teamId) {
        return String.valueOf(arenaId).toLowerCase(Locale.ROOT) + '/'
                + String.valueOf(teamId).toLowerCase(Locale.ROOT);
    }

    /**
     * Allinea un singolo giocatore al livello posseduto dalla sua squadra.
     *
     * <p>Chiamata a spawn, respawn, reconnect e subito dopo un acquisto. Se
     * l'effetto risulta gia' applicato con lo stesso amplificatore non viene
     * toccato: due reconnect consecutivi producono quindi lo stesso stato di
     * uno solo.</p>
     *
     * @param eligible indica se il giocatore ha diritto all'effetto: spettatori
     *                 ed eliminati vanno passati come non eleggibili
     * @return {@code true} se questa chiamata ha cambiato qualcosa
     */
    public synchronized boolean apply(String arenaId, String teamId,
                                      UUID playerId, boolean eligible) {
        return set(arenaId, teamId, playerId, eligible, false);
    }

    /**
     * Rinnova l'effetto anche quando risulta gia' applicato.
     *
     * <p>Serve al ciclo di partita per mantenere vivo un effetto dichiarato
     * permanente senza registrare task aggiuntivi.</p>
     *
     * @param eligibleMembers membri vivi della squadra
     * @return il numero di giocatori raggiunti
     */
    public synchronized int refresh(String arenaId, String teamId,
                                    Collection<UUID> eligibleMembers) {
        if (eligibleMembers == null) {
            return 0;
        }
        int touched = 0;
        for (UUID member : eligibleMembers) {
            if (set(arenaId, teamId, member, true, true)) {
                touched++;
            }
        }
        return touched;
    }

    private boolean set(String arenaId, String teamId, UUID playerId,
                        boolean eligible, boolean forced) {
        if (playerId == null) {
            return false;
        }
        if (!eligible || arenaId == null || teamId == null) {
            return forget(playerId);
        }

        TeamUpgradeDefinition definition =
                upgrades.getCatalog().getTeamUpgrade(TeamUpgradeType.HASTE);
        if (definition == null || definition.getEffect() == null) {
            return forget(playerId);
        }
        TeamUpgradeState state = upgrades.peekState(arenaId, teamId);
        int level = state == null ? 0 : state.getLevel(TeamUpgradeType.HASTE);
        UpgradeLevel resolved = definition.getLevel(level);
        if (resolved == null) {
            // Nessun livello acquistato: l'eventuale effetto residuo va tolto.
            return forget(playerId);
        }

        String teamKey = key(arenaId, teamId);
        PotionEffectType type = definition.getEffect();
        int amplifier = Math.max(0, resolved.getAmplifier());
        int duration = resolved.getDurationTicks() > 0
                ? resolved.getDurationTicks() : SUSTAINED_DURATION_TICKS;

        Applied current = applied.get(playerId);
        if (!forced && current != null
                && current.matches(teamKey, type, amplifier)) {
            return false;
        }
        if (current != null && !current.type.equals(type)) {
            effects.clear(playerId, current.type);
        }
        effects.apply(playerId, type, duration, amplifier);
        applied.put(playerId, new Applied(teamKey, type, amplifier));
        return true;
    }

    /**
     * Rimuove l'effetto applicato a un giocatore che lascia la partita.
     *
     * @return {@code true} se c'era davvero qualcosa da rimuovere
     */
    public synchronized boolean forget(UUID playerId) {
        Applied removed = playerId == null ? null : applied.remove(playerId);
        if (removed == null) {
            return false;
        }
        effects.clear(playerId, removed.type);
        return true;
    }

    /**
     * Rimuove gli effetti di tutti i giocatori di un'arena.
     *
     * @return il numero di giocatori ripuliti
     */
    public synchronized int clearArena(String arenaId) {
        if (arenaId == null) {
            return 0;
        }
        String prefix = arenaId.toLowerCase(Locale.ROOT) + '/';
        int removed = 0;
        for (UUID playerId : new ArrayList<UUID>(applied.keySet())) {
            Applied entry = applied.get(playerId);
            if (entry != null && entry.teamKey.startsWith(prefix)) {
                applied.remove(playerId);
                effects.clear(playerId, entry.type);
                removed++;
            }
        }
        return removed;
    }

    /**
     * Rimuove ogni effetto tracciato, allo spegnimento del plugin.
     *
     * @return il numero di giocatori ripuliti
     */
    public synchronized int clearAll() {
        int removed = applied.size();
        for (Map.Entry<UUID, Applied> entry : applied.entrySet()) {
            effects.clear(entry.getKey(), entry.getValue().type);
        }
        applied.clear();
        return removed;
    }

    /**
     * Amplificatore attualmente applicato a un giocatore.
     *
     * @return l'amplificatore, oppure {@code -1} se nessun effetto e' attivo
     */
    public synchronized int getAppliedAmplifier(UUID playerId) {
        Applied entry = playerId == null ? null : applied.get(playerId);
        return entry == null ? -1 : entry.amplifier;
    }

    /**
     * Numero di giocatori con un effetto di squadra attivo.
     */
    public synchronized int getTrackedPlayers() {
        return applied.size();
    }
}
