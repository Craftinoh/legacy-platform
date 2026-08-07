package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.chicken.ChickenService;
import it.legacynetwork.chickenwars.chicken.RoyalChickenDamageService;
import it.legacynetwork.chickenwars.chicken.RoyalChickenRegistry;
import it.legacynetwork.chickenwars.chicken.RoyalDefeatDispatcher;
import it.legacynetwork.chickenwars.chicken.RoyalUpgradeApplier;
import it.legacynetwork.chickenwars.config.ChickenWarsConfig;
import it.legacynetwork.chickenwars.death.PlayerDeathProcessor;
import it.legacynetwork.chickenwars.economy.ResourceTransferService;
import it.legacynetwork.chickenwars.effect.HealPoolService;
import it.legacynetwork.chickenwars.effect.TeamEffectService;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.player.ReconnectService;
import it.legacynetwork.chickenwars.player.equipment.EquipmentService;
import it.legacynetwork.chickenwars.shop.ChickenMenuService;
import it.legacynetwork.chickenwars.shop.ShopService;
import it.legacynetwork.chickenwars.trap.BaseEntryTracker;
import it.legacynetwork.chickenwars.trap.TrapTriggerService;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Servizi condivisi tra tutte le partite.
 *
 * <p>Raccogliere le dipendenze in un unico oggetto mantiene compatti i
 * costruttori e consente di sostituire la configurazione a ogni reload senza
 * ricreare le partite in corso.</p>
 */
public final class GameServices {

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final ChickenService chickens;
    private final ShopService shop;
    private final EquipmentService equipment;
    private final ResourceTransferService transfers;
    private final ReconnectService reconnects;
    private final TeamUpgradeService upgrades;
    private final TeamEffectService teamEffects;
    private final HealPoolService healPool;
    private final BaseEntryTracker baseEntryTracker;
    private final TrapTriggerService traps;
    private final RoyalChickenRegistry royalRegistry;
    private final RoyalChickenDamageService royalDamage;
    private final RoyalUpgradeApplier royalApplier;
    private final RoyalDefeatDispatcher royalDefeatDispatcher;
    private final ChickenMenuService chickenMenu;
    private final PlayerDeathProcessor deaths;

    private volatile ChickenWarsConfig config;

    public GameServices(JavaPlugin plugin, MessageService messages,
                        ChickenService chickens, ShopService shop,
                        EquipmentService equipment,
                        ResourceTransferService transfers,
                        ReconnectService reconnects,
                        TeamUpgradeService upgrades,
                        TeamEffectService teamEffects,
                        HealPoolService healPool,
                        BaseEntryTracker baseEntryTracker,
                        TrapTriggerService traps,
                        RoyalChickenRegistry royalRegistry,
                        RoyalChickenDamageService royalDamage,
                        RoyalUpgradeApplier royalApplier,
                        RoyalDefeatDispatcher royalDefeatDispatcher,
                        ChickenMenuService chickenMenu,
                        ChickenWarsConfig config) {
        if (plugin == null || messages == null || chickens == null
                || shop == null || equipment == null || transfers == null
                || reconnects == null || upgrades == null
                || teamEffects == null || healPool == null
                || baseEntryTracker == null || traps == null
                || royalRegistry == null || royalDamage == null
                || royalApplier == null || royalDefeatDispatcher == null
                || chickenMenu == null
                || config == null) {
            throw new IllegalArgumentException("Servizi ChickenWars incompleti");
        }
        this.plugin = plugin;
        this.messages = messages;
        this.chickens = chickens;
        this.shop = shop;
        this.equipment = equipment;
        this.transfers = transfers;
        this.reconnects = reconnects;
        this.upgrades = upgrades;
        this.teamEffects = teamEffects;
        this.healPool = healPool;
        this.baseEntryTracker = baseEntryTracker;
        this.traps = traps;
        this.royalRegistry = royalRegistry;
        this.royalDamage = royalDamage;
        this.royalApplier = royalApplier;
        this.royalDefeatDispatcher = royalDefeatDispatcher;
        this.chickenMenu = chickenMenu;
        this.config = config;
        this.deaths = new PlayerDeathProcessor(transfers, equipment);
        equipment.setEnchantProvider(upgrades);
    }

    /**
     * Stato permanente conservato tra logout e rientro nella stessa partita.
     */
    public ReconnectService getReconnects() {
        return reconnects;
    }

    /**
     * Orchestratore unico delle morti, compreso l'abbandono in combattimento.
     */
    public PlayerDeathProcessor getDeaths() {
        return deaths;
    }

    public EquipmentService getEquipment() {
        return equipment;
    }

    public ResourceTransferService getTransfers() {
        return transfers;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public MessageService getMessages() {
        return messages;
    }

    public ChickenService getChickens() {
        return chickens;
    }

    public ShopService getShop() {
        return shop;
    }

    public TeamUpgradeService getUpgrades() {
        return upgrades;
    }

    public TeamEffectService getTeamEffects() {
        return teamEffects;
    }

    public HealPoolService getHealPool() {
        return healPool;
    }

    public BaseEntryTracker getBaseEntryTracker() {
        return baseEntryTracker;
    }

    public TrapTriggerService getTraps() {
        return traps;
    }

    public RoyalChickenRegistry getRoyalRegistry() {
        return royalRegistry;
    }

    public RoyalChickenDamageService getRoyalDamage() {
        return royalDamage;
    }

    public RoyalUpgradeApplier getRoyalApplier() {
        return royalApplier;
    }

    public RoyalDefeatDispatcher getRoyalDefeatDispatcher() {
        return royalDefeatDispatcher;
    }

    public ChickenMenuService getChickenMenu() {
        return chickenMenu;
    }

    public ChickenWarsConfig getConfig() {
        return config;
    }

    /**
     * Sostituisce la configurazione dopo un reload riuscito.
     */
    public void setConfig(ChickenWarsConfig config) {
        if (config != null) {
            this.config = config;
        }
    }
}
