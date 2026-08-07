package it.legacynetwork.chickenwars.effect;

import it.legacynetwork.chickenwars.upgrade.TeamUpgradeDefinition;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeState;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeType;
import it.legacynetwork.chickenwars.upgrade.UpgradeLevel;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Rigenerazione attiva soltanto dentro la propria base.
 *
 * <p>Il servizio conserva un marcatore per ogni giocatore a cui l'effetto e'
 * stato davvero applicato: e' quel marcatore, e non la semplice presenza di una
 * rigenerazione, a decidere la rimozione. Una pozione di rigenerazione bevuta
 * dal giocatore non viene quindi mai cancellata dal Heal Pool.</p>
 *
 * <p>Nessuna scansione periodica di tutti i giocatori: il servizio viene
 * interrogato dal listener di movimento e dal ciclo di partita, sempre con un
 * giocatore gia' individuato.</p>
 */
public final class HealPoolService {

    /**
     * Marcatore di un effetto applicato dal Heal Pool.
     */
    private static final class Marker {

        private final String teamKey;
        private final PotionEffectType type;

        Marker(String teamKey, PotionEffectType type) {
            this.teamKey = teamKey;
            this.type = type;
        }
    }

    private final TeamUpgradeService upgrades;
    private final EffectAdapter effects;
    private final Map<UUID, Marker> markers = new LinkedHashMap<UUID, Marker>();

    public HealPoolService(TeamUpgradeService upgrades, EffectAdapter effects) {
        if (upgrades == null || effects == null) {
            throw new IllegalArgumentException("Heal Pool incompleto");
        }
        this.upgrades = upgrades;
        this.effects = effects;
    }

    private static String key(String arenaId, String teamId) {
        return String.valueOf(arenaId).toLowerCase(Locale.ROOT) + '/'
                + String.valueOf(teamId).toLowerCase(Locale.ROOT);
    }

    /**
     * Allinea l'effetto alla posizione corrente del giocatore.
     *
     * <p>Il rinnovo e' volutamente incondizionato quando il giocatore resta
     * dentro: l'effetto configurato dura pochi secondi e va mantenuto vivo dal
     * movimento e dal ciclo di partita. L'adapter sostituisce sempre l'effetto
     * esistente, quindi non si accumula.</p>
     *
     * @param inside   indica se il giocatore si trova nella propria base
     * @param eligible indica se il giocatore e' un membro vivo della squadra
     * @return {@code true} se l'effetto risulta attivo dopo questa chiamata
     */
    public synchronized boolean update(String arenaId, String teamId,
                                       UUID playerId, boolean inside,
                                       boolean eligible) {
        if (playerId == null) {
            return false;
        }
        if (!inside || !eligible || arenaId == null || teamId == null) {
            forget(playerId);
            return false;
        }

        TeamUpgradeDefinition definition =
                upgrades.getCatalog().getTeamUpgrade(TeamUpgradeType.HEAL_POOL);
        if (definition == null || definition.getEffect() == null) {
            forget(playerId);
            return false;
        }
        TeamUpgradeState state = upgrades.peekState(arenaId, teamId);
        int level =
                state == null ? 0 : state.getLevel(TeamUpgradeType.HEAL_POOL);
        UpgradeLevel resolved = definition.getLevel(level);
        if (resolved == null) {
            forget(playerId);
            return false;
        }

        int duration = resolved.getDurationTicks() > 0
                ? resolved.getDurationTicks()
                : TeamEffectService.SUSTAINED_DURATION_TICKS;
        PotionEffectType type = definition.getEffect();

        Marker previous = markers.get(playerId);
        if (previous != null && !previous.type.equals(type)) {
            effects.clear(playerId, previous.type);
        }
        effects.apply(playerId, type, duration,
                Math.max(0, resolved.getAmplifier()));
        markers.put(playerId, new Marker(key(arenaId, teamId), type));
        return true;
    }

    /**
     * Indica se il servizio ha un effetto attivo su questo giocatore.
     */
    public synchronized boolean isActive(UUID playerId) {
        return playerId != null && markers.containsKey(playerId);
    }

    /**
     * Rimuove l'effetto applicato dal Heal Pool, se presente.
     *
     * @return {@code true} se c'era davvero un effetto da rimuovere
     */
    public synchronized boolean forget(UUID playerId) {
        Marker removed = playerId == null ? null : markers.remove(playerId);
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
        for (UUID playerId : new ArrayList<UUID>(markers.keySet())) {
            Marker marker = markers.get(playerId);
            if (marker != null && marker.teamKey.startsWith(prefix)) {
                markers.remove(playerId);
                effects.clear(playerId, marker.type);
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
        int removed = markers.size();
        for (Map.Entry<UUID, Marker> entry : markers.entrySet()) {
            effects.clear(entry.getKey(), entry.getValue().type);
        }
        markers.clear();
        return removed;
    }

    public synchronized int getTrackedPlayers() {
        return markers.size();
    }
}
