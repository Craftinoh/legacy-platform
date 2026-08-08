package it.legacynetwork.screenshare;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.legacynetwork.language.PlayerLanguageProvider;
import it.legacynetwork.language.PlayerLanguageProviderHolder;
import it.legacynetwork.reports.api.LegacyReportsApi;
import it.legacynetwork.reports.api.LegacyReportsApiHolder;
import it.legacynetwork.screenshare.command.ScreenshareCommandHandler;
import it.legacynetwork.screenshare.config.ConfigSection;
import it.legacynetwork.screenshare.config.ScreenshareConfiguration;
import it.legacynetwork.screenshare.config.ScreenshareConfigurationException;
import it.legacynetwork.screenshare.message.ScreenshareLanguageResolver;
import it.legacynetwork.screenshare.message.ScreenshareMessages;
import it.legacynetwork.screenshare.message.ScreensharePresenter;
import it.legacynetwork.screenshare.reports.ReportLink;
import it.legacynetwork.screenshare.repository.InMemoryScreenshareEventRepository;
import it.legacynetwork.screenshare.repository.InMemoryScreenshareRepository;
import it.legacynetwork.screenshare.repository.JdbcScreenshareEventRepository;
import it.legacynetwork.screenshare.repository.JdbcScreenshareRepository;
import it.legacynetwork.screenshare.repository.ScreenshareEventRepository;
import it.legacynetwork.screenshare.repository.ScreenshareRepository;
import it.legacynetwork.screenshare.repository.ScreenshareSchemaMigrator;
import it.legacynetwork.screenshare.service.ScreenshareService;
import it.legacynetwork.screenshare.session.ActiveSessionRegistry;
import it.legacynetwork.screenshare.session.ServerSwitchPolicy;
import it.legacynetwork.screenshare.session.TargetCommandPolicy;
import it.legacynetwork.screenshare.velocity.ConnectionListener;
import it.legacynetwork.screenshare.velocity.ScreenshareCommand;
import it.legacynetwork.screenshare.velocity.ServerSwitchListener;
import it.legacynetwork.screenshare.velocity.TargetCommandListener;
import it.legacynetwork.screenshare.velocity.VelocityPlayerDirectory;
import it.legacynetwork.screenshare.velocity.VelocityTransferGateway;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Plugin proxy di LegacyScreenshare.
 *
 * <p>Gestisce le sessioni di controllo interamente sul proxy: assegnazione,
 * trasferimento sul server dedicato, blocco dei cambi server, scadenze,
 * disconnessioni e collegamento al report.</p>
 *
 * <p>Non esiste alcun congelamento di movimento o inventario: quelli vivono sul
 * server e da qui non sono raggiungibili. Nessuna punizione viene applicata:
 * LegacyPunishments non esiste ancora, e la violazione viene consegnata a una
 * porta che oggi si limita a registrarla.</p>
 *
 * <p>NetworkLanguage e LegacyReports sono dipendenze reali: le loro classi non
 * vengono spedite in questo artefatto.</p>
 */
@Plugin(id = "legacyscreenshare", name = "LegacyScreenshare", version = "0.1.0",
        authors = {"LegacyNetwork"},
        dependencies = {@Dependency(id = "networklanguage"),
                @Dependency(id = "legacyreports")})
public final class LegacyScreensharePlugin {

    private static final String NETWORK_LANGUAGE_ID = "networklanguage";
    private static final String LEGACY_REPORTS_ID = "legacyreports";

    /** Intervallo della passata su scadenze e finestre di rientro. */
    private static final long TICK_SECONDS = 5L;

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private HikariDataSource dataSource;
    private ExecutorService databaseExecutor;
    private ActiveSessionRegistry registry;
    private ScreenshareService service;

    @Inject
    public LegacyScreensharePlugin(ProxyServer proxy, Logger logger,
                                   @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        PlayerLanguageProvider languageProvider = networkLanguageProvider();
        if (languageProvider == null) {
            logger.error("LegacyScreenshare non parte: NetworkLanguage non e'"
                    + " disponibile e ogni messaggio visibile passa da li'.");
            return;
        }
        LegacyReportsApi reportsApi = legacyReportsApi();
        if (reportsApi == null) {
            logger.error("LegacyScreenshare non parte: LegacyReports non ha"
                    + " pubblicato la propria API e i controlli devono poterla"
                    + " aggiornare.");
            return;
        }

        ConfigSection root = ConfigSection.of(loadConfiguration());
        ScreenshareConfiguration configuration;
        try {
            configuration = ScreenshareConfiguration.fromRoot(root);
        } catch (ScreenshareConfigurationException invalid) {
            logger.error("LegacyScreenshare non parte: {}",
                    invalid.getMessage());
            return;
        }
        if (!configuration.hasServer()) {
            logger.warn("LegacyScreenshare: 'screenshare.server' non"
                    + " configurato, /ss start restera' rifiutato.");
        }

        ConfigSection database = root.section("database");
        String jdbcUrl = database.text("jdbc-url", "");
        databaseExecutor = Executors.newFixedThreadPool(
                Math.max(2, database.number("pool-size", 4)), runnable -> {
                    Thread thread = new Thread(runnable, "legacyscreenshare-db");
                    thread.setDaemon(true);
                    return thread;
                });

        ScreenshareRepository sessions;
        ScreenshareEventRepository events;
        if (jdbcUrl.isEmpty()) {
            logger.warn("LegacyScreenshare: 'database.jdbc-url' non"
                    + " configurato, le sessioni restano solo in memoria.");
            sessions = new InMemoryScreenshareRepository();
            events = new InMemoryScreenshareEventRepository();
        } else {
            dataSource = buildDataSource(database, jdbcUrl);
            new ScreenshareSchemaMigrator(dataSource).migrate();
            sessions = new JdbcScreenshareRepository(dataSource,
                    databaseExecutor);
            events = new JdbcScreenshareEventRepository(dataSource,
                    databaseExecutor);
        }

        ScreenshareMessages messages =
                ScreenshareMessages.load(configuration.getFallbackLanguage());
        ScreensharePresenter presenter = new ScreensharePresenter(messages);
        ScreenshareLanguageResolver languages = new ScreenshareLanguageResolver(
                languageProvider, configuration.getFallbackLanguage());
        registry = new ActiveSessionRegistry();

        VelocityPlayerDirectory directory = new VelocityPlayerDirectory(proxy);
        service = new ScreenshareService(configuration,
                sessions, events, new VelocityTransferGateway(proxy), directory,
                presenter, languages, new ReportLink(reportsApi),
                new it.legacynetwork.screenshare.violation
                        .AuditOnlyScreenshareViolationHandler(logger::warn),
                registry, Instant::now);

        ServerSwitchPolicy switchPolicy =
                new ServerSwitchPolicy(registry, configuration);
        TargetCommandPolicy commandPolicy =
                new TargetCommandPolicy(registry, configuration);

        proxy.getEventManager().register(this, new ServerSwitchListener(
                switchPolicy, registry, service, presenter, languages));
        proxy.getEventManager().register(this, new TargetCommandListener(
                commandPolicy, registry, service, presenter, languages));
        proxy.getEventManager().register(this,
                new ConnectionListener(service, registry));

        ScreenshareCommandHandler handler = new ScreenshareCommandHandler(
                configuration, service, directory, presenter, languages);
        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("ss")
                        .aliases("screenshare").build(),
                new ScreenshareCommand(handler));

        // Una sola passata centrale: nessun task infinito per sessione.
        proxy.getScheduler().buildTask(this, service::tick)
                .repeat(TICK_SECONDS, TimeUnit.SECONDS)
                .schedule();

        service.recover().thenAccept(closed -> logger.info(
                "LegacyScreenshare avviato: storage {}, sessioni chiuse al"
                        + " ripristino {}, server di controllo '{}'.",
                jdbcUrl.isEmpty() ? "in memoria" : "database", closed,
                configuration.getServer()));
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (service != null) service.beginShutdown();
        if (registry != null) {
            registry.clear();
            registry = null;
        }
        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
            try {
                databaseExecutor.awaitTermination(5L, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            databaseExecutor = null;
        }
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
        service = null;
    }

    private HikariDataSource buildDataSource(ConfigSection database,
                                             String jdbcUrl) {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(jdbcUrl);
        String username = database.text("username", "");
        if (!username.isEmpty()) {
            configuration.setUsername(username);
        }
        String password = database.text("password", "");
        if (!password.isEmpty()) {
            configuration.setPassword(password);
        }
        configuration.setMaximumPoolSize(database.number("pool-size", 4));
        configuration.setPoolName("LegacyScreenshare");
        return new HikariDataSource(configuration);
    }

    private Map<String, Object> loadConfiguration() {
        try {
            Files.createDirectories(dataDirectory);
            Path file = dataDirectory.resolve("config.yml");
            if (!Files.exists(file)) {
                try (InputStream template =
                             getClass().getResourceAsStream("/config.yml")) {
                    if (template != null) {
                        Files.copy(template, file);
                    }
                }
            }
            if (!Files.exists(file)) {
                return Collections.emptyMap();
            }
            try (InputStream stream = Files.newInputStream(file)) {
                Object parsed = new Yaml().load(stream);
                return parsed instanceof Map ? cast(parsed)
                        : Collections.emptyMap();
            }
        } catch (IOException unreadable) {
            logger.warn("LegacyScreenshare: config.yml illeggibile ({}).",
                    unreadable.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Provider lingua pubblicato da NetworkLanguage.
     *
     * <p>Stesso collegamento adottato dopo il lavoro su ChickenWars: plugin
     * manager, istanza, interfaccia condivisa. Nessuna reflection.</p>
     */
    private PlayerLanguageProvider networkLanguageProvider() {
        try {
            Optional<PluginContainer> container =
                    proxy.getPluginManager().getPlugin(NETWORK_LANGUAGE_ID);
            if (!container.isPresent()) {
                return null;
            }
            Object instance = container.get().getInstance().orElse(null);
            if (!(instance instanceof PlayerLanguageProviderHolder)) {
                return null;
            }
            return ((PlayerLanguageProviderHolder) instance).languageProvider();
        } catch (RuntimeException | LinkageError unavailable) {
            logger.error("LegacyScreenshare: provider lingua non disponibile"
                    + " ({}).", unavailable.getMessage());
            return null;
        }
    }

    /**
     * API pubblicata da LegacyReports, con lo stesso schema.
     */
    private LegacyReportsApi legacyReportsApi() {
        try {
            Optional<PluginContainer> container =
                    proxy.getPluginManager().getPlugin(LEGACY_REPORTS_ID);
            if (!container.isPresent()) {
                return null;
            }
            Object instance = container.get().getInstance().orElse(null);
            if (!(instance instanceof LegacyReportsApiHolder)) {
                return null;
            }
            return ((LegacyReportsApiHolder) instance).reportsApi();
        } catch (RuntimeException | LinkageError unavailable) {
            logger.error("LegacyScreenshare: API dei report non disponibile"
                    + " ({}).", unavailable.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object raw) {
        return (Map<String, Object>) raw;
    }
}
