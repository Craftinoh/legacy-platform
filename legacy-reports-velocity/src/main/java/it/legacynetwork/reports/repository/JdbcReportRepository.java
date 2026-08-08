package it.legacynetwork.reports.repository;

import it.legacynetwork.reports.api.Report;
import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.api.ReportSnapshot;
import it.legacynetwork.reports.api.ReportStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Storage dei report su database relazionale.
 *
 * <p>Ogni query e' parametrizzata, ogni risorsa chiusa e nessuna gira sul thread
 * che ha chiamato: l'esecutore passato dal plugin e' l'unico posto dove si
 * blocca. L'aggiornamento e' condizionato allo stato e alla revisione attesi,
 * quindi due staffer che agiscono insieme non si sovrascrivono a vicenda.</p>
 */
public final class JdbcReportRepository implements ReportRepository {

    private static final String COLUMNS =
            "id, reporter_uuid, reporter_name, target_uuid, target_name, "
                    + "reason_id, details, server_id, target_ping, proxy_id, "
                    + "created_at, updated_at, status, assigned_staff_uuid, "
                    + "assigned_staff_name, resolution, punishment_id, "
                    + "screenshare_id, revision";

    private static final String INSERT_SQL =
            "INSERT INTO legacy_reports (" + COLUMNS + ") VALUES "
                    + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_SQL =
            "SELECT " + COLUMNS + " FROM legacy_reports WHERE id = ?";

    private static final String FIND_PREFIX_SQL =
            "SELECT " + COLUMNS + " FROM legacy_reports WHERE id LIKE ? "
                    + "ORDER BY created_at DESC LIMIT 2";

    private static final String LIST_TARGET_SQL =
            "SELECT " + COLUMNS + " FROM legacy_reports WHERE target_uuid = ? "
                    + "ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";

    private static final String COUNT_TARGET_SQL =
            "SELECT COUNT(*) FROM legacy_reports WHERE target_uuid = ?";

    private static final String DUPLICATE_SQL =
            "SELECT " + COLUMNS + " FROM legacy_reports WHERE reporter_uuid = ? "
                    + "AND target_uuid = ? AND created_at >= ? "
                    + "ORDER BY created_at DESC LIMIT 1";

    private static final String UPDATE_SQL =
            "UPDATE legacy_reports SET status = ?, assigned_staff_uuid = ?, "
                    + "assigned_staff_name = ?, resolution = ?, "
                    + "punishment_id = ?, screenshare_id = ?, updated_at = ?, "
                    + "revision = ? WHERE id = ? AND status = ? "
                    + "AND revision = ?";

    private final DataSource dataSource;
    private final Executor executor;

    public JdbcReportRepository(DataSource dataSource, Executor executor) {
        if (dataSource == null || executor == null) {
            throw new IllegalArgumentException("Repository report incompleto");
        }
        this.dataSource = dataSource;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Report> insert(Report report) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(INSERT_SQL)) {
                bindInsert(statement, report);
                statement.executeUpdate();
                return report;
            } catch (SQLException failure) {
                throw new ReportRepositoryException(
                        "Inserimento report fallito", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<Report>> find(ReportId id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(FIND_SQL)) {
                statement.setString(1, id.storageValue());
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows))
                            : Optional.<Report>empty();
                }
            } catch (SQLException failure) {
                throw new ReportRepositoryException(
                        "Lettura report fallita", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<Report>> findByReference(
            String reference) {
        Optional<String> normalized = ReportId.normalizeReference(reference);
        if (!normalized.isPresent()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        String prefix = normalized.get();
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(FIND_PREFIX_SQL)) {
                statement.setString(1, prefix + "%");
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) {
                        return Optional.<Report>empty();
                    }
                    Report first = map(rows);
                    // Prefisso ambiguo: nessuna scelta arbitraria.
                    return rows.next() ? Optional.<Report>empty()
                            : Optional.of(first);
                }
            } catch (SQLException failure) {
                throw new ReportRepositoryException(
                        "Ricerca report fallita", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<ReportPage> listByStatuses(
            Set<ReportStatus> statuses, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, pageSize);
        List<ReportStatus> filter = statuses == null
                ? Collections.emptyList() : new ArrayList<>(statuses);
        return CompletableFuture.supplyAsync(() -> {
            String where = filter.isEmpty() ? ""
                    : " WHERE status IN (" + placeholders(filter.size()) + ")";
            try (Connection connection = dataSource.getConnection()) {
                long total;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM legacy_reports" + where)) {
                    bindStatuses(statement, 1, filter);
                    try (ResultSet rows = statement.executeQuery()) {
                        total = rows.next() ? rows.getLong(1) : 0L;
                    }
                }
                List<Report> items = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + COLUMNS + " FROM legacy_reports" + where
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
                return new ReportPage(items, safePage, safeSize, total);
            } catch (SQLException failure) {
                throw new ReportRepositoryException(
                        "Elenco report fallito", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<ReportPage> listByTarget(UUID targetId, int page,
                                                      int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, pageSize);
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                long total;
                try (PreparedStatement statement =
                             connection.prepareStatement(COUNT_TARGET_SQL)) {
                    statement.setString(1, SqlValues.uuid(targetId));
                    try (ResultSet rows = statement.executeQuery()) {
                        total = rows.next() ? rows.getLong(1) : 0L;
                    }
                }
                List<Report> items = new ArrayList<>();
                try (PreparedStatement statement =
                             connection.prepareStatement(LIST_TARGET_SQL)) {
                    statement.setString(1, SqlValues.uuid(targetId));
                    statement.setInt(2, safeSize);
                    statement.setInt(3, (safePage - 1) * safeSize);
                    try (ResultSet rows = statement.executeQuery()) {
                        while (rows.next()) {
                            items.add(map(rows));
                        }
                    }
                }
                return new ReportPage(items, safePage, safeSize, total);
            } catch (SQLException failure) {
                throw new ReportRepositoryException(
                        "Storico giocatore fallito", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> countByReporter(
            UUID reporterId, Set<ReportStatus> statuses) {
        List<ReportStatus> filter = statuses == null
                ? Collections.emptyList() : new ArrayList<>(statuses);
        return CompletableFuture.supplyAsync(() -> {
            String where = filter.isEmpty() ? ""
                    : " AND status IN (" + placeholders(filter.size()) + ")";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT COUNT(*) FROM legacy_reports "
                                 + "WHERE reporter_uuid = ?" + where)) {
                statement.setString(1, SqlValues.uuid(reporterId));
                bindStatuses(statement, 2, filter);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? rows.getInt(1) : 0;
                }
            } catch (SQLException failure) {
                throw new ReportRepositoryException(
                        "Conteggio report fallito", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<Report>> findRecentDuplicate(
            UUID reporterId, UUID targetId, Instant notBefore) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(DUPLICATE_SQL)) {
                statement.setString(1, SqlValues.uuid(reporterId));
                statement.setString(2, SqlValues.uuid(targetId));
                statement.setLong(3, SqlValues.millis(notBefore));
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows))
                            : Optional.<Report>empty();
                }
            } catch (SQLException failure) {
                throw new ReportRepositoryException(
                        "Ricerca duplicati fallita", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> update(Report updated,
                                             ReportStatus expectedStatus,
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
                    statement.setString(2, SqlValues.uuid(
                            updated.getAssignedStaffId().orElse(null)));
                    statement.setString(3,
                            updated.getAssignedStaffName().orElse(null));
                    statement.setString(4,
                            updated.getResolution().orElse(null));
                    statement.setString(5,
                            updated.getPunishmentId().orElse(null));
                    statement.setString(6, SqlValues.uuid(
                            updated.getScreenshareId().orElse(null)));
                    statement.setLong(7,
                            SqlValues.millis(updated.getUpdatedAt()));
                    statement.setLong(8, updated.getRevision());
                    statement.setString(9, updated.getId().storageValue());
                    statement.setString(10, expectedStatus.name());
                    statement.setLong(11, expectedRevision);
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
                throw new ReportRepositoryException(
                        "Aggiornamento report fallito", failure);
            } finally {
                closeQuietly(connection);
            }
        }, executor);
    }

    private static void bindInsert(PreparedStatement statement, Report report)
            throws SQLException {
        ReportSnapshot snapshot = report.getSnapshot();
        statement.setString(1, report.getId().storageValue());
        statement.setString(2, SqlValues.uuid(report.getReporterId()));
        statement.setString(3, report.getReporterName());
        statement.setString(4, SqlValues.uuid(report.getTargetId()));
        statement.setString(5, report.getTargetName());
        statement.setString(6, report.getReasonId());
        statement.setString(7, report.getDetails().orElse(null));
        statement.setString(8, snapshot.getServerId());
        statement.setLong(9, snapshot.getTargetPingMillis());
        statement.setString(10, snapshot.getProxyId());
        statement.setLong(11, SqlValues.millis(report.getCreatedAt()));
        statement.setLong(12, SqlValues.millis(report.getUpdatedAt()));
        statement.setString(13, report.getStatus().name());
        statement.setString(14,
                SqlValues.uuid(report.getAssignedStaffId().orElse(null)));
        statement.setString(15, report.getAssignedStaffName().orElse(null));
        statement.setString(16, report.getResolution().orElse(null));
        statement.setString(17, report.getPunishmentId().orElse(null));
        statement.setString(18,
                SqlValues.uuid(report.getScreenshareId().orElse(null)));
        statement.setLong(19, report.getRevision());
    }

    private static Report map(ResultSet rows) throws SQLException {
        Instant createdAt = SqlValues.instant(rows.getLong("created_at"));
        ReportSnapshot snapshot = new ReportSnapshot(
                rows.getString("server_id"), rows.getLong("target_ping"),
                rows.getString("proxy_id"), createdAt);
        return Report.builder()
                .id(ReportId.parse(rows.getString("id")).orElseThrow(
                        () -> new ReportRepositoryException(
                                "Identificatore report illeggibile")))
                .reporter(SqlValues.uuid(rows.getString("reporter_uuid")),
                        rows.getString("reporter_name"))
                .target(SqlValues.uuid(rows.getString("target_uuid")),
                        rows.getString("target_name"))
                .reasonId(rows.getString("reason_id"))
                .details(rows.getString("details"))
                .snapshot(snapshot)
                .createdAt(createdAt)
                .updatedAt(SqlValues.instant(rows.getLong("updated_at")))
                .status(ReportStatus.parse(rows.getString("status"))
                        .orElseThrow(() -> new ReportRepositoryException(
                                "Stato report sconosciuto")))
                .assignedStaff(
                        SqlValues.uuid(rows.getString("assigned_staff_uuid")),
                        rows.getString("assigned_staff_name"))
                .resolution(rows.getString("resolution"))
                .punishmentId(rows.getString("punishment_id"))
                .screenshareId(SqlValues.uuid(rows.getString("screenshare_id")))
                .revision(rows.getLong("revision"))
                .build();
    }

    private static int bindStatuses(PreparedStatement statement, int from,
                                    List<ReportStatus> statuses)
            throws SQLException {
        int index = from;
        for (ReportStatus status : statuses) {
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
                // Nulla da fare: l'errore originale e' gia' in viaggio.
            }
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // Connessione gia' compromessa: la chiusura la scarta comunque.
            }
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Chiusura best effort.
            }
        }
    }
}
