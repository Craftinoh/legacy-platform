package it.legacynetwork.screenshare.repository;

import it.legacynetwork.screenshare.model.ScreenshareEvent;
import it.legacynetwork.screenshare.model.ScreenshareEventType;
import it.legacynetwork.screenshare.model.ScreenshareSessionId;
import it.legacynetwork.screenshare.model.ScreenshareStatus;

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
 * Storico delle sessioni su database relazionale.
 *
 * <p>Solo inserimento e lettura: nessuna istruzione di modifica o
 * cancellazione esiste in questa classe.</p>
 */
public final class JdbcScreenshareEventRepository
        implements ScreenshareEventRepository {

    private static final String COLUMNS =
            "id, session_id, actor_uuid, actor_name, event_type, "
                    + "previous_status, new_status, message, proxy_id, "
                    + "created_at";

    private static final String INSERT_SQL =
            "INSERT INTO legacy_screenshare_events (" + COLUMNS + ") VALUES "
                    + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_SQL =
            "SELECT " + COLUMNS + " FROM legacy_screenshare_events "
                    + "WHERE session_id = ? ORDER BY created_at DESC, id DESC "
                    + "LIMIT ?";

    private final DataSource dataSource;
    private final Executor executor;

    public JdbcScreenshareEventRepository(DataSource dataSource,
                                          Executor executor) {
        if (dataSource == null || executor == null) {
            throw new IllegalArgumentException("Storico sessioni incompleto");
        }
        this.dataSource = dataSource;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<ScreenshareEvent> append(ScreenshareEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(INSERT_SQL)) {
                statement.setString(1, SqlValues.uuid(event.getId()));
                statement.setString(2, event.getSessionId().storageValue());
                statement.setString(3,
                        SqlValues.uuid(event.getActorId().orElse(null)));
                statement.setString(4, event.getActorName());
                statement.setString(5, event.getType().name());
                statement.setString(6, event.getPreviousStatus()
                        .map(ScreenshareStatus::name).orElse(null));
                statement.setString(7, event.getNewStatus()
                        .map(ScreenshareStatus::name).orElse(null));
                statement.setString(8, event.getMessage().orElse(null));
                statement.setString(9, event.getProxyId());
                statement.setLong(10, SqlValues.millis(event.getCreatedAt()));
                statement.executeUpdate();
                return event;
            } catch (SQLException failure) {
                throw new ScreenshareRepositoryException(
                        "Inserimento evento sessione fallito", failure);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<ScreenshareEvent>> findBySession(
            ScreenshareSessionId sessionId, int limit) {
        int safeLimit = Math.max(1, limit);
        return CompletableFuture.supplyAsync(() -> {
            List<ScreenshareEvent> events = new ArrayList<>();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(FIND_SQL)) {
                statement.setString(1, sessionId.storageValue());
                statement.setInt(2, safeLimit);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        events.add(map(rows));
                    }
                }
                return events;
            } catch (SQLException failure) {
                throw new ScreenshareRepositoryException(
                        "Lettura storico sessione fallita", failure);
            }
        }, executor);
    }

    private static ScreenshareEvent map(ResultSet rows) throws SQLException {
        return new ScreenshareEvent(
                SqlValues.uuid(rows.getString("id")),
                ScreenshareSessionId.parse(rows.getString("session_id"))
                        .orElseThrow(() -> new ScreenshareRepositoryException(
                                "Identificatore sessione illeggibile")),
                SqlValues.uuid(rows.getString("actor_uuid")),
                rows.getString("actor_name"),
                ScreenshareEventType.parse(rows.getString("event_type"))
                        .orElseThrow(() -> new ScreenshareRepositoryException(
                                "Tipo evento sconosciuto")),
                ScreenshareStatus.parse(rows.getString("previous_status"))
                        .orElse(null),
                ScreenshareStatus.parse(rows.getString("new_status"))
                        .orElse(null),
                rows.getString("message"),
                rows.getString("proxy_id"),
                SqlValues.instant(rows.getLong("created_at")));
    }
}
