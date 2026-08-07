package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.mode.ModeProfile;
import it.legacynetwork.chickenwars.mode.ModeProfileRegistry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

/** Finalizzazione PostgreSQL atomica, sempre eseguita sull'executor DB. */
public final class JdbcMatchPersistence implements MatchPersistence {
    private final DataSource dataSource;
    private final Executor executor;
    public JdbcMatchPersistence(DataSource dataSource, Executor executor) {
        if (dataSource == null || executor == null) throw new IllegalArgumentException("JDBC incompleto");
        this.dataSource = dataSource; this.executor = executor;
    }
    @Override public CompletionStage<MatchFinalizationResult> finalizeMatch(
            final MatchFinalizationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try { return finalizeNow(request); }
            catch (SQLException failure) { throw new PersistenceException("Finalizzazione fallita", failure); }
        }, executor);
    }
    MatchFinalizationResult finalizeNow(MatchFinalizationRequest request)
            throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (exists(connection, request.getMatchId())) {
                    connection.rollback(); return new MatchFinalizationResult(false);
                }
                insertResult(connection, request);
                ModeProfile profile = ModeProfileRegistry.defaults().get(request.getMode());
                for (MatchParticipantRecord player : request.getParticipants()) {
                    if (profile.isRewardsEnabled()) {
                        long balance = upsertProgress(connection, player, request);
                        if (player.getCoins() > 0L) {
                            insertLedger(connection, player, request, balance);
                        }
                    }
                    if (profile.isTracked()) upsertStatistics(connection, player, request);
                    insertParticipant(connection, player, request);
                }
                connection.commit(); return new MatchFinalizationResult(true);
            } catch (SQLException failure) {
                connection.rollback(); throw failure;
            } finally { connection.setAutoCommit(true); }
        }
    }
    private boolean exists(Connection c, String id) throws SQLException {
        try (PreparedStatement q = c.prepareStatement("SELECT 1 FROM cw_match_results WHERE match_id=?")) {
            q.setString(1, id); try (ResultSet r = q.executeQuery()) { return r.next(); }
        }
    }
    private void insertResult(Connection c, MatchFinalizationRequest r) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO cw_match_results"
                + "(match_id,mode,winner_team_id,finished_at) VALUES(?,?,?,?)")) {
            s.setString(1,r.getMatchId()); s.setString(2,r.getMode().name());
            s.setString(3,r.getWinnerTeamId()); s.setLong(4,r.getFinishedAtEpochMillis()); s.executeUpdate();
        }
    }
    private long upsertProgress(Connection c, MatchParticipantRecord p,
                                MatchFinalizationRequest r) throws SQLException {
        int changed;
        try (PreparedStatement s = c.prepareStatement("UPDATE cw_player_progress SET "
                + "total_experience=total_experience+?,coins=coins+?,updated_at=? WHERE player_id=?")) {
            s.setLong(1,p.getExperience()); s.setLong(2,p.getCoins());
            s.setLong(3,r.getFinishedAtEpochMillis());
            s.setString(4,p.getPlayerId().toString()); changed=s.executeUpdate();
        }
        if (changed == 0) {
            try (PreparedStatement s = c.prepareStatement("INSERT INTO cw_player_progress"
                    + "(player_id,total_experience,coins,updated_at) VALUES(?,?,?,?)")) {
                s.setString(1,p.getPlayerId().toString()); s.setLong(2,p.getExperience());
                s.setLong(3,p.getCoins()); s.setLong(4,r.getFinishedAtEpochMillis()); s.executeUpdate();
            }
        }
        try (PreparedStatement query = c.prepareStatement(
                "SELECT coins FROM cw_player_progress WHERE player_id=?")) {
            query.setString(1, p.getPlayerId().toString());
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) throw new SQLException("Saldo non scritto");
                return result.getLong(1);
            }
        }
    }

    private void insertLedger(Connection c, MatchParticipantRecord p,
            MatchFinalizationRequest r, long balance) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO cw_coin_ledger"
                + "(transaction_id,player_id,amount,balance_after,idempotency_key,created_at) VALUES(?,?,?,?,?,?)")) {
            s.setString(1, java.util.UUID.randomUUID().toString());
            s.setString(2, p.getPlayerId().toString()); s.setLong(3, p.getCoins());
            s.setLong(4, balance); s.setString(5, "match:" + r.getMatchId()
                    + ":" + p.getPlayerId()); s.setLong(6, r.getFinishedAtEpochMillis());
            s.executeUpdate();
        }
    }
    private void upsertStatistics(Connection c, MatchParticipantRecord p,
                                  MatchFinalizationRequest r) throws SQLException {
        int changed;
        try (PreparedStatement s = c.prepareStatement("UPDATE cw_player_statistics SET "
                + "games=games+1,wins=wins+?,kills=kills+?,final_kills=final_kills+?,"
                + "deaths=deaths+?,eliminations=eliminations+?,resources_collected=resources_collected+?,"
                + "play_seconds=play_seconds+? WHERE player_id=? AND mode=?")) {
            s.setLong(1,p.isWinner()?1L:0L); s.setLong(2,p.getKills());
            s.setLong(3,p.getFinalKills()); s.setLong(4,p.getDeaths());
            s.setLong(5,p.getEliminations()); s.setLong(6,p.getResources());
            s.setLong(7,p.getPlaySeconds()); s.setString(8,p.getPlayerId().toString());
            s.setString(9,r.getMode().name()); changed=s.executeUpdate();
        }
        if (changed == 0) {
            try (PreparedStatement s = c.prepareStatement("INSERT INTO cw_player_statistics"
                    + "(player_id,mode,games,wins,kills,final_kills,deaths,eliminations,chicken_kills,resources_collected,play_seconds) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                s.setString(1,p.getPlayerId().toString()); s.setString(2,r.getMode().name());
                s.setLong(3,1L); s.setLong(4,p.isWinner()?1L:0L); s.setLong(5,p.getKills());
                s.setLong(6,p.getFinalKills()); s.setLong(7,p.getDeaths()); s.setLong(8,p.getEliminations());
                s.setLong(9,0L); s.setLong(10,p.getResources()); s.setLong(11,p.getPlaySeconds()); s.executeUpdate();
            }
        }
    }
    private void insertParticipant(Connection c, MatchParticipantRecord p,
                                   MatchFinalizationRequest r) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO cw_match_participants"
                + "(match_id,player_id,team_id,winner,experience,coins) VALUES(?,?,?,?,?,?)")) {
            s.setString(1,r.getMatchId()); s.setString(2,p.getPlayerId().toString());
            s.setString(3,p.getTeamId()); s.setBoolean(4,p.isWinner());
            s.setLong(5,p.getExperience()); s.setLong(6,p.getCoins()); s.executeUpdate();
        }
    }
    @Override public CompletionStage<Void> close() { return CompletableFuture.completedFuture(null); }
}
