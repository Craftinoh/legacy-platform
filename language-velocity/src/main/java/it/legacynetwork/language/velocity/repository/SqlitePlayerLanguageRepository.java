package it.legacynetwork.language.velocity.repository;

import org.sqlite.SQLiteConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public final class SqlitePlayerLanguageRepository
        implements PlayerLanguageRepository, AutoCloseable {

    private final String dbPath;
    private final int busyTimeout;
    private final AtomicLong latestRevision = new AtomicLong(0);
    private final ExecutorService executor;
    private final Logger logger;
    private final SQLiteConfig config;

    public SqlitePlayerLanguageRepository(String dbPath, int busyTimeout,
                                            String proxyId, Logger logger) {
        this.dbPath = dbPath;
        this.busyTimeout = busyTimeout;
        this.logger = logger;
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "sqlite-db");
            t.setDaemon(true);
            return t;
        });
        this.config = new SQLiteConfig();
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        config.enforceForeignKeys(true);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setBusyTimeout(busyTimeout);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
    }

    public void initialize() throws SQLException {
        createTables();
        runMigrations();
    }

    private void createTables() throws SQLException {
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_languages ("
                    + "player_uuid TEXT PRIMARY KEY, language_code TEXT NOT NULL, "
                    + "client_locale TEXT, revision INTEGER NOT NULL DEFAULT 0, "
                    + "updated_at INTEGER NOT NULL)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_language_changes ("
                    + "request_id TEXT PRIMARY KEY, player_uuid TEXT NOT NULL, "
                    + "old_language TEXT, new_language TEXT NOT NULL, "
                    + "result TEXT NOT NULL, changed_at INTEGER NOT NULL, "
                    + "revision INTEGER, proxy_id TEXT)");

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_changes_player_time "
                    + "ON player_language_changes(player_uuid, changed_at)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS schema_migrations ("
                    + "version TEXT PRIMARY KEY, applied_at INTEGER NOT NULL)");
        }
    }

    private void runMigrations() throws SQLException {
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            try {
                ResultSet rs = stmt.executeQuery(
                        "SELECT version FROM schema_migrations WHERE version = 'V1'");
                if (!rs.next()) {
                    stmt.executeUpdate("INSERT INTO schema_migrations "
                            + "(version, applied_at) VALUES ('V1', "
                            + "CAST(strftime('%s','now') AS INTEGER))");
                }
                conn.commit();
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                throw e;
            }
        }
    }

    private Connection openConnection() throws SQLException {
        return config.createConnection("jdbc:sqlite:" + dbPath);
    }

    public AtomicLong getLatestRevision() {
        return latestRevision;
    }

    @Override
    public CompletableFuture<Optional<LanguageState>> find(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT language_code, client_locale, revision "
                                 + "FROM player_languages WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return Optional.of(new LanguageState(
                            rs.getString("language_code"),
                            rs.getString("client_locale"),
                            rs.getLong("revision")));
                }
                return Optional.empty();
            } catch (SQLException e) {
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<ChangeResult> changeLanguage(UUID requestId,
            UUID playerUuid, String newLanguage, String newLocale,
            String proxyId, int cooldownSeconds,
            int maxChangesPerWindow, int windowMinutes) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = openConnection()) {
                conn.setAutoCommit(false);
                try {
                    String reqId = requestId.toString();
                    PreparedStatement checkDup = conn.prepareStatement(
                            "SELECT result, new_language FROM player_language_changes "
                                    + "WHERE request_id = ?");
                    checkDup.setString(1, reqId);
                    ResultSet dupRs = checkDup.executeQuery();
                    if (dupRs.next()) {
                        conn.commit();
                        return new ChangeResult(ChangeStatus.SUCCESS,
                                dupRs.getString("new_language"), "duplicate");
                    }

                    PreparedStatement lock = conn.prepareStatement(
                            "SELECT language_code, revision FROM player_languages "
                                    + "WHERE player_uuid = ?");
                    lock.setString(1, playerUuid.toString());
                    ResultSet lockRs = lock.executeQuery();
                    String currentLang = lockRs.next()
                            ? lockRs.getString("language_code") : null;
                    long currentRevision = lockRs.wasNull() ? 0
                            : lockRs.getLong("revision");

                    if (currentLang != null
                            && currentLang.equalsIgnoreCase(newLanguage)) {
                        conn.rollback();
                        return new ChangeResult(ChangeStatus.ALREADY_SELECTED,
                                newLanguage, "already_selected");
                    }

                    long now = System.currentTimeMillis();
                    long cooldownMs = (long) cooldownSeconds * 1000;
                    PreparedStatement cdStmt = conn.prepareStatement(
                            "SELECT MAX(changed_at) FROM player_language_changes "
                                    + "WHERE player_uuid = ? AND result = 'SUCCESS'");
                    cdStmt.setString(1, playerUuid.toString());
                    ResultSet cdRs = cdStmt.executeQuery();
                    if (cdRs.next() && cdRs.getLong(1) > 0) {
                        long lastChange = cdRs.getLong(1);
                        if (now - lastChange < cooldownMs) {
                            conn.rollback();
                            return new ChangeResult(ChangeStatus.CHANGE_COOLDOWN,
                                    null, "cooldown");
                        }
                    }

                    long windowMs = (long) windowMinutes * 60 * 1000;
                    PreparedStatement limitStmt = conn.prepareStatement(
                            "SELECT COUNT(*) FROM player_language_changes "
                                    + "WHERE player_uuid = ? AND result = 'SUCCESS' "
                                    + "AND changed_at > ?");
                    limitStmt.setString(1, playerUuid.toString());
                    limitStmt.setLong(2, now - windowMs);
                    ResultSet limitRs = limitStmt.executeQuery();
                    if (limitRs.next()
                            && limitRs.getInt(1) >= maxChangesPerWindow) {
                        conn.rollback();
                        return new ChangeResult(ChangeStatus.HOURLY_LIMIT,
                                null, "hourly_limit");
                    }

                    long newRevision = currentLang != null
                            ? currentRevision + 1 : 1;

                    PreparedStatement upsert = conn.prepareStatement(
                            "INSERT INTO player_languages "
                                    + "(player_uuid, language_code, client_locale, "
                                    + "revision, updated_at) VALUES (?, ?, ?, ?, ?) "
                                    + "ON CONFLICT(player_uuid) DO UPDATE SET "
                                    + "language_code = excluded.language_code, "
                                    + "client_locale = excluded.client_locale, "
                                    + "revision = excluded.revision, "
                                    + "updated_at = excluded.updated_at");
                    upsert.setString(1, playerUuid.toString());
                    upsert.setString(2, newLanguage);
                    upsert.setString(3, newLocale);
                    upsert.setLong(4, newRevision);
                    upsert.setLong(5, now);
                    upsert.executeUpdate();

                    PreparedStatement insertChange = conn.prepareStatement(
                            "INSERT INTO player_language_changes "
                                    + "(request_id, player_uuid, old_language, "
                                    + "new_language, result, changed_at, revision, proxy_id) "
                                    + "VALUES (?, ?, ?, ?, 'SUCCESS', ?, ?, ?)");
                    insertChange.setString(1, reqId);
                    insertChange.setString(2, playerUuid.toString());
                    insertChange.setString(3, currentLang);
                    insertChange.setString(4, newLanguage);
                    insertChange.setLong(5, now);
                    insertChange.setLong(6, newRevision);
                    insertChange.setString(7, proxyId);
                    insertChange.executeUpdate();

                    conn.commit();
                    latestRevision.set(newRevision);

                    return new ChangeResult(ChangeStatus.SUCCESS, newLanguage,
                            "changed");
                } catch (SQLException e) {
                    try { conn.rollback(); } catch (SQLException ignored) {}
                    throw e;
                }
            } catch (SQLException e) {
                return new ChangeResult(ChangeStatus.DATABASE_ERROR,
                        null, "db_error");
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> upsertState(UUID playerUuid,
                                                String languageCode,
                                                String clientLocale) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = openConnection()) {
                conn.setAutoCommit(false);
                try {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO player_languages "
                                    + "(player_uuid, language_code, client_locale, "
                                    + "revision, updated_at) "
                                    + "VALUES (?, ?, ?, "
                                    + "COALESCE((SELECT revision FROM player_languages "
                                    + "WHERE player_uuid = ?), 0), ?) "
                                    + "ON CONFLICT(player_uuid) DO UPDATE SET "
                                    + "language_code = excluded.language_code, "
                                    + "client_locale = excluded.client_locale, "
                                    + "revision = player_languages.revision, "
                                    + "updated_at = excluded.updated_at");
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, languageCode);
                    ps.setString(3, clientLocale);
                    ps.setString(4, playerUuid.toString());
                    ps.setLong(5, System.currentTimeMillis());
                    ps.executeUpdate();
                    conn.commit();
                } catch (SQLException e) {
                    try { conn.rollback(); } catch (SQLException ignored) {}
                    throw e;
                }
            } catch (SQLException ignored) {
            }
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
