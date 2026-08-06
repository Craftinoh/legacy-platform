package it.legacynetwork.language.velocity.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class PostgresLanguageNotificationService implements AutoCloseable {

    private static final String CHANNEL = "legacy_language_updates";
    private static final long RECONNECT_DELAY_MS = 5000;
    private static final long MAX_RECONNECT_DELAY_MS = 60000;

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String proxyId;
    private final Logger logger;
    private final AtomicLong latestRevision = new AtomicLong(0);
    private final ConcurrentHashMap<UUID, Long> revisionCache = new ConcurrentHashMap<>();
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "pg-listen");
                t.setDaemon(true);
                return t;
            });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private volatile Connection listenerConnection;
    private Consumer<NotificationEvent> callback;

    public PostgresLanguageNotificationService(String host, int port, String database,
                                                String username, String password,
                                                String proxyId, Logger logger) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.proxyId = proxyId;
        this.logger = logger;
    }

    public void setCallback(Consumer<NotificationEvent> callback) {
        this.callback = callback;
    }

    public AtomicLong getLatestRevision() {
        return latestRevision;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor.execute(this::listenLoop);
        logger.info("PostgreSQL LISTEN avviato sul channel " + CHANNEL);
    }

    private void listenLoop() {
        long delay = RECONNECT_DELAY_MS;
        while (running.get()) {
            try {
                Properties props = new Properties();
                props.setProperty("user", username);
                props.setProperty("password", password);
                props.setProperty("ApplicationName", "legacy-listen-" + proxyId);
                String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
                listenerConnection = DriverManager.getConnection(url, props);
                listenerConnection.setAutoCommit(true);

                try (Statement stmt = listenerConnection.createStatement()) {
                    stmt.execute("LISTEN " + CHANNEL);
                }
                connected.set(true);
                delay = RECONNECT_DELAY_MS;
                logger.info("PostgreSQL LISTEN connesso.");

                while (running.get() && !listenerConnection.isClosed()) {
                    try {
                        if (listenerConnection.createStatement()
                                .execute("SELECT 1")) {
                            ResultSet rs = listenerConnection.createStatement()
                                    .getResultSet();
                            rs.close();
                        }
                        org.postgresql.PGNotification[] notifications =
                                ((org.postgresql.PGConnection) listenerConnection)
                                        .getNotifications(5000);
                        if (notifications != null) {
                            for (org.postgresql.PGNotification n : notifications) {
                                if (CHANNEL.equals(n.getName())) {
                                    handleNotification(n.getParameter());
                                }
                            }
                        }
                    } catch (SQLException e) {
                        if (!running.get()) break;
                        connected.set(false);
                        break;
                    }
                }
            } catch (Exception e) {
                connected.set(false);
                if (!running.get()) break;
                logger.warning("LISTEN connection error, retrying in "
                        + delay + "ms: " + e.getMessage());
            } finally {
                closeConnection();
                connected.set(false);
            }
            if (running.get()) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
                delay = Math.min(delay * 2, MAX_RECONNECT_DELAY_MS);
            }
        }
    }

    void handleNotification(String payload) {
        if (payload == null || payload.isEmpty()) return;
        try {
            String[] parts = payload.split("\\|");
            if (parts.length < 2) return;
            UUID playerId = UUID.fromString(parts[0]);
            long revision = Long.parseLong(parts[1]);
            String langCode = parts.length > 2 ? parts[2] : null;
            String locale = parts.length > 3 ? parts[3] : null;
            String sourceProxy = parts.length > 4 ? parts[4] : null;

            if (proxyId.equals(sourceProxy)) return;

            Long cached = revisionCache.get(playerId);
            if (cached != null && cached >= revision) return;

            revisionCache.put(playerId, revision);
            if (revision > latestRevision.get()) {
                latestRevision.set(revision);
            }

            if (callback != null) {
                callback.accept(new NotificationEvent(
                        playerId, revision, langCode, locale, sourceProxy));
            }
        } catch (Exception e) {
            logger.fine("Invalid LISTEN payload: " + e.getMessage());
        }
    }

    public static void notifyChange(Connection connection, UUID playerId,
                                      long revision, String langCode,
                                      String locale, String proxyId)
            throws SQLException {
        String payload = playerId + "|" + revision + "|"
                + (langCode != null ? langCode : "") + "|"
                + (locale != null ? locale : "") + "|"
                + (proxyId != null ? proxyId : "");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SELECT pg_notify('" + CHANNEL + "', '" + payload + "')");
        }
    }

    public CompletableFuture<Long> queryRevision(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://" + host + ":" + port + "/" + database,
                    username, password);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT revision FROM player_languages WHERE player_uuid = ?")) {
                ps.setObject(1, playerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    long rev = rs.getLong("revision");
                    revisionCache.put(playerId, rev);
                    return rev;
                }
                return 0L;
            } catch (SQLException e) {
                return 0L;
            }
        }, executor);
    }

    private void closeConnection() {
        try {
            if (listenerConnection != null && !listenerConnection.isClosed()) {
                listenerConnection.close();
            }
        } catch (SQLException ignored) {
        }
        listenerConnection = null;
    }

    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void close() {
        running.set(false);
        closeConnection();
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public static final class NotificationEvent {
        public final UUID playerId;
        public final long revision;
        public final String languageCode;
        public final String locale;
        public final String sourceProxy;

        public NotificationEvent(UUID playerId, long revision, String languageCode,
                                  String locale, String sourceProxy) {
            this.playerId = playerId;
            this.revision = revision;
            this.languageCode = languageCode;
            this.locale = locale;
            this.sourceProxy = sourceProxy;
        }
    }
}
