package it.legacynetwork.language.velocity.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.legacynetwork.language.velocity.luckperms.LocalizedPrefixProvider;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Logger;

public final class PlayerLanguageRepositoryFactory {

    private PlayerLanguageRepositoryFactory() {
    }

    public static RepositoryResult create(String storageType,
                                           Path dataDirectory,
                                           String proxyId,
                                           Logger logger) {
        if ("postgresql".equalsIgnoreCase(storageType)) {
            return new RepositoryResult(
                    null, null, null,
                    "PostgreSQL mode requires HikariCP config. Use createPostgres().");
        }
        String dbName = "languages.db";
        File dbFile = dataDirectory.resolve(dbName).toFile();
        String path = dbFile.getAbsolutePath();
        if (!path.startsWith(dataDirectory.toAbsolutePath().toString())) {
            throw new IllegalArgumentException("SQLite path outside data directory");
        }

        SqlitePlayerLanguageRepository repo = new SqlitePlayerLanguageRepository(
                path, 5000, proxyId, logger);
        try {
            repo.initialize();
            logger.info("SQLite storage enabled: single-proxy mode");
            logger.info("SQLite database: " + dbFile.getName());
            return new RepositoryResult(repo, null, null,
                    "SQLite initialized successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite: "
                    + e.getMessage(), e);
        }
    }

    public static RepositoryResult createPostgres(String host, int port,
                                                    String database,
                                                    String username,
                                                    String password,
                                                    String proxyId,
                                                    Logger logger) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:postgresql://" + host + ":"
                + port + "/" + database);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        PostgresPlayerLanguageRepository repo =
                new PostgresPlayerLanguageRepository(dataSource, proxyId);
        logger.info("PostgreSQL storage enabled: multi-proxy mode");

        PostgresLanguageNotificationService notifyService =
                new PostgresLanguageNotificationService(
                        host, port, database, username, password, proxyId, logger);
        notifyService.start();

        repo.setNotificationCallback(commitEvent -> {
            try (java.sql.Connection conn = dataSource.getConnection()) {
                PostgresLanguageNotificationService.notifyChange(conn,
                        commitEvent.playerUuid, commitEvent.revision,
                        commitEvent.languageCode, commitEvent.locale, proxyId);
            } catch (java.sql.SQLException e) {
                logger.warning("NOTIFY failed after commit: " + e.getMessage());
            }
        });

        return new RepositoryResult(repo, dataSource, notifyService,
                "PostgreSQL initialized successfully.");
    }

    public static final class RepositoryResult {
        public final PlayerLanguageRepository repository;
        public final HikariDataSource dataSource;
        public final PostgresLanguageNotificationService notificationService;
        public final String message;

        RepositoryResult(PlayerLanguageRepository repository,
                         HikariDataSource dataSource,
                         PostgresLanguageNotificationService notificationService,
                         String message) {
            this.repository = repository;
            this.dataSource = dataSource;
            this.notificationService = notificationService;
            this.message = message;
        }
    }
}
