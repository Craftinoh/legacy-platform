package it.legacynetwork.chickenwars;

import it.legacynetwork.chickenwars.api.ChickenWarsService;
import it.legacynetwork.chickenwars.api.ChickenWarsServiceImpl;
import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.bootstrap.ChickenWarsNetworkRuntime;
import it.legacynetwork.chickenwars.chicken.ChickenService;
import it.legacynetwork.chickenwars.chicken.ChickenSettings;
import it.legacynetwork.chickenwars.chicken.RoyalChickenDamageService;
import it.legacynetwork.chickenwars.chicken.RoyalChickenRegistry;
import it.legacynetwork.chickenwars.chicken.RoyalDefeatDispatcher;
import it.legacynetwork.chickenwars.chicken.RoyalUpgradeApplier;
import it.legacynetwork.chickenwars.command.AdminCommand;
import it.legacynetwork.chickenwars.command.ChickenWarsCommand;
import it.legacynetwork.chickenwars.command.HelpService;
import it.legacynetwork.chickenwars.config.ChickenWarsConfig;
import it.legacynetwork.chickenwars.config.GeneratorSettings;
import it.legacynetwork.chickenwars.effect.BukkitEffectAdapter;
import it.legacynetwork.chickenwars.effect.EffectAdapter;
import it.legacynetwork.chickenwars.effect.HealPoolService;
import it.legacynetwork.chickenwars.effect.TeamEffectService;
import it.legacynetwork.chickenwars.game.GameLoopTask;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.generator.GeneratedResourceRegistry;
import it.legacynetwork.chickenwars.listener.BaseRegionListener;
import it.legacynetwork.chickenwars.listener.CombatListener;
import it.legacynetwork.chickenwars.listener.ConnectionListener;
import it.legacynetwork.chickenwars.listener.InteractionListener;
import it.legacynetwork.chickenwars.listener.LobbyInventoryListener;
import it.legacynetwork.chickenwars.listener.SetupListener;
import it.legacynetwork.chickenwars.listener.WorldProtectionListener;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.player.PendingRestoreService;
import it.legacynetwork.chickenwars.setup.SetupService;
import it.legacynetwork.chickenwars.economy.ResourceTransferService;
import it.legacynetwork.chickenwars.listener.VoidProtectionListener;
import it.legacynetwork.chickenwars.persistence.QuickBuyRepository;
import it.legacynetwork.chickenwars.player.ReconnectService;
import it.legacynetwork.chickenwars.player.equipment.EquipmentService;
import it.legacynetwork.chickenwars.player.equipment.EquipmentSettings;
import it.legacynetwork.chickenwars.protection.VoidFallGuard;
import it.legacynetwork.chickenwars.shop.ChickenMenuService;
import it.legacynetwork.chickenwars.shop.QuickBuyService;
import it.legacynetwork.chickenwars.shop.ShopConfigLoader;
import it.legacynetwork.chickenwars.shop.ShopConfiguration;
import it.legacynetwork.chickenwars.shop.ShopService;
import it.legacynetwork.chickenwars.scoreboard.ScoreboardConfigLoader;
import it.legacynetwork.chickenwars.scoreboard.ScoreboardSettings;
import it.legacynetwork.chickenwars.trap.BaseEntryTracker;
import it.legacynetwork.chickenwars.trap.TrapTriggerService;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import it.legacynetwork.chickenwars.upgrade.UpgradeCatalog;
import it.legacynetwork.chickenwars.upgrade.UpgradeConfigLoader;
import it.legacynetwork.chickenwars.world.WorldService;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Punto di ingresso di ChickenWars.
 *
 * <p>Si limita a costruire i servizi, registrare comandi e listener e avviare
 * l'unico ciclo di gioco: la logica risiede nei servizi dedicati.</p>
 */
public final class LegacyChickenWarsPlugin extends JavaPlugin {

    private MessageService messages;
    private ShopService shop;
    private GameServices services;
    private ArenaManager arenas;
    private GameLoopTask gameLoop;
    private PendingRestoreService pendingRestores;
    private SetupService setup;
    private WorldService worlds;
    private HelpService help;
    private EquipmentService equipment;
    private ResourceTransferService transfers;
    private ReconnectService reconnects;
    private VoidFallGuard voidGuard;
    private QuickBuyService quickBuy;
    private QuickBuyRepository quickBuyRepository;
    private ChickenWarsNetworkRuntime network;
    private TeamUpgradeService upgrades;
    private TeamEffectService teamEffects;
    private HealPoolService healPool;
    private BaseEntryTracker baseEntryTracker;
    private TrapTriggerService traps;
    private RoyalChickenRegistry royalRegistry;
    private RoyalChickenDamageService royalDamage;
    private RoyalUpgradeApplier royalApplier;
    private RoyalDefeatDispatcher royalDefeatDispatcher;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("chickens.yml", false);
        saveResource("shop.yml", false);
        saveResource("upgrades.yml", false);
        saveResource("scoreboard.yml", false);
        saveResource("messages_it.yml", false);
        saveResource("messages_en.yml", false);

        try {
            messages = new MessageService(this);
            ChickenWarsConfig config = loadConfiguration();
            if (config == null) {
                throw new IllegalStateException(
                        "config.yml, chickens.yml o file messaggi non validi");
            }

            equipment = new EquipmentService(loadEquipmentSettings());
            network = new ChickenWarsNetworkRuntime(this, messages, getConfig());
            quickBuyRepository = network.quickBuy();
            quickBuy = new QuickBuyService(quickBuyRepository, getLogger());
            shop = new ShopService(messages, equipment, quickBuy);
            shop.setConfiguration(loadShopConfiguration());

            transfers = new ResourceTransferService();
            reconnects = new ReconnectService();
            voidGuard = new VoidFallGuard(config.getVoidDropTolerance());

            upgrades = new TeamUpgradeService();
            upgrades.setCatalog(loadUpgradeCatalog());

            EffectAdapter effectAdapter = new BukkitEffectAdapter();
            teamEffects = new TeamEffectService(upgrades, effectAdapter);
            healPool = new HealPoolService(upgrades, effectAdapter);
            baseEntryTracker = new BaseEntryTracker();
            traps = new TrapTriggerService(upgrades, effectAdapter);
            royalRegistry = new RoyalChickenRegistry();
            royalDamage = new RoyalChickenDamageService();
            royalApplier = new RoyalUpgradeApplier(upgrades);
            royalDefeatDispatcher = new RoyalDefeatDispatcher();

            ChickenMenuService chickenMenu = new ChickenMenuService(null,
                    messages);
            services = new GameServices(this, messages,
                    new ChickenService(getLogger()), shop, equipment,
                    transfers, reconnects, upgrades,
                    teamEffects, healPool, baseEntryTracker, traps,
                    royalRegistry, royalDamage, royalApplier,
                    royalDefeatDispatcher, chickenMenu,
                    network.progression(), network.routing(),
                    network.lobby(), network.selector(),
                    network.returnLobby(),
                    new GeneratedResourceRegistry(), config);
            chickenMenu.setServices(services);
            pendingRestores = new PendingRestoreService();
            help = new HelpService(messages);

            worlds = new WorldService(this);
            worlds.reload(config.getWorldNamePrefix(),
                    config.getDefaultWorldTemplate(),
                    config.isClearWorldEntities());

            arenas = new ArenaManager(services,
                    new File(getDataFolder(), "arenas"), getLogger());
            int loaded = arenas.loadAll();

            if (config.isAutoLoadWorlds()) {
                int activated = worlds.loadAll(collectArenaWorlds());
                if (activated > 0) {
                    getLogger().info("Mondi arena attivati: " + activated + ".");
                }
            }

            setup = new SetupService(arenas, messages, pendingRestores, worlds);

            registerCommands();
            registerListeners();

            gameLoop = new GameLoopTask(arenas);
            gameLoop.start(this);
            network.startHeartbeat(arenas);

            getServer().getServicesManager().register(ChickenWarsService.class,
                    new ChickenWarsServiceImpl(arenas), this, ServicePriority.Normal);

            getLogger().info("ChickenWars avviato con " + loaded + " arene.");
            if (!shop.isAvailable()) {
                getLogger().warning("Shop non disponibile: controllare shop.yml.");
            }
        } catch (RuntimeException exception) {
            getLogger().severe("Impossibile inizializzare ChickenWars: "
                    + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        } catch (LinkageError error) {
            getLogger().severe("Dipendenza incompatibile durante l'avvio: "
                    + error.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (setup != null) {
            setup.shutdown();
            setup = null;
        }
        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }
        if (arenas != null) {
            arenas.shutdown();
        }
        if (teamEffects != null) {
            teamEffects.clearAll();
            teamEffects = null;
        }
        if (healPool != null) {
            healPool.clearAll();
            healPool = null;
        }
        if (baseEntryTracker != null) {
            baseEntryTracker.clearAll();
            baseEntryTracker = null;
        }
        if (royalRegistry != null) {
            royalRegistry.clearAll();
            royalRegistry = null;
        }
        if (royalDefeatDispatcher != null) {
            royalDefeatDispatcher.clear();
            royalDefeatDispatcher = null;
        }
        getServer().getServicesManager().unregisterAll(this);
        if (pendingRestores != null) {
            pendingRestores.clear();
        }
        if (transfers != null) {
            transfers.clearAll();
        }
        if (reconnects != null) {
            reconnects.clear();
        }
        if (upgrades != null) {
            upgrades.clearAll();
            upgrades = null;
        }
        if (network != null) {
            network.close();
            network = null;
        }
        if (services != null) {
            services.getGeneratedResources().clear();
        }
        if (messages != null) {
            messages.close();
        }
        arenas = null;
        services = null;
        shop = null;
        messages = null;
        worlds = null;
        help = null;
        equipment = null;
        transfers = null;
        reconnects = null;
        voidGuard = null;
        quickBuy = null;
        quickBuyRepository = null;
    }

    private void registerCommands() {
        PluginCommand command = getCommand("chickenwars");
        if (command == null) {
            throw new IllegalStateException(
                    "Comando chickenwars mancante in plugin.yml");
        }
        AdminCommand adminCommand = new AdminCommand(arenas, services, setup,
                worlds, help, new Runnable() {
                    @Override
                    public void run() {
                        reloadEverything(getServer().getConsoleSender());
                    }
                });
        ChickenWarsCommand executor =
                new ChickenWarsCommand(arenas, services, adminCommand, help);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new ConnectionListener(arenas, pendingRestores, reconnects,
                        quickBuy, services), this);
        getServer().getPluginManager().registerEvents(
                new VoidProtectionListener(arenas, voidGuard), this);
        getServer().getPluginManager().registerEvents(
                new CombatListener(arenas, services), this);
        getServer().getPluginManager().registerEvents(
                new WorldProtectionListener(arenas, services), this);
        getServer().getPluginManager().registerEvents(
                new InteractionListener(arenas, services), this);
        getServer().getPluginManager().registerEvents(
                new SetupListener(setup), this);
        getServer().getPluginManager().registerEvents(
                new BaseRegionListener(arenas, services), this);
        getServer().getPluginManager().registerEvents(
                new LobbyInventoryListener(network.selector()), this);
    }

    /**
     * Ricarica configurazioni, messaggi, shop e arene.
     *
     * <p>Se una configurazione non e' valida viene conservata la precedente e
     * l'errore segnalato, senza interrompere le partite in corso.</p>
     *
     * @param sender destinatario del riscontro
     */
    public void reloadEverything(CommandSender sender) {
        reloadConfig();
        ChickenWarsConfig config = loadConfiguration();
        if (config == null) {
            messages.send(sender, "admin.reload-failed");
            return;
        }
        services.setConfig(config);
        worlds.reload(config.getWorldNamePrefix(),
                config.getDefaultWorldTemplate(),
                config.isClearWorldEntities());
        equipment.setSettings(loadEquipmentSettings());
        voidGuard.setTolerance(config.getVoidDropTolerance());

        UpgradeCatalog reloadedUpgrades = loadUpgradeCatalog();
        if (!reloadedUpgrades.isEmpty()) {
            upgrades.setCatalog(reloadedUpgrades);
        }

        ShopConfiguration reloaded = loadShopConfiguration();
        if (!reloaded.isEmpty()) {
            shop.setConfiguration(reloaded);
        } else {
            messages.send(sender, "admin.shop-reload-failed");
        }

        int loaded = arenas.loadAll();
        if (config.isAutoLoadWorlds()) {
            worlds.loadAll(collectArenaWorlds());
        }
        messages.send(sender, "admin.reloaded", "{arenas}", String.valueOf(loaded));
        getLogger().info("ChickenWars ricaricato: " + loaded + " arene.");
    }

    /**
     * Costruisce la configurazione a partire dai file su disco.
     *
     * @return la configurazione, oppure {@code null} se un file non e' valido
     */
    private ChickenWarsConfig loadConfiguration() {
        if (!messages.reload(getConfig().getString("language.fallback", "it"))) {
            return null;
        }

        File chickenFile = new File(getDataFolder(), "chickens.yml");
        if (!chickenFile.isFile()) {
            getLogger().warning("chickens.yml mancante.");
            return null;
        }
        YamlConfiguration chickenConfig =
                YamlConfiguration.loadConfiguration(chickenFile);

        try {
            ChickenSettings chickenSettings = ChickenSettings.fromSection(
                    chickenConfig.getConfigurationSection("default"));
            GeneratorSettings generatorSettings = GeneratorSettings.fromSection(
                    getConfig().getConfigurationSection("generators"));
            ScoreboardSettings scoreboard = ScoreboardConfigLoader.load(
                    new File(getDataFolder(), "scoreboard.yml"), getLogger());
            return ChickenWarsConfig.create(getConfig(), generatorSettings,
                    chickenSettings, scoreboard);
        } catch (RuntimeException exception) {
            getLogger().warning("Configurazione non valida: "
                    + exception.getMessage());
            return null;
        }
    }

    /**
     * Elenca senza duplicati i mondi richiesti dalle arene configurate.
     */
    private List<String> collectArenaWorlds() {
        List<String> worldNames = new ArrayList<String>();
        for (ArenaDefinition definition : arenas.getDefinitions()) {
            String worldName = definition.getWorld();
            if (worldName != null && !worldName.trim().isEmpty()
                    && !worldNames.contains(worldName)) {
                worldNames.add(worldName);
            }
        }
        return worldNames;
    }

    /**
     * Legge {@code shop.yml} e riporta in console le anomalie rilevate.
     */
    private ShopConfiguration loadShopConfiguration() {
        File file = new File(getDataFolder(), "shop.yml");
        if (!file.isFile()) {
            getLogger().warning("shop.yml assente: shop disabilitato.");
            return ShopConfiguration.empty();
        }
        ShopConfiguration loaded = ShopConfigLoader.load(
                YamlConfiguration.loadConfiguration(file));
        for (String warning : loaded.getWarnings()) {
            getLogger().warning("shop.yml: " + warning);
        }
        return loaded;
    }

    /**
     * Legge {@code upgrades.yml} e riporta in console le anomalie rilevate.
     */
    private UpgradeCatalog loadUpgradeCatalog() {
        File file = new File(getDataFolder(), "upgrades.yml");
        if (!file.isFile()) {
            getLogger().warning("upgrades.yml assente: upgrade disabilitati.");
            return UpgradeCatalog.empty();
        }
        UpgradeCatalog loaded = UpgradeConfigLoader.load(
                YamlConfiguration.loadConfiguration(file));
        for (String warning : loaded.getWarnings()) {
            getLogger().warning("upgrades.yml: " + warning);
        }
        return loaded;
    }

    private EquipmentSettings loadEquipmentSettings() {
        return EquipmentSettings.fromSection(
                getConfig().getConfigurationSection("equipment"));
    }

    public ArenaManager getArenas() {
        return arenas;
    }

    public GameServices getServices() {
        return services;
    }

    public MessageService getMessages() {
        return messages;
    }
}
