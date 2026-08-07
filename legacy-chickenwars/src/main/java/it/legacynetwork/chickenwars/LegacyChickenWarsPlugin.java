package it.legacynetwork.chickenwars;

import it.legacynetwork.chickenwars.api.ChickenWarsService;
import it.legacynetwork.chickenwars.api.ChickenWarsServiceImpl;
import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.chicken.ChickenService;
import it.legacynetwork.chickenwars.chicken.ChickenSettings;
import it.legacynetwork.chickenwars.command.AdminCommand;
import it.legacynetwork.chickenwars.command.ChickenWarsCommand;
import it.legacynetwork.chickenwars.command.HelpService;
import it.legacynetwork.chickenwars.config.ChickenWarsConfig;
import it.legacynetwork.chickenwars.config.GeneratorSettings;
import it.legacynetwork.chickenwars.game.GameLoopTask;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.listener.CombatListener;
import it.legacynetwork.chickenwars.listener.ConnectionListener;
import it.legacynetwork.chickenwars.listener.InteractionListener;
import it.legacynetwork.chickenwars.listener.SetupListener;
import it.legacynetwork.chickenwars.listener.WorldProtectionListener;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.player.PendingRestoreService;
import it.legacynetwork.chickenwars.setup.SetupService;
import it.legacynetwork.chickenwars.shop.ShopCatalog;
import it.legacynetwork.chickenwars.shop.ShopService;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("chickens.yml", false);
        saveResource("shop.yml", false);
        saveResource("messages_it.yml", false);
        saveResource("messages_en.yml", false);

        try {
            messages = new MessageService(this);
            ChickenWarsConfig config = loadConfiguration();
            if (config == null) {
                throw new IllegalStateException(
                        "config.yml, chickens.yml o file messaggi non validi");
            }

            shop = new ShopService(messages);
            shop.setCatalog(loadShopCatalog());

            services = new GameServices(this, messages,
                    new ChickenService(getLogger()), shop, config);
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
        getServer().getServicesManager().unregisterAll(this);
        if (pendingRestores != null) {
            pendingRestores.clear();
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
                new ConnectionListener(arenas, pendingRestores), this);
        getServer().getPluginManager().registerEvents(
                new CombatListener(arenas, services), this);
        getServer().getPluginManager().registerEvents(
                new WorldProtectionListener(arenas, services), this);
        getServer().getPluginManager().registerEvents(
                new InteractionListener(arenas, services), this);
        getServer().getPluginManager().registerEvents(
                new SetupListener(setup), this);
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

        ShopCatalog catalog = loadShopCatalog();
        if (catalog != null) {
            shop.setCatalog(catalog);
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
        if (!messages.reload(getConfig().getString("language.italian-file",
                        "messages_it.yml"),
                getConfig().getString("language.english-file", "messages_en.yml"),
                getConfig().getString("language.fallback", "it"))) {
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
            return ChickenWarsConfig.create(getConfig(), generatorSettings,
                    chickenSettings);
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

    private ShopCatalog loadShopCatalog() {
        return ShopCatalog.load(new File(getDataFolder(), "shop.yml"), getLogger());
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
