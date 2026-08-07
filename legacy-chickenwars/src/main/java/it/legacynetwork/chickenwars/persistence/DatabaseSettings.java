package it.legacynetwork.chickenwars.persistence;

/** Configurazione esterna del datasource PostgreSQL. */
public final class DatabaseSettings {
    private final boolean enabled;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maximumPoolSize;
    private final long connectionTimeoutMillis;

    public DatabaseSettings(boolean enabled, String jdbcUrl, String username,
            String password, int maximumPoolSize, long connectionTimeoutMillis) {
        if (enabled && (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:"))) {
            throw new IllegalArgumentException("JDBC URL non valida");
        }
        if (maximumPoolSize <= 0 || connectionTimeoutMillis < 250L) {
            throw new IllegalArgumentException("Pool database non valido");
        }
        this.enabled = enabled; this.jdbcUrl = jdbcUrl;
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.maximumPoolSize = maximumPoolSize;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }
    public boolean isEnabled() { return enabled; }
    public String getJdbcUrl() { return jdbcUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getMaximumPoolSize() { return maximumPoolSize; }
    public long getConnectionTimeoutMillis() { return connectionTimeoutMillis; }
}
