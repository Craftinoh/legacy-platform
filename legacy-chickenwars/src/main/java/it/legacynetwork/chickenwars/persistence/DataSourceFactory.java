package it.legacynetwork.chickenwars.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** Costruisce il pool Java 8 senza conoscere Bukkit. */
public final class DataSourceFactory {
    private DataSourceFactory() { }
    public static HikariDataSource create(DatabaseSettings settings) {
        if (settings == null || !settings.isEnabled()) {
            throw new IllegalArgumentException("Database non abilitato");
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.getJdbcUrl());
        config.setUsername(settings.getUsername());
        config.setPassword(settings.getPassword());
        config.setMaximumPoolSize(settings.getMaximumPoolSize());
        config.setMinimumIdle(0);
        config.setConnectionTimeout(settings.getConnectionTimeoutMillis());
        config.setValidationTimeout(Math.max(250L,
                Math.min(5000L, settings.getConnectionTimeoutMillis())));
        config.setPoolName("chickenwars-database");
        config.setInitializationFailTimeout(-1L);
        return new HikariDataSource(config);
    }
}
