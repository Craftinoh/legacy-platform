package it.legacynetwork.reports;

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
import it.legacynetwork.reports.command.CooldownRegistry;
import it.legacynetwork.reports.command.ReportCommandHandler;
import it.legacynetwork.reports.command.ReportsCommandHandler;
import it.legacynetwork.reports.config.ConfigSection;
import it.legacynetwork.reports.config.ReportsConfiguration;
import it.legacynetwork.reports.message.ReportLanguageResolver;
import it.legacynetwork.reports.message.ReportMessages;
import it.legacynetwork.reports.message.ReportPresenter;
import it.legacynetwork.reports.notification.StaffNotificationPreferences;
import it.legacynetwork.reports.notification.StaffNotificationService;
import it.legacynetwork.reports.repository.InMemoryReportEventRepository;
import it.legacynetwork.reports.repository.InMemoryReportRepository;
import it.legacynetwork.reports.repository.JdbcReportEventRepository;
import it.legacynetwork.reports.repository.JdbcReportRepository;
import it.legacynetwork.reports.repository.ReportEventRepository;
import it.legacynetwork.reports.repository.ReportRepository;
import it.legacynetwork.reports.repository.ReportSchemaMigrator;
import it.legacynetwork.reports.service.DefaultLegacyReportsApi;
import it.legacynetwork.reports.service.ReportService;
import it.legacynetwork.reports.velocity.PlayerCleanupListener;
import it.legacynetwork.reports.velocity.ReportCommand;
import it.legacynetwork.reports.velocity.ReportsCommand;
import it.legacynetwork.reports.velocity.VelocityPlayerDirectory;
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
 * Plugin proxy di LegacyReports.
 *
 * <p>Gestisce le segnalazioni della rete interamente sul proxy: comandi,
 * messaggi Adventure, paginazione e pulsanti cliccabili. Non apre inventari e
 * non conosce alcuna API di server, perche' non ne ha nessuna a disposizione.</p>
 *
 * <p>NetworkLanguage e' una dipendenza reale: le classi lingua non vengono
 * spedite in questo artefatto e arrivano dal suo classloader a runtime.</p>
 */
@Plugin(id = "legacyreports", name = "LegacyReports", version = "0.1.0",
        authors = {"LegacyNetwork"},
        dependencies = {@Dependency(id = "networklanguage")})
public final class LegacyReportsPlugin implements LegacyReportsApiHolder {

    /** Identificatore del plugin che gestisce le lingue del network. */
    private static final String NETWORK_LANGUAGE_ID = "networklanguage";

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private HikariDataSource dataSource;
    private ExecutorService databaseExecutor;
    private volatile LegacyReportsApi api;

    @Inject
    public LegacyReportsPlugin(ProxyServer proxy, Logger logger,
                               @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    /**
     * API pubblicata sul proxy, {@code null} finche' il plugin non e' pronto.
     */
    @Override
    public LegacyReportsApi reportsApi() {
        return api;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        ConfigSection root = ConfigSection.of(loadConfiguration());
        ReportsConfiguration configuration = ReportsConfiguration.fromRoot(root);

        ConfigSection database = root.section("database");
        String jdbcUrl = database.text("jdbc-url", "");
        databaseExecutor = Executors.newFixedThreadPool(
                Math.max(2, database.number("pool-size", 4)), runnable -> {
                    Thread thread = new Thread(runnable, "legacyreports-db");
                    thread.setDaemon(true);
                    return thread;
                });

        ReportRepository reports;
        ReportEventRepository events;
        if (jdbcUrl.isEmpty()) {
            // Senza database il plugin resta usabile, ma nulla sopravvive a un
            // riavvio: e' una modalita' di prova, non di produzione.
            logger.warn("LegacyReports: 'database.jdbc-url' non configurato,"
                    + " i report restano solo in memoria.");
            reports = new InMemoryReportRepository();
            events = new InMemoryReportEventRepository();
        } else {
            dataSource = buildDataSource(database, jdbcUrl);
            new ReportSchemaMigrator(dataSource).migrate();
            reports = new JdbcReportRepository(dataSource, databaseExecutor);
            events = new JdbcReportEventRepository(dataSource,
                    databaseExecutor);
        }

        ReportService service = new ReportService(reports, events,
                Instant::now, configuration.getProxyId());
        api = new DefaultLegacyReportsApi(service);

        ReportMessages messages =
                ReportMessages.load(configuration.getFallbackLanguage());
        ReportLanguageResolver languages = new ReportLanguageResolver(
                networkLanguageProvider(), configuration.getFallbackLanguage());
        ReportPresenter presenter =
                new ReportPresenter(messages, configuration.getReasons());

        VelocityPlayerDirectory directory = new VelocityPlayerDirectory(proxy);
        StaffNotificationPreferences preferences =
                new StaffNotificationPreferences(
                        configuration.isNotifyByDefault());
        StaffNotificationService notifications = new StaffNotificationService(
                directory, presenter, languages, preferences,
                configuration.getPermissions().getStaffView());
        CooldownRegistry cooldowns = new CooldownRegistry();

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("report").build(),
                new ReportCommand(new ReportCommandHandler(configuration,
                        service, directory, presenter, languages, notifications,
                        cooldowns, Instant::now)));
        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("reports").build(),
                new ReportsCommand(new ReportsCommandHandler(configuration,
                        service, directory, presenter, languages,
                        preferences)));
        proxy.getEventManager().register(this,
                new PlayerCleanupListener(cooldowns, preferences));

        logger.info("LegacyReports avviato: storage {}, motivi attivi {},"
                        + " lingue {}.",
                jdbcUrl.isEmpty() ? "in memoria" : "database",
                configuration.getReasons().enabled().size(),
                languages.hasProvider() ? "da NetworkLanguage"
                        : "solo fallback "
                        + configuration.getFallbackLanguage().getCode());
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        api = null;
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
        configuration.setPoolName("LegacyReports");
        return new HikariDataSource(configuration);
    }

    /**
     * Legge {@code config.yml}, creandolo dal modello incluso se assente.
     */
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
            logger.warn("LegacyReports: config.yml illeggibile ({}).",
                    unreadable.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Cerca il provider lingua pubblicato da NetworkLanguage.
     *
     * <p>Stesso collegamento usato dal proxy di ChickenWars: plugin manager,
     * istanza, interfaccia condivisa. Nessuna reflection.</p>
     *
     * @return il provider, oppure {@code null} se non ancora disponibile
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
            logger.warn("LegacyReports: provider lingua non disponibile ({}).",
                    unavailable.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object raw) {
        return (Map<String, Object>) raw;
    }
}
