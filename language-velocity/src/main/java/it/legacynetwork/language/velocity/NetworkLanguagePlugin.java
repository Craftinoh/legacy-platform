package it.legacynetwork.language.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.legacynetwork.language.LanguageProtocol;
import it.legacynetwork.language.LocaleLanguageResolver;
import it.legacynetwork.language.TranslationService;
import it.legacynetwork.language.velocity.command.LanguageCommand;
import it.legacynetwork.language.velocity.listener.PlayerConnectedListener;
import it.legacynetwork.language.velocity.listener.PlayerSettingsListener;
import it.legacynetwork.language.velocity.listener.ProxyShutdownListener;
import it.legacynetwork.language.velocity.listener.ServerPostConnectListener;
import it.legacynetwork.language.velocity.luckperms.LanguageContextCalculator;
import it.legacynetwork.language.velocity.luckperms.LocalizedPrefixProvider;
import it.legacynetwork.language.velocity.repository.FileLanguageRepository;
import it.legacynetwork.language.velocity.repository.PlayerLanguageRepositoryFactory;
import it.legacynetwork.language.velocity.repository.PostgresLanguageNotificationService;
import it.legacynetwork.language.velocity.repository.PostgresPlayerLanguageRepository;
import it.legacynetwork.language.velocity.service.LanguageSynchronizationService;
import it.legacynetwork.language.velocity.service.ProxyLanguageService;
import it.legacynetwork.language.velocity.tablist.VelocityTabListService;
import it.legacynetwork.language.velocity.translation.PropertiesTranslationLoader;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Plugin(
        id = "networklanguage",
        name = "NetworkLanguage",
        version = "0.1.0-SNAPSHOT",
        authors = {"LegacyNetwork"}
)
public final class NetworkLanguagePlugin {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private VelocityTabListService tabListService;
    private ScheduledExecutorService tabScheduler;
    private ProxyLanguageService languageService;
    private LanguageSynchronizationService synchronizationService;
    private FileLanguageRepository repository;
    private PostgresPlayerLanguageRepository postgresRepository;
    private HikariDataSource hikariDataSource;
    private PostgresLanguageNotificationService notificationService;
    private LocalizedPrefixProvider localizedPrefixProvider;
    private Object luckPermsInstance;
    private LanguageContextCalculator lpContextCalculator;

    @Inject
    public NetworkLanguagePlugin(ProxyServer proxy,
                                  Logger logger,
                                  @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        try {
            TranslationService translations = new PropertiesTranslationLoader(
                    getClass().getClassLoader()).load();
            repository = new FileLanguageRepository(dataDirectory);
            repository.load().join();

            languageService = new ProxyLanguageService(
                    repository, new LocaleLanguageResolver());
            LegacyChannelIdentifier channel =
                    new LegacyChannelIdentifier("NetworkLang");
            synchronizationService = new LanguageSynchronizationService(
                    channel, new LanguageProtocol(), languageService, logger);

            proxy.getChannelRegistrar().register(channel);
            registerCommand(translations);
            proxy.getEventManager().register(this,
                    new PlayerConnectedListener(languageService));
            proxy.getEventManager().register(this,
                    new PlayerSettingsListener(languageService, synchronizationService));
            proxy.getEventManager().register(this,
                    new ServerPostConnectListener(synchronizationService));
            proxy.getEventManager().register(this,
                    new ProxyShutdownListener(repository));

            saveResource("tablist.yml");
            saveResource("config.yml");
            loadPostgresConfig();

            tabScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "networklang-tab");
                t.setDaemon(true);
                return t;
            });
            tabListService = new VelocityTabListService(
                    proxy, logger, dataDirectory, tabScheduler);
            tabListService.setLanguageResolver(
                    p -> languageService.current(p).language());
            tabListService.setLocalizedPrefixProvider(localizedPrefixProvider);
            tabListService.load();

            registerLuckPerms();
            registerTabListeners();

            logger.info("NetworkLanguage inizializzato.");
        } catch (IOException | RuntimeException exception) {
            logger.error("Impossibile inizializzare NetworkLanguage", exception);
            throw new IllegalStateException(
                    "NetworkLanguage initialization failed", exception);
        }
    }

    private void registerLuckPerms() {
        try {
            Class<?> lpClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object lp = lpClass.getMethod("get").invoke(null);
            luckPermsInstance = lp;

            lpContextCalculator = new LanguageContextCalculator(languageService);
            lpClass.getMethod("getContextManager")
                    .invoke(lp)
                    .getClass()
                    .getMethod("registerCalculator",
                            Class.forName("net.luckperms.api.context.ContextCalculator"))
                    .invoke(lpClass.getMethod("getContextManager").invoke(lp),
                            lpContextCalculator);

            localizedPrefixProvider = new LocalizedPrefixProvider(
                    (net.luckperms.api.LuckPerms) lp,
                    java.util.logging.Logger.getLogger("NetworkLanguage-LP"));
            if (tabListService != null) {
                tabListService.setLocalizedPrefixProvider(localizedPrefixProvider);
            }

            logger.info("LuckPerms ContextCalculator registrato.");
        } catch (Exception e) {
            logger.warn("LuckPerms non disponibile, context lingua non attivi.");
            luckPermsInstance = null;
            lpContextCalculator = null;
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (tabListService != null) {
            tabListService.close();
        }
        if (tabScheduler != null) {
            tabScheduler.shutdown();
            try {
                tabScheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        if (notificationService != null) {
            notificationService.close();
        }
        if (localizedPrefixProvider != null) {
            localizedPrefixProvider.close();
        }
        if (luckPermsInstance != null && lpContextCalculator != null) {
            try {
                luckPermsInstance.getClass()
                        .getMethod("getContextManager")
                        .invoke(luckPermsInstance)
                        .getClass()
                        .getMethod("unregisterCalculator",
                                Class.forName("net.luckperms.api.context.ContextCalculator"))
                        .invoke(luckPermsInstance.getClass()
                                .getMethod("getContextManager").invoke(luckPermsInstance),
                                lpContextCalculator);
            } catch (Exception ignored) {
            }
        }
        if (postgresRepository != null) {
            postgresRepository.close();
        }
        if (hikariDataSource != null) {
            hikariDataSource.close();
        }
    }

    private void registerTabListeners() {
        proxy.getEventManager().register(this, new Object() {
            @Subscribe
            public void onLogin(PostLoginEvent event) {
                if (tabListService != null) {
                    tabListService.sendScheduled(event.getPlayer(), true);
                }
            }

            @Subscribe
            public void onServerConnect(ServerPostConnectEvent event) {
                if (tabListService != null) {
                    tabListService.sendScheduled(event.getPlayer(), true);
                }
            }

            @Subscribe
            public void onDisconnect(DisconnectEvent event) {
                if (tabListService != null) {
                    tabListService.clear(event.getPlayer());
                }
            }

            @Subscribe
            public void onSettingsChanged(PlayerSettingsChangedEvent event) {
                if (tabListService != null) {
                    tabListService.sendScheduled(event.getPlayer(), true);
                }
            }
        });
    }

    private void registerCommand(TranslationService translations) {
        CommandMeta langMeta = proxy.getCommandManager()
                .metaBuilder("networklang")
                .plugin(this)
                .aliases("langproxy", "nlang")
                .build();
        proxy.getCommandManager().register(langMeta,
                new LanguageCommand(languageService, synchronizationService,
                        translations, logger, tabListService));

        CommandMeta reloadMeta = proxy.getCommandManager()
                .metaBuilder("networklangreload")
                .plugin(this)
                .build();
        proxy.getCommandManager().register(reloadMeta,
                new NetworkLangReloadCommand(this));
    }

    public void reloadAll() {
        repository.load().join();
        if (tabListService != null) {
            tabListService.reload();
        }
        logger.info("NetworkLanguage reload completato.");
    }

    public VelocityTabListService getTabListService() {
        return tabListService;
    }

    void saveResource(String resource) throws IOException {
        Path target = dataDirectory.resolve(resource);
        if (Files.exists(target)) {
            return;
        }
        Files.createDirectories(target.getParent());
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream(resource)) {
            if (in == null) {
                return;
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void loadPostgresConfig() {
        Path configFile = dataDirectory.resolve("config.yml");
        if (!Files.exists(configFile)) {
            logger.info("No config.yml found, using SQLite default.");
            initSqlite(dataDirectory);
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> config = loadYaml(configFile);
        if (config == null) {
            logger.info("Invalid config.yml, using SQLite default.");
            initSqlite(dataDirectory);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> storage = (Map<String, Object>) config.get("storage");
        String storageType = storage != null
                ? getString(storage, "type", "sqlite") : "sqlite";

        if ("postgresql".equalsIgnoreCase(storageType)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pg = (Map<String, Object>) config.get("postgresql");
            if (pg == null) {
                logger.warn("PostgreSQL config section missing, using SQLite.");
                initSqlite(dataDirectory);
                return;
            }
            String host = getString(pg, "host", "localhost");
            int port = getInt(pg, "port", 5432);
            String database = getString(pg, "database", "networklanguage");
            String username = getString(pg, "username", "postgres");
            String password = getString(pg, "password", "");
            String proxyId = getString(pg, "proxy-id", "velocity-1");

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:postgresql://" + host + ":" + port
                    + "/" + database);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(getInt(pg, "pool-size", 10));
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(getInt(pg, "connection-timeout-ms", 5000));

            hikariDataSource = new HikariDataSource(hikariConfig);
            postgresRepository = new PostgresPlayerLanguageRepository(
                    hikariDataSource, proxyId);

            notificationService = new PostgresLanguageNotificationService(
                    host, port, database, username, password, proxyId,
                    java.util.logging.Logger.getLogger("NetworkLanguage-PG"));
            notificationService.start();

            postgresRepository.setNotificationCallback(commitEvent -> {
                try (java.sql.Connection conn = hikariDataSource.getConnection()) {
                    PostgresLanguageNotificationService.notifyChange(conn,
                            commitEvent.playerUuid, commitEvent.revision,
                            commitEvent.languageCode, commitEvent.locale, proxyId);
                } catch (java.sql.SQLException e) {
                    logger.warn("NOTIFY failed after commit: " + e.getMessage());
                }
            });

            logger.info("NetworkLanguage PostgreSQL pool + LISTEN/NOTIFY initialized.");
        } else {
            initSqlite(dataDirectory);
        }
    }

    private void initSqlite(Path dataDir) {
        String proxyId = "velocity-1";
        PlayerLanguageRepositoryFactory.RepositoryResult result =
                PlayerLanguageRepositoryFactory.create(
                        "sqlite", dataDir, proxyId,
                        java.util.logging.Logger.getLogger("NetworkLanguage-SQLite"));
        postgresRepository = null;
        hikariDataSource = null;
        notificationService = null;
        logger.info(result.message);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(Path file) {
        try {
            Yaml yaml = new Yaml();
            try (InputStreamReader reader = new InputStreamReader(
                    Files.newInputStream(file), StandardCharsets.UTF_8)) {
                Object loaded = yaml.load(reader);
                if (loaded instanceof Map) {
                    return (Map<String, Object>) loaded;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static boolean getBool(Map<String, Object> map, String key, boolean def) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return def;
    }

    private static int getInt(Map<String, Object> map, String key, int def) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return def;
    }

    private static String getString(Map<String, Object> map, String key, String def) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return def;
    }
}
