package it.legacynetwork.chickenwars.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.legacynetwork.chickenwars.routing.JdbcReconnectCoordinator;
import it.legacynetwork.chickenwars.routing.JdbcReconnectSessionInspector;
import it.legacynetwork.chickenwars.routing.JdbcRoutingCoordinator;
import it.legacynetwork.chickenwars.routing.RejoinVerdictCodec;
import it.legacynetwork.chickenwars.velocity.command.ChickenWarsCommand;
import it.legacynetwork.chickenwars.velocity.message.RejoinLanguageResolver;
import it.legacynetwork.chickenwars.velocity.message.RejoinMessages;
import it.legacynetwork.chickenwars.velocity.rejoin.BackendVerdictRegistry;
import it.legacynetwork.chickenwars.velocity.rejoin.RejoinAttemptRegistry;
import it.legacynetwork.chickenwars.velocity.rejoin.RejoinCommandPolicy;
import it.legacynetwork.chickenwars.velocity.rejoin.RejoinCoordinator;
import it.legacynetwork.chickenwars.velocity.rejoin.RejoinSettings;
import it.legacynetwork.chickenwars.velocity.transfer.BackendVerdictListener;
import it.legacynetwork.chickenwars.velocity.transfer.VelocityTransferGateway;
import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;
import it.legacynetwork.language.PlayerLanguageProviderHolder;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Plugin proxy di ChickenWars: espone soltanto {@code /cw rejoin}.
 *
 * <p>Non contiene logica di gioco. Legge lo stesso schema del backend tramite i
 * contratti condivisi e delega ogni decisione al sistema distribuito gia'
 * esistente.</p>
 */
@Plugin(id = "legacy-chickenwars-proxy", name = "LegacyChickenWarsProxy",
        version = "0.1.0", authors = {"LegacyNetwork"},
        // Dipendenza reale: NetworkLanguage fornisce a runtime le classi di
        // language-common, che questo artefatto non spedisce.
        dependencies = {@Dependency(id = "networklanguage")})
public final class ChickenWarsProxyPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    /** Identificatore del plugin che gestisce le lingue del network. */
    private static final String NETWORK_LANGUAGE_ID = "networklanguage";

    private HikariDataSource dataSource;
    private ExecutorService databaseExecutor;
    private RejoinAttemptRegistry attempts;
    private BackendVerdictRegistry verdicts;

    @Inject
    public ChickenWarsProxyPlugin(ProxyServer proxy, Logger logger,
                                  @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        Map<String, Object> configuration = loadConfiguration();
        RejoinSettings settings = RejoinSettings.fromMap(
                section(configuration, "chickenwars-rejoin"));

        Map<String, Object> database = section(configuration, "database");
        String jdbcUrl = text(database.get("jdbc-url"));
        if (jdbcUrl.isEmpty()) {
            logger.warn("ChickenWars proxy: 'database.jdbc-url' non configurato,"
                    + " /cw rejoin resta disabilitato.");
            return;
        }

        dataSource = buildDataSource(database, jdbcUrl);
        databaseExecutor = Executors.newFixedThreadPool(
                Math.max(2, number(database.get("pool-size"), 4)));
        attempts = new RejoinAttemptRegistry(settings.getTransferTimeoutMillis()
                + settings.getLookupTimeoutMillis());
        verdicts = new BackendVerdictRegistry(
                (action, delayMillis) -> proxy.getScheduler()
                        .buildTask(this, action)
                        .delay(delayMillis, TimeUnit.MILLISECONDS)
                        .schedule(),
                settings.getTransferTimeoutMillis());

        RejoinCoordinator coordinator = new RejoinCoordinator(
                new JdbcReconnectCoordinator(dataSource, databaseExecutor,
                        settings.getHeartbeatTimeoutMillis(),
                        settings.getReservationTtlMillis()),
                new JdbcRoutingCoordinator(dataSource, databaseExecutor,
                        settings.getHeartbeatTimeoutMillis(),
                        settings.getReservationTtlMillis()),
                new JdbcReconnectSessionInspector(dataSource, databaseExecutor,
                        settings.getHeartbeatTimeoutMillis()),
                new VelocityTransferGateway(proxy),
                attempts, verdicts, System::currentTimeMillis,
                settings.getReconnectTtlMillis());

        proxy.getChannelRegistrar().register(VelocityTransferGateway.channel());
        proxy.getChannelRegistrar().register(
                new LegacyChannelIdentifier(RejoinVerdictCodec.CHANNEL));
        proxy.getEventManager().register(this,
                new BackendVerdictListener(verdicts));

        Language fallback = fallback(configuration);
        RejoinLanguageResolver languages = new RejoinLanguageResolver(
                networkLanguageProvider(), fallback);

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("cw").build(),
                new ChickenWarsCommand(
                        new RejoinCommandPolicy(settings.isEnabled(),
                                settings.getPermission()),
                        coordinator, RejoinMessages.load(fallback), languages));

        logger.info("ChickenWars proxy avviato: /cw rejoin {}, lingue {}.",
                settings.isEnabled() ? "attivo" : "disattivato",
                languages.hasProvider() ? "da NetworkLanguage"
                        : "solo fallback " + fallback.getCode());
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (attempts != null) {
            attempts.clear();
            attempts = null;
        }
        if (verdicts != null) {
            verdicts.clear();
            verdicts = null;
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
    }

    private HikariDataSource buildDataSource(Map<String, Object> database,
                                             String jdbcUrl) {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(jdbcUrl);
        String username = text(database.get("username"));
        if (!username.isEmpty()) {
            configuration.setUsername(username);
        }
        String password = text(database.get("password"));
        if (!password.isEmpty()) {
            configuration.setPassword(password);
        }
        configuration.setMaximumPoolSize(number(database.get("pool-size"), 4));
        configuration.setPoolName("ChickenWarsProxy");
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
                try (InputStream template = getClass()
                        .getResourceAsStream("/config.yml")) {
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
                return parsed instanceof Map
                        ? castSection(parsed) : Collections.emptyMap();
            }
        } catch (IOException unreadable) {
            logger.warn("ChickenWars proxy: config.yml illeggibile ({}).",
                    unreadable.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Cerca il provider lingua pubblicato da NetworkLanguage.
     *
     * <p>La dipendenza e' facoltativa e il tipo condiviso vive in
     * {@code language-common}: se quel plugin non e' installato il comando
     * continua a funzionare usando il fallback configurato.</p>
     *
     * @return il provider, oppure {@code null} se non disponibile
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
            logger.warn("ChickenWars proxy: provider lingua non disponibile ({}).",
                    unavailable.getMessage());
            return null;
        }
    }

    private Language fallback(Map<String, Object> configuration) {
        String code = text(section(configuration, "language").get("fallback"));
        return Language.findByInput(code).orElse(Language.ENGLISH);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castSection(Object raw) {
        return (Map<String, Object>) raw;
    }

    private static Map<String, Object> section(Map<String, Object> root,
                                               String key) {
        Object value = root == null ? null : root.get(key);
        return value instanceof Map ? castSection(value)
                : Collections.<String, Object>emptyMap();
    }

    private static String text(Object raw) {
        return raw == null ? "" : String.valueOf(raw).trim();
    }

    private static int number(Object raw, int fallback) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return raw == null ? fallback
                    : Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }
}
