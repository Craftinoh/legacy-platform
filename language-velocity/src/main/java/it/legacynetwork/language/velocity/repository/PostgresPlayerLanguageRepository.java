package it.legacynetwork.language.velocity.repository;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PostgresPlayerLanguageRepository implements PlayerLanguageRepository, AutoCloseable {

    private static final String FIND_SQL =
            "SELECT language_code, client_locale, revision FROM player_languages WHERE player_uuid = ?";

    private static final String UPSERT_SQL =
            "INSERT INTO player_languages (player_uuid, language_code, client_locale) VALUES (?, ?, ?) "
                    + "ON CONFLICT (player_uuid) DO UPDATE SET language_code = ?, client_locale = ?, "
                    + "revision = revision + 1, updated_at = NOW()";

    private static final String REQUEST_EXISTS_SQL =
            "SELECT 1 FROM player_language_changes WHERE request_id = ?";

    private static final String LOCK_ROW_SQL =
            "SELECT language_code, revision FROM player_languages WHERE player_uuid = ? FOR UPDATE";

    private static final String COOLDOWN_SQL =
            "SELECT MAX(changed_at) FROM player_language_changes WHERE player_uuid = ? AND result = 'SUCCESS'";

    private static final String HOURLY_LIMIT_SQL =
            "SELECT COUNT(*) FROM player_language_changes "
                    + "WHERE player_uuid = ? AND result = 'SUCCESS' "
                    + "AND changed_at > NOW() - make_interval(mins => ?)";

    private static final String UPDATE_LANG_SQL =
            "UPDATE player_languages SET language_code = ?, client_locale = ?, "
                    + "revision = revision + 1, updated_at = NOW() WHERE player_uuid = ?";

    private static final String INSERT_CHANGE_SQL =
            "INSERT INTO player_language_changes (request_id, player_uuid, old_language, new_language, proxy_id, result) "
                    + "VALUES (?, ?, ?, ?, ?, 'SUCCESS')";

    private final HikariDataSource dataSource;
    private final String proxyId;
    private final ExecutorService executor;
    private Consumer<CommitEvent> notificationCallback;
    private Logger debugLogger;

    public PostgresPlayerLanguageRepository(HikariDataSource dataSource, String proxyId) {
        this.dataSource = dataSource;
        this.proxyId = proxyId;
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "networklang-db");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public CompletableFuture<Optional<LanguageState>> find(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(FIND_SQL)) {
                ps.setObject(1, playerUuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new LanguageState(
                                rs.getString("language_code"),
                                rs.getString("client_locale"),
                                rs.getLong("revision")
                        ));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw new RepositoryException("Failed to find language state", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> upsertState(UUID playerUuid, String languageCode, String clientLocale) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
                ps.setObject(1, playerUuid);
                ps.setString(2, languageCode);
                ps.setString(3, clientLocale);
                ps.setString(4, languageCode);
                ps.setString(5, clientLocale);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RepositoryException("Failed to upsert language state", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<ChangeResult> changeLanguage(UUID requestId, UUID playerUuid,
                                                           String newLanguage, String newLocale, String proxyId,
                                                           int cooldownSeconds, int maxChangesPerWindow,
                                                           int windowMinutes) {
        return CompletableFuture.supplyAsync(() -> {
            Connection conn = null;
            try {
                conn = dataSource.getConnection();
                conn.setAutoCommit(false);

                if (requestExists(conn, requestId)) {
                    conn.commit();
                    return new ChangeResult(ChangeStatus.SUCCESS, newLanguage, "duplicate_request");
                }

                String currentLang = lockPlayerRow(conn, playerUuid);
                if (currentLang == null) {
                    conn.rollback();
                    return new ChangeResult(ChangeStatus.DATABASE_ERROR, newLanguage, "player_not_found");
                }

                if (currentLang.equalsIgnoreCase(newLanguage)) {
                    conn.rollback();
                    return new ChangeResult(ChangeStatus.ALREADY_SELECTED, newLanguage, "already_selected");
                }

                if (isInCooldown(conn, playerUuid, cooldownSeconds)) {
                    conn.rollback();
                    return new ChangeResult(ChangeStatus.CHANGE_COOLDOWN, newLanguage, "cooldown_active");
                }

                if (isOverLimit(conn, playerUuid, maxChangesPerWindow, windowMinutes)) {
                    conn.rollback();
                    return new ChangeResult(ChangeStatus.HOURLY_LIMIT, newLanguage, "hourly_limit");
                }

                updatePlayerLanguage(conn, playerUuid, newLanguage, newLocale);
                insertChangeRecord(conn, requestId, playerUuid, currentLang, newLanguage, proxyId);

                conn.commit();

                long newRevision = getRevision(conn, playerUuid);
                String currentLocale = newLocale;

                if (notificationCallback != null) {
                    try {
                        notificationCallback.accept(new CommitEvent(
                                playerUuid, newRevision, newLanguage, currentLocale));
                    } catch (Exception e) {
                        if (debugLogger != null) {
                            debugLogger.warning("Notify callback failed after commit: "
                                    + e.getMessage());
                        }
                    }
                }

                return new ChangeResult(ChangeStatus.SUCCESS, newLanguage, "changed");
            } catch (SQLException e) {
                rollback(conn);
                return new ChangeResult(ChangeStatus.DATABASE_ERROR, newLanguage, "database_error");
            } finally {
                closeQuietly(conn);
            }
        }, executor);
    }

    private boolean requestExists(Connection conn, UUID requestId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(REQUEST_EXISTS_SQL)) {
            ps.setObject(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String lockPlayerRow(Connection conn, UUID playerUuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(LOCK_ROW_SQL)) {
            ps.setObject(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("language_code");
                }
                return null;
            }
        }
    }

    private boolean isInCooldown(Connection conn, UUID playerUuid, int cooldownSeconds) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(COOLDOWN_SQL)) {
            ps.setObject(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp lastChange = rs.getTimestamp(1);
                    if (lastChange != null) {
                        long elapsed = System.currentTimeMillis() - lastChange.getTime();
                        return elapsed < (long) cooldownSeconds * 1000L;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOverLimit(Connection conn, UUID playerUuid,
                                 int maxChangesPerWindow, int windowMinutes) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(HOURLY_LIMIT_SQL)) {
            ps.setObject(1, playerUuid);
            ps.setInt(2, windowMinutes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) >= maxChangesPerWindow;
                }
            }
        }
        return false;
    }

    private void updatePlayerLanguage(Connection conn, UUID playerUuid,
                                       String newLanguage, String newLocale) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_LANG_SQL)) {
            ps.setString(1, newLanguage);
            ps.setString(2, newLocale);
            ps.setObject(3, playerUuid);
            ps.executeUpdate();
        }
    }

    private void insertChangeRecord(Connection conn, UUID requestId, UUID playerUuid,
                                     String oldLanguage, String newLanguage, String proxyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_CHANGE_SQL)) {
            ps.setObject(1, requestId);
            ps.setObject(2, playerUuid);
            ps.setString(3, oldLanguage);
            ps.setString(4, newLanguage);
            ps.setString(5, proxyId);
            ps.executeUpdate();
        }
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private long getRevision(Connection conn, UUID playerUuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT revision FROM player_languages WHERE player_uuid = ?")) {
            ps.setObject(1, playerUuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("revision");
            }
        }
        return 0L;
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private static final class RepositoryException extends RuntimeException {
        private RepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public void setNotificationCallback(Consumer<CommitEvent> notificationCallback) {
        this.notificationCallback = notificationCallback;
    }

    public void setDebugLogger(Logger debugLogger) {
        this.debugLogger = debugLogger;
    }

    public static final class CommitEvent {
        public final UUID playerUuid;
        public final long revision;
        public final String languageCode;
        public final String locale;

        public CommitEvent(UUID playerUuid, long revision,
                            String languageCode, String locale) {
            this.playerUuid = playerUuid;
            this.revision = revision;
            this.languageCode = languageCode;
            this.locale = locale;
        }
    }
}
