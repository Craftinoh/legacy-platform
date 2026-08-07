package it.legacynetwork.chickenwars.upgrade;

import it.legacynetwork.chickenwars.economy.ResourceInventory;
import it.legacynetwork.chickenwars.economy.ResourceWallet;
import it.legacynetwork.chickenwars.mode.ModeProfile;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.player.equipment.TeamEnchantProvider;
import it.legacynetwork.chickenwars.shop.ItemCost;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Stato e acquisti degli upgrade di squadra.
 *
 * <p>Custodisce l'unico {@link TeamUpgradeState} per squadra e per partita, e
 * fornisce a {@code EquipmentService} i livelli di Protection e Sharpness
 * tramite {@link TeamEnchantProvider}: l'equipaggiamento resta allineato dopo
 * respawn e reconnect senza che il chiamante conosca gli upgrade.</p>
 *
 * <p>L'acquisto riserva il livello prima di addebitare: se l'addebito fallisce
 * la riserva viene annullata, quindi nessuna risorsa puo' andare persa.</p>
 */
public final class TeamUpgradeService implements TeamEnchantProvider {

    private final Map<String, TeamUpgradeState> states =
            new LinkedHashMap<String, TeamUpgradeState>();

    private volatile UpgradeCatalog catalog = UpgradeCatalog.empty();

    public void setCatalog(UpgradeCatalog catalog) {
        this.catalog = catalog == null ? UpgradeCatalog.empty() : catalog;
    }

    public UpgradeCatalog getCatalog() {
        return catalog;
    }

    // ------------------------------------------------------------------
    // Registro degli stati
    // ------------------------------------------------------------------

    /**
     * Chiave che distingue squadre omonime di arene diverse.
     */
    static String key(String arenaId, String teamId) {
        return String.valueOf(arenaId).toLowerCase(Locale.ROOT) + '/'
                + String.valueOf(teamId).toLowerCase(Locale.ROOT);
    }

    /**
     * Stato della squadra, creato al primo accesso.
     *
     * @return lo stato, oppure {@code null} se gli identificatori mancano
     */
    public synchronized TeamUpgradeState getState(String arenaId,
                                                  String teamId) {
        if (arenaId == null || teamId == null) {
            return null;
        }
        String composite = key(arenaId, teamId);
        TeamUpgradeState state = states.get(composite);
        if (state == null) {
            state = new TeamUpgradeState(teamId);
            states.put(composite, state);
        }
        return state;
    }

    /**
     * Stato della squadra senza crearlo.
     *
     * @return lo stato, oppure {@code null} se non esiste
     */
    public synchronized TeamUpgradeState peekState(String arenaId,
                                                   String teamId) {
        if (arenaId == null || teamId == null) {
            return null;
        }
        return states.get(key(arenaId, teamId));
    }

    /**
     * Rimuove tutti gli stati di un'arena, a fine partita o allo scaricamento.
     *
     * @return il numero di squadre ripulite
     */
    public synchronized int clearArena(String arenaId) {
        if (arenaId == null) {
            return 0;
        }
        String prefix = arenaId.toLowerCase(Locale.ROOT) + '/';
        int removed = 0;
        for (String composite : new java.util.ArrayList<String>(states.keySet())) {
            if (composite.startsWith(prefix)) {
                TeamUpgradeState state = states.remove(composite);
                if (state != null) {
                    state.clear();
                    removed++;
                }
            }
        }
        return removed;
    }

    /**
     * Azzera l'intero registro, allo spegnimento del plugin.
     */
    public synchronized void clearAll() {
        for (TeamUpgradeState state : states.values()) {
            state.clear();
        }
        states.clear();
    }

    // ------------------------------------------------------------------
    // Incantesimi di squadra
    // ------------------------------------------------------------------

    @Override
    public int getProtectionLevel(PlayerSession session) {
        return levelOf(session, TeamUpgradeType.PROTECTION);
    }

    @Override
    public int getSharpnessLevel(PlayerSession session) {
        return levelOf(session, TeamUpgradeType.SHARPNESS);
    }

    private int levelOf(PlayerSession session, TeamUpgradeType type) {
        if (session == null || session.getTeamId() == null) {
            return 0;
        }
        TeamUpgradeState state =
                peekState(session.getArenaId(), session.getTeamId());
        if (state == null) {
            return 0;
        }
        TeamUpgradeDefinition definition = catalog.getTeamUpgrade(type);
        if (definition == null) {
            return 0;
        }
        int level = state.getLevel(type);
        UpgradeLevel resolved = definition.getLevel(level);
        // L'amplificatore configurato e' il livello reale dell'incantesimo.
        return resolved == null ? 0 : Math.max(0, resolved.getAmplifier() + 1);
    }

    // ------------------------------------------------------------------
    // Acquisti
    // ------------------------------------------------------------------

    /**
     * Acquista il livello successivo di un upgrade di squadra.
     *
     * @param player  acquirente
     * @param arenaId arena della partita
     * @param teamId  squadra dell'acquirente
     * @param type    upgrade richiesto
     * @param profile profilo economico della partita
     * @return l'esito, mai nullo
     */
    public UpgradeResult purchase(Player player, String arenaId, String teamId,
                                  TeamUpgradeType type, ModeProfile profile) {
        if (player == null) {
            return UpgradeResult.UNAVAILABLE;
        }
        return purchase(ResourceWallet.adapterFor(player), arenaId, teamId,
                type, profile);
    }

    /**
     * Variante deterministica dell'acquisto, verificabile senza Bukkit.
     *
     * <p>Produzione e test percorrono lo stesso codice: cambia soltanto
     * l'origine dell'inventario.</p>
     */
    public UpgradeResult purchase(ResourceInventory wallet, String arenaId,
                                  String teamId, TeamUpgradeType type,
                                  ModeProfile profile) {
        if (wallet == null || type == null) {
            return UpgradeResult.UNAVAILABLE;
        }
        if (teamId == null) {
            return UpgradeResult.NO_TEAM;
        }
        if (profile == null) {
            return UpgradeResult.NOT_IN_GAME;
        }

        TeamUpgradeDefinition definition = catalog.getTeamUpgrade(type);
        if (definition == null) {
            return UpgradeResult.UNAVAILABLE;
        }

        TeamUpgradeState state = getState(arenaId, teamId);
        // La riserva precede l'addebito: due membri simultanei non possono
        // pagare entrambi lo stesso livello.
        int reserved = state.reserveNextLevel(type,
                definition.getMaximumLevel());
        if (reserved == 0) {
            return UpgradeResult.MAX_LEVEL;
        }

        ItemCost cost = definition.getCost(reserved,
                profile.getPricingProfile());
        if (cost == null) {
            state.rollbackLevel(type, reserved);
            return UpgradeResult.NO_PRICE;
        }
        if (!withdraw(wallet, cost)) {
            state.rollbackLevel(type, reserved);
            return UpgradeResult.NOT_ENOUGH;
        }
        return UpgradeResult.SUCCESS;
    }

    /**
     * Acquista il livello successivo di un potenziamento della Gallina Reale.
     */
    public UpgradeResult purchaseRoyal(ResourceInventory wallet, String arenaId,
                                       String teamId, RoyalUpgradeType type,
                                       ModeProfile profile) {
        if (wallet == null || type == null) {
            return UpgradeResult.UNAVAILABLE;
        }
        if (teamId == null) {
            return UpgradeResult.NO_TEAM;
        }
        if (profile == null) {
            return UpgradeResult.NOT_IN_GAME;
        }

        RoyalUpgradeDefinition definition = catalog.getRoyalUpgrade(type);
        if (definition == null) {
            return UpgradeResult.UNAVAILABLE;
        }

        TeamUpgradeState state = getState(arenaId, teamId);
        int reserved = state.reserveNextRoyalLevel(type,
                definition.getMaximumLevel());
        if (reserved == 0) {
            return UpgradeResult.MAX_LEVEL;
        }

        ItemCost cost = definition.getCost(reserved,
                profile.getPricingProfile());
        if (cost == null) {
            state.rollbackRoyalLevel(type, reserved);
            return UpgradeResult.NO_PRICE;
        }
        if (!withdraw(wallet, cost)) {
            state.rollbackRoyalLevel(type, reserved);
            return UpgradeResult.NOT_ENOUGH;
        }
        return UpgradeResult.SUCCESS;
    }

    /**
     * Accoda una trappola pagandone il costo della posizione occupata.
     */
    public UpgradeResult purchaseTrap(ResourceInventory wallet, String arenaId,
                                      String teamId, String trapId,
                                      ModeProfile profile) {
        if (wallet == null || trapId == null) {
            return UpgradeResult.UNAVAILABLE;
        }
        if (teamId == null) {
            return UpgradeResult.NO_TEAM;
        }
        if (profile == null) {
            return UpgradeResult.NOT_IN_GAME;
        }
        if (catalog.getTrap(trapId) == null) {
            return UpgradeResult.UNAVAILABLE;
        }

        TeamUpgradeState state = getState(arenaId, teamId);
        int position = state.queueTrap(trapId, catalog.getMaximumTraps());
        if (position < 0) {
            return UpgradeResult.TRAP_QUEUE_FULL;
        }

        ItemCost cost = catalog.getTrapCost(position, profile);
        if (cost == null) {
            state.removeLastTrap();
            return UpgradeResult.NO_PRICE;
        }
        if (!withdraw(wallet, cost)) {
            state.removeLastTrap();
            return UpgradeResult.NOT_ENOUGH;
        }
        return UpgradeResult.SUCCESS;
    }

    /**
     * Addebita il costo solo se interamente disponibile.
     */
    private boolean withdraw(ResourceInventory wallet, ItemCost cost) {
        ResourceType currency = cost.getCurrency();
        if (wallet.count(currency) < cost.getAmount()) {
            return false;
        }
        if (cost.getAmount() == 0) {
            return true;
        }
        return wallet.withdraw(currency, cost.getAmount());
    }
}
