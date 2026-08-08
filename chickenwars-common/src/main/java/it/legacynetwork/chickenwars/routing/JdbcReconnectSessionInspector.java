package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.persistence.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

/**
 * Diagnosi di sola lettura sulle stesse tabelle usate dal routing.
 *
 * <p>Legge {@code cw_reconnect_sessions} e {@code cw_game_instances} senza
 * bloccare righe e senza scrivere: puo' quindi girare in parallelo al
 * coordinatore autorevole senza interferire.</p>
 */
public final class JdbcReconnectSessionInspector
        implements ReconnectSessionInspector {

    private final DataSource dataSource;
    private final Executor executor;
    private final long heartbeatTimeout;

    public JdbcReconnectSessionInspector(DataSource dataSource,
                                         Executor executor,
                                         long heartbeatTimeout) {
        if (dataSource == null || executor == null) {
            throw new IllegalArgumentException("Inspector incompleto");
        }
        this.dataSource = dataSource;
        this.executor = executor;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    @Override
    public CompletionStage<ReconnectDiagnosis> inspect(final UUID playerId,
                                                       final long now) {
        return CompletableFuture.supplyAsync(new java.util.function.Supplier
                <ReconnectDiagnosis>() {
            @Override
            public ReconnectDiagnosis get() {
                try {
                    return inspectNow(playerId, now);
                } catch (SQLException failure) {
                    throw new PersistenceException(
                            "Diagnosi reconnect fallita", failure);
                }
            }
        }, executor);
    }

    private ReconnectDiagnosis inspectNow(UUID playerId, long now)
            throws SQLException {
        String instanceId;
        boolean consumed;
        long expiresAt;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT instance_id,expires_at,consumed"
                             + " FROM cw_reconnect_sessions WHERE player_id=?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return ReconnectDiagnosis.NONE;
                }
                instanceId = row.getString(1);
                expiresAt = row.getLong(2);
                consumed = row.getBoolean(3);
            }
        }

        if (consumed) {
            return ReconnectDiagnosis.CONSUMED;
        }
        if (expiresAt <= now) {
            return ReconnectDiagnosis.EXPIRED;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT status,heartbeat_at FROM cw_game_instances"
                             + " WHERE instance_id=?")) {
            statement.setString(1, instanceId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return ReconnectDiagnosis.INSTANCE_MISSING;
                }
                InstanceStatus status = InstanceStatus.valueOf(row.getString(1));
                long heartbeatAt = row.getLong(2);
                if (status == InstanceStatus.ENDING) {
                    return ReconnectDiagnosis.MATCH_ENDED;
                }
                if (status == InstanceStatus.OFFLINE
                        || heartbeatAt < now - heartbeatTimeout) {
                    return ReconnectDiagnosis.INSTANCE_OFFLINE;
                }
                return ReconnectDiagnosis.READY;
            }
        }
    }
}
