package it.legacynetwork.screenshare.repository;

import it.legacynetwork.screenshare.model.ScreenshareOutcome;
import it.legacynetwork.screenshare.model.ScreenshareSession;
import it.legacynetwork.screenshare.model.ScreenshareSessionId;
import it.legacynetwork.screenshare.model.ScreenshareStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Storage delle sessioni su database relazionale.
 *
 * <p>Query parametrizzate, risorse chiuse, aggiornamenti condizionati a stato e
 * revisione, e nulla che giri sul thread eventi del proxy.</p>
 */
public final class JdbcScreenshareRepository implements ScreenshareRepository {

    private static final String COLUMNS =
            "id, target_uuid, target_name, staff_uuid, staff_name, report_id, "
                    + "server_id, created_at, started_at, expires_at, "
                    + "ended_at, status, outcome, notes, proxy_id, revision";

    private static final String INSERT_SQL =
            "INSERT INTO legacy_screenshare_sessions (" + COLUMNS + ") VALUES "
                    + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_SQL =
            "SELECT " + COLUMNS + " FROM legacy_screenshare_sessions "
                    + "WHERE id = ?";

    private static final String UPDATE_SQL =
            "UPDATE legacy_screenshare_sessions SET status = ?, "
                    + "started_at = ?, ended_at = ?, outcome = ?, notes = ?, "
                    + "expires_at = ?, revision = ? "
                    + "WHERE id = ? AND status = ? AND revision = ?";

    private static final Set<ScreenshareStatus> OPEN_STATUSES = EnumSet.of(
            ScreenshareStatus.CREATED, ScreenshareStatus.TRANSFERRING,
            ScreenshareStatus.ACTIVE);

    private final DataSource dataSource;
    private final Executor executor;

    public JdbcScreenshareRepository(DataSource dataSource, Executor executor) {
        if (dataSource == null || executor == null) {
            throw new IllegalArgumentException(
                    "Repository screenshare incompleto");
        }
        this.dataSource = dataSource;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<ScreenshareSession> insert(
            ScreenshareSession session) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(INSERT_SQL)) {
                statement.setString(1, session.getId().storageValue());
                statement.setString(2, SqlValues.uuid(session.getTargetId()));
                statement.setString(3, session.getTargetName());
                statement.setString(4, SqlValues.uuid(session.getStaffId()));
                statement.setString(5, session.getStaffName());
                statement.setString(6, SqlValues.uuid(
                        session.getReportId().orElse(null)));
                statement.setString(7, session.getServerId());
                statement.setLong(8, SqlValues.millis(session.getCreatedAt()));
                statement.setLong(9, SqlValues.millis(
                        session.getStartedAt().orElse(null)));
                statement.setLong(10,
                        SqlValues.millis(session.getExpiresAt()));
                statement.setLong(11, SqlValues.millis(
                        session.getEndedAt().orElse(null)));
                statement.setString(12, session.getStatus().name());
                statement.setString(13, session.getOutcome()
                        .map(ScreenshareOutcome::name).orElse(null));
                statement.setString(14, session.getNotes().orElse(null));
                statement.setString(15, session.getProxyId());
                statement.setLong(16, session.getRevision());
                statement.executeUpdate();
                return session;
            } catch (SQLException failure) {
                throw new ScreenshareRepositoryException(
                        "Inserimento sessione fallito", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<ScreenshareSession>> find(
            ScreenshareSessionId id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(FIND_SQL)) {
                statement.setString(1, id.storageValue());
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows))
                            : Optional.<ScreenshareSession>empty();
                }
            } catch (SQLException failure) {
                throw new ScreenshareRepositoryException(
                        "Lettura sessione fallita", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<ScreenshareSession>> findOpenByTarget(
            UUID targetId) {
        return query("WHERE target_uuid = ? AND status IN ("
                        + placeholders(OPEN_STATUSES.size())
                        + ") ORDER BY created_at DESC",
                statement -> {
                    statement.setString(1, SqlValues.uuid(targetId));
                    bindStatuses(statement, 2, OPEN_STATUSES);
                }).thenApply(sessions -> sessions.isEmpty()
                ? Optional.empty() : Optional.of(sessions.get(0)));
    }

    @Override
    public CompletableFuture<List<ScreenshareSession>> findOpenByStaff(
            UUID staffId) {
        return query("WHERE staff_uuid = ? AND status IN ("
                        + placeholders(OPEN_STATUSES.size())
                        + ") ORDER BY created_at DESC",
                statement -> {
                    statement.setString(1, SqlValues.uuid(staffId));
                    bindStatuses(statement, 2, OPEN_STATUSES);
                });
    }

    @Override
    public CompletableFuture<List<ScreenshareSession>> findOpen() {
        return query("WHERE status IN (" + placeholders(OPEN_STATUSES.size())
                        + ") ORDER BY created_at DESC",
                statement -> bindStatuses(statement, 1, OPEN_STATUSES));
    }

    @Override
    public CompletableFuture<ScreensharePage> listByStatuses(
            Set<ScreenshareStatus> statuses, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, pageSize);
        List<ScreenshareStatus> filter = statuses == null
                ? Collections.emptyList() : new ArrayList<>(statuses);
        return CompletableFuture.supplyAsync(() -> {
            String where = filter.isEmpty() ? ""
                    : " WHERE status IN (" + placeholders(filter.size()) + ")";
            try (Connection connection = dataSource.getConnection()) {
                long total;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM legacy_screenshare_sessions"
                                + where)) {
                    bindStatuses(statement, 1, filter);
                    try (ResultSet rows = statement.executeQuery()) {
                        total = rows.next() ? rows.getLong(1) : 0L;
                    }
                }
                List<ScreenshareSession> items = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + COLUMNS
                                + " FROM legacy_screenshare_sessions" + where
                                + " ORDER BY created_at DESC, id DESC "
                                + "LIMIT ? OFFSET ?")) {
                    int index = bindStatuses(statement, 1, filter);
                    statement.setInt(index, safeSize);
                    statement.setInt(index + 1, (safePage - 1) * safeSize);
                    try (ResultSet rows = statement.executeQuery()) {
                        while (rows.next()) {
                            items.add(map(rows));
                        }
                    }
                }
                return new ScreensharePage(items, safePage, safeSize, total);
            } catch (SQLException failure) {
                throw new ScreenshareRepositoryException(
                        "Elenco sessioni fallito", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> update(ScreenshareSession updated,
                                             ScreenshareStatus expectedStatus,
                                             long expectedRevision) {
        return CompletableFuture.supplyAsync(() -> {
            Connection connection = null;
            try {
                connection = dataSource.getConnection();
                connection.setAutoCommit(false);
                int changed;
                try (PreparedStatement statement =
                             connection.prepareStatement(UPDATE_SQL)) {
                    statement.setString(1, updated.getStatus().name());
                    statement.setLong(2, SqlValues.millis(
                            updated.getStartedAt().orElse(null)));
                    statement.setLong(3, SqlValues.millis(
                            updated.getEndedAt().orElse(null)));
                    statement.setString(4, updated.getOutcome()
                            .map(ScreenshareOutcome::name).orElse(null));
                    statement.setString(5, updated.getNotes().orElse(null));
                    statement.setLong(6,
                            SqlValues.millis(updated.getExpiresAt()));
                    statement.setLong(7, updated.getRevision());
                    statement.setString(8, updated.getId().storageValue());
                    statement.setString(9, expectedStatus.name());
                    statement.setLong(10, expectedRevision);
                    changed = statement.executeUpdate();
                }
                if (changed == 0) {
                    connection.rollback();
                    return Boolean.FALSE;
                }
                connection.commit();
                return Boolean.TRUE;
            } catch (SQLException failure) {
                rollbackQuietly(connection);
                throw new ScreenshareRepositoryException(
                        "Aggiornamento sessione fallito", failure);
            } finally {
                closeQuietly(connection);
            }
        }, executor);
    }

    private CompletableFuture<List<ScreenshareSession>> query(
            String whereClause, StatementBinder binder) {
        return CompletableFuture.supplyAsync(() -> {
            List<ScreenshareSession> sessions = new ArrayList<>();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT " + COLUMNS
                                 + " FROM legacy_screenshare_sessions "
                                 + whereClause)) {
                binder.bind(statement);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        sessions.add(map(rows));
                    }
                }
                return sessions;
            } catch (SQLException failure) {
                throw new ScreenshareRepositoryException(
                        "Ricerca sessioni fallita", failure);
            }
        }, executor);
    }

    private static ScreenshareSession map(ResultSet rows) throws SQLException {
        return ScreenshareSession.builder()
                .id(ScreenshareSessionId.parse(rows.getString("id"))
                        .orElseThrow(() -> new ScreenshareRepositoryException(
                                "Identificatore sessione illeggibile")))
                .target(SqlValues.uuid(rows.getString("target_uuid")),
                        rows.getString("target_name"))
                .staff(SqlValues.uuid(rows.getString("staff_uuid")),
                        rows.getString("staff_name"))
                .reportId(SqlValues.uuid(rows.getString("report_id")))
                .serverId(rows.getString("server_id"))
                .createdAt(SqlValues.instant(rows.getLong("created_at")))
                .startedAt(SqlValues.nullableInstant(
                        rows.getLong("started_at")))
                .expiresAt(SqlValues.instant(rows.getLong("expires_at")))
                .endedAt(SqlValues.nullableInstant(rows.getLong("ended_at")))
                .status(ScreenshareStatus.parse(rows.getString("status"))
                        .orElseThrow(() -> new ScreenshareRepositoryException(
                                "Stato sessione sconosciuto")))
                .outcome(ScreenshareOutcome.parse(rows.getString("outcome"))
                        .orElse(null))
                .notes(rows.getString("notes"))
                .proxyId(rows.getString("proxy_id"))
                .revision(rows.getLong("revision"))
                .build();
    }

    private static int bindStatuses(PreparedStatement statement, int from,
                                    Iterable<ScreenshareStatus> statuses)
            throws SQLException {
        int index = from;
        for (ScreenshareStatus status : statuses) {
            statement.setString(index++, status.name());
        }
        return index;
    }

    private static String placeholders(int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(index == 0 ? "?" : ", ?");
        }
        return builder.toString();
    }

    private static void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // L'errore originale e' gia' in viaggio.
            }
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // Connessione compromessa: la chiusura la scarta comunque.
            }
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Chiusura best effort.
            }
        }
    }

    /** Associazione dei parametri a una query preparata. */
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
