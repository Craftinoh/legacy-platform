package it.legacynetwork.language.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.legacynetwork.language.Language;
import it.legacynetwork.language.LanguageChangeResult;
import it.legacynetwork.language.LanguageProtocol;
import it.legacynetwork.language.LanguageProtocolAction;
import it.legacynetwork.language.LanguageProtocolException;
import it.legacynetwork.language.LanguageProtocolMessage;
import it.legacynetwork.language.LocaleLanguageResolver;
import it.legacynetwork.language.TranslationService;
import it.legacynetwork.language.velocity.command.LanguageCommand;
import it.legacynetwork.language.velocity.listener.PlayerConnectedListener;
import it.legacynetwork.language.velocity.listener.PlayerSettingsListener;
import it.legacynetwork.language.velocity.listener.ServerPostConnectListener;
import it.legacynetwork.language.velocity.luckperms.LanguageContextCalculator;
import it.legacynetwork.language.velocity.luckperms.LocalizedPrefixProvider;
import it.legacynetwork.language.velocity.repository.PlayerLanguageRepository;
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
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private final LanguageProtocol protocol = new LanguageProtocol();

    private VelocityTabListService tabListService;
    private ScheduledExecutorService tabScheduler;
    private ProxyLanguageService languageService;
    private LanguageSynchronizationService synchronizationService;
    private PlayerLanguageRepository playerRepository;
    private PostgresPlayerLanguageRepository postgresRepository;
    private HikariDataSource hikariDataSource;
    private PostgresLanguageNotificationService notificationService;
    private LocalizedPrefixProvider localizedPrefixProvider;
    private Object luckPermsInstance;
    private LanguageContextCalculator lpContextCalculator;
    private LegacyChannelIdentifier languageChannel;
    private String proxyId = "velocity-1";

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
            saveResource("tablist.yml");
            saveResource("config.yml");
            initializeStorage();

            TranslationService translations = new PropertiesTranslationLoader(
                    getClass().getClassLoader()).load();
            languageService = new ProxyLanguageService(
                    playerRepository, new LocaleLanguageResolver(), proxyId);

            languageChannel = new LegacyChannelIdentifier(
                    LanguageProtocol.CHANNEL);
            synchronizationService = new LanguageSynchronizationService(
                    languageChannel, protocol, languageService, logger);

            proxy.getChannelRegistrar().register(languageChannel);
            registerCommand(translations);
            proxy.getEventManager().register(this,
                    new PlayerConnectedListener(languageService));
            proxy.getEventManager().register(this,
                    new PlayerSettingsListener(
                            languageService, synchronizationService));
            proxy.getEventManager().register(this,
                    new ServerPostConnectListener(
                            synchronizationService));

            tabScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "networklang-tab");
                t.setDaemon(true);
                return t;
            });
            tabListService = new VelocityTabListService(
                    proxy, logger, dataDirectory, tabScheduler);
            tabListService.setLanguageResolver(
                    p -> languageService.current(p).language());
            tabListService.load();

            registerLuckPerms();
            registerTabListeners();

            logger.info("NetworkLanguage inizializzato sul canale {}.",
                    LanguageProtocol.CHANNEL);
        } catch (IOException | RuntimeException exception) {
            logger.error(
                    "Impossibile inizializzare NetworkLanguage",
                    exception);
            throw new IllegalStateException(
                    "NetworkLanguage initialization failed",
                    exception);
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (languageChannel == null
                || !languageChannel.equals(event.getIdentifier())) {
            return;
        }
        event.setResult(
                PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection)
                || !(event.getTarget() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getTarget();
        try {
            LanguageProtocolMessage message =
                    protocol.deserialize(event.getData());
            if (message.getAction()
                    != LanguageProtocolAction.LANGUAGE_CHANGE_REQUEST) {
                return;
            }
            if (!player.getUniqueId().equals(
                    message.getPlayerUuid())) {
                logger.warn(
                        "Richiesta lingua con UUID non corrispondente da backend.");
                return;
            }

            Optional<Language> selected = Language.findByInput(
                    message.getLanguageCode());
            if (!selected.isPresent()) {
                return;
            }

            final Language requestedLanguage = selected.get();
            languageService.requestManualChange(
                            player, requestedLanguage)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            logger.error(
                                    "Cambio lingua fallito per {}",
                                    player.getUsername(),
                                    throwable);
                            synchronizationService.synchronize(player);
                            synchronizationService.sendChangeResult(
                                    player,
                                    requestedLanguage,
                                    LanguageChangeResult.ERROR);
                            return;
                        }

                        LanguageChangeResult publicResult =
                                mapChangeResult(result.status);

                        synchronizationService.synchronize(player);
                        synchronizationService.sendChangeResult(
                                player,
                                requestedLanguage,
                                publicResult);

                        if (result.status
                                == PlayerLanguageRepository.ChangeStatus.SUCCESS
                                || result.status
                                == PlayerLanguageRepository.ChangeStatus.ALREADY_SELECTED) {
                            if (tabListService != null) {
                                tabListService.sendImmediately(
                                        player, true);
                            }
                            return;
                        }

                        logger.info(
                                "Cambio lingua rifiutato per {}: {}",
                                player.getUsername(),
                                result.messageCode);
                    });
        } catch (LanguageProtocolException exception) {
            logger.warn(
                    "Payload lingua non valido dal backend: {}",
                    exception.getMessage());
        }
    }

    private LanguageChangeResult mapChangeResult(
            PlayerLanguageRepository.ChangeStatus status) {
        if (status == null) {
            return LanguageChangeResult.ERROR;
        }
        switch (status) {
            case SUCCESS:
                return LanguageChangeResult.SUCCESS;
            case ALREADY_SELECTED:
                return LanguageChangeResult.ALREADY_SELECTED;
            case OPEN_COOLDOWN:
            case CHANGE_COOLDOWN:
                return LanguageChangeResult.COOLDOWN;
            case HOURLY_LIMIT:
                return LanguageChangeResult.RATE_LIMITED;
            case UNSUPPORTED_LANGUAGE:
            case DATABASE_ERROR:
            default:
                return LanguageChangeResult.ERROR;
        }
    }

    private void registerLuckPerms() {
        try {
            Class<?> lpClass = Class.forName(
                    "net.luckperms.api.LuckPermsProvider");
            Object lp = lpClass.getMethod("get").invoke(null);
            luckPermsInstance = lp;

            lpContextCalculator =
                    new LanguageContextCalculator(languageService);
            Object contextManager = lpClass
                    .getMethod("getContextManager")
                    .invoke(lp);
            contextManager.getClass()
                    .getMethod(
                            "registerCalculator",
                            Class.forName(
                                    "net.luckperms.api.context.ContextCalculator"))
                    .invoke(contextManager, lpContextCalculator);

            localizedPrefixProvider = new LocalizedPrefixProvider(
                    (net.luckperms.api.LuckPerms) lp,
                    java.util.logging.Logger.getLogger(
                            "NetworkLanguage-LP"));
            if (tabListService != null) {
                tabListService.setLocalizedPrefixProvider(
                        localizedPrefixProvider);
            }

            logger.info(
                    "LuckPerms ContextCalculator registrato.");
        } catch (Exception exception) {
            logger.warn(
                    "LuckPerms non disponibile, context lingua non attivi.");
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
                tabScheduler.awaitTermination(
                        5, TimeUnit.SECONDS);
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
        if (luckPermsInstance != null
                && lpContextCalculator != null) {
            try {
                Object contextManager = luckPermsInstance
                        .getClass()
                        .getMethod("getContextManager")
                        .invoke(luckPermsInstance);
                contextManager.getClass()
                        .getMethod(
                                "unregisterCalculator",
                                Class.forName(
                                        "net.luckperms.api.context.ContextCalculator"))
                        .invoke(contextManager, lpContextCalculator);
            } catch (Exception ignored) {
                // LuckPerms may already be shutting down.
            }
        }
        if (playerRepository instanceof AutoCloseable) {
            try {
                ((AutoCloseable) playerRepository).close();
            } catch (Exception exception) {
                logger.warn(
                        "Errore chiusura repository lingua: {}",
                        exception.getMessage());
            }
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
                    tabListService.sendScheduled(
                            event.getPlayer(), true);
                }
            }

            @Subscribe
            public void onServerConnect(
                    ServerPostConnectEvent event) {
                if (tabListService != null) {
                    tabListService.sendScheduled(
                            event.getPlayer(), true);
                }
            }

            @Subscribe
            public void onDisconnect(DisconnectEvent event) {
                if (tabListService != null) {
                    tabListService.clear(event.getPlayer());
                }
            }

            @Subscribe
            public void onSettingsChanged(
                    PlayerSettingsChangedEvent event) {
                if (tabListService != null) {
                    tabListService.sendScheduled(
                            event.getPlayer(), true);
                }
            }
        });
    }

    private void registerCommand(
            TranslationService translations) {
        CommandMeta langMeta = proxy.getCommandManager()
                .metaBuilder("networklang")
                .plugin(this)
                .aliases("langproxy", "nlang")
                .build();
        proxy.getCommandManager().register(
                langMeta,
                new LanguageCommand(
                        languageService,
                        synchronizationService,
                        translations,
                        logger,
                        tabListService));

        CommandMeta reloadMeta = proxy.getCommandManager()
                .metaBuilder("networklangreload")
                .plugin(this)
                .build();
        proxy.getCommandManager().register(
                reloadMeta,
                new NetworkLangReloadCommand(this));
    }

    public void reloadAll() {
        if (tabListService != null) {
            tabListService.reload();
        }
        logger.info(
                "NetworkLanguage reload completato.");
    }

    public VelocityTabListService getTabListService() {
        return tabListService;
    }

    void saveResource(String resource) throws IOException {
        Path target = dataDirectory.resolve(resource);
        if (Files.exists(target)) {
            return;
        }
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        try (InputStream in = getClass()
                .getClassLoader()
                .getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException(
                        "Risorsa incorporata mancante: "
                                + resource);
            }
            Files.copy(
                    in,
                    target,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void initializeStorage() {
        Path configFile = dataDirectory.resolve("config.yml");
        @SuppressWarnings("unchecked")
        Map<String, Object> config = loadYaml(configFile);
        if (config == null) {
            initSqlite(dataDirectory);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> storage =
                (Map<String, Object>) config.get("storage");
        String storageType = storage != null
                ? getString(storage, "type", "sqlite")
                : "sqlite";

        if ("postgresql".equalsIgnoreCase(storageType)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pg =
                    (Map<String, Object>) config.get("postgresql");
            if (pg == null) {
                throw new IllegalStateException(
                        "PostgreSQL selezionato ma sezione postgresql mancante");
            }
            String host = getString(pg, "host", "localhost");
            int port = getInt(pg, "port", 5432);
            String database = getString(
                    pg, "database", "networklanguage");
            String username = getString(
                    pg, "username", "postgres");
            String password = getString(pg, "password", "");
            proxyId = getString(pg, "proxy-id", "velocity-1");

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(
                    "jdbc:postgresql://"
                            + host + ":" + port
                            + "/" + database);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(
                    getInt(pg, "pool-size", 10));
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(
                    getInt(pg, "connection-timeout-ms", 5000));

            hikariDataSource =
                    new HikariDataSource(hikariConfig);
            postgresRepository =
                    new PostgresPlayerLanguageRepository(
                            hikariDataSource, proxyId);
            playerRepository = postgresRepository;

            notificationService =
                    new PostgresLanguageNotificationService(
                            host,
                            port,
                            database,
                            username,
                            password,
                            proxyId,
                            java.util.logging.Logger.getLogger(
                                    "NetworkLanguage-PG"));
            notificationService.start();

            postgresRepository.setNotificationCallback(
                    commitEvent -> {
                        try (java.sql.Connection conn =
                                     hikariDataSource.getConnection()) {
                            PostgresLanguageNotificationService
                                    .notifyChange(
                                            conn,
                                            commitEvent.playerUuid,
                                            commitEvent.revision,
                                            commitEvent.languageCode,
                                            commitEvent.locale,
                                            proxyId);
                        } catch (java.sql.SQLException exception) {
                            logger.warn(
                                    "NOTIFY failed after commit: {}",
                                    exception.getMessage());
                        }
                    });

            logger.info(
                    "PostgreSQL storage attivo per il proxy {}.",
                    proxyId);
            return;
        }

        if (!"sqlite".equalsIgnoreCase(storageType)) {
            throw new IllegalArgumentException(
                    "storage.type non supportato: "
                            + storageType);
        }
        initSqlite(dataDirectory);
    }

    private void initSqlite(Path dataDir) {
        proxyId = "velocity-1";
        PlayerLanguageRepositoryFactory.RepositoryResult result =
                PlayerLanguageRepositoryFactory.create(
                        "sqlite",
                        dataDir,
                        proxyId,
                        java.util.logging.Logger.getLogger(
                                "NetworkLanguage-SQLite"));
        playerRepository = result.repository;
        postgresRepository = null;
        hikariDataSource = null;
        notificationService = null;
        logger.info(result.message);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(Path file) {
        try {
            Yaml yaml = new Yaml();
            try (InputStreamReader reader =
                         new InputStreamReader(
                                 Files.newInputStream(file),
                                 StandardCharsets.UTF_8)) {
                Object loaded = yaml.load(reader);
                if (loaded instanceof Map) {
                    return (Map<String, Object>) loaded;
                }
            }
        } catch (IOException exception) {
            logger.warn(
                    "Impossibile leggere config.yml: {}",
                    exception.getMessage());
        }
        return null;
    }

    private static int getInt(Map<String, Object> map,
                              String key,
                              int def) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return def;
    }

    private static String getString(Map<String, Object> map,
                                    String key,
                                    String def) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return def;
    }
}
