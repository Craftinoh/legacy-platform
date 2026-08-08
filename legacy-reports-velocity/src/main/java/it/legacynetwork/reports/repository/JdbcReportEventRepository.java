package it.legacynetwork.reports.repository;

import it.legacynetwork.reports.model.ReportEvent;
import it.legacynetwork.reports.model.ReportEventType;
import it.legacynetwork.reports.model.ReportId;
import it.legacynetwork.reports.model.ReportStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Storico dei report su database relazionale.
 *
 * <p>Solo inserimento e lettura: la classe non espone alcuna istruzione di
 * {@code UPDATE} o {@code DELETE}, cosi' l'append-only e' una proprieta' del
 * codice e non una promessa scritta in un commento.</p>
 */
public final class JdbcReportEventRepository implements ReportEventRepository {

    private static final String COLUMNS =
            "id, report_id, actor_uuid, actor_name, event_type, "
                    + "previous_status, new_status, message, proxy_id, "
                    + "created_at";

    private static final String INSERT_SQL =
            "INSERT INTO legacy_report_events (" + COLUMNS + ") VALUES "
                    + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_SQL =
            "SELECT " + COLUMNS + " FROM legacy_report_events "
                    + "WHERE report_id = ? ORDER BY created_at DESC, id DESC "
                    + "LIMIT ?";

    private final DataSource dataSource;
    private final Executor executor;

    public JdbcReportEventRepository(DataSource dataSource, Executor executor) {
        if (dataSource == null || executor == null) {
            throw new IllegalArgumentException("Storico report incompleto");
        }
        this.dataSource = dataSource;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<ReportEvent> append(ReportEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(INSERT_SQL)) {
                statement.setString(1, SqlValues.uuid(event.getId()));
                statement.setString(2, event.getReportId().storageValue());
                statement.setString(3,
                        SqlValues.uuid(event.getActorId().orElse(null)));
                statement.setString(4, event.getActorName());
                statement.setString(5, event.getType().name());
                statement.setString(6, event.getPreviousStatus()
                        .map(ReportStatus::name).orElse(null));
                statement.setString(7, event.getNewStatus()
                        .map(ReportStatus::name).orElse(null));
                statement.setString(8, event.getMessage().orElse(null));
                statement.setString(9, event.getProxyId());
                statement.setLong(10, SqlValues.millis(event.getCreatedAt()));
                statement.executeUpdate();
                return event;
            } catch (SQLException failure) {
                throw new ReportRepositoryException(
                        "Inserimento evento report fallito", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<ReportEvent>> findByReport(ReportId reportId,
                                                             int limit) {
        int safeLimit = Math.max(1, limit);
        return CompletableFuture.supplyAsync(() -> {
            List<ReportEvent> events = new ArrayList<>();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(FIND_SQL)) {
                statement.setString(1, reportId.storageValue());
                statement.setInt(2, safeLimit);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        events.add(map(rows));
                    }
                }
                return events;
            } catch (SQLException failure) {
                throw new ReportRepositoryException(
                        "Lettura storico report fallita", failure);
            }
        }, executor);
    }

    private static ReportEvent map(ResultSet rows) throws SQLException {
        return new ReportEvent(
                SqlValues.uuid(rows.getString("id")),
                ReportId.parse(rows.getString("report_id")).orElseThrow(
                        () -> new ReportRepositoryException(
                                "Identificatore report illeggibile")),
                SqlValues.uuid(rows.getString("actor_uuid")),
                rows.getString("actor_name"),
                ReportEventType.parse(rows.getString("event_type")).orElseThrow(
                        () -> new ReportRepositoryException(
                                "Tipo evento sconosciuto")),
                ReportStatus.parse(rows.getString("previous_status"))
                        .orElse(null),
                ReportStatus.parse(rows.getString("new_status")).orElse(null),
                rows.getString("message"),
                rows.getString("proxy_id"),
                SqlValues.instant(rows.getLong("created_at")));
    }
}
