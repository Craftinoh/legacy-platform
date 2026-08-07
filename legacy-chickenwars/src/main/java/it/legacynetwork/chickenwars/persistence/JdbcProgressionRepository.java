package it.legacynetwork.chickenwars.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

public final class JdbcProgressionRepository implements ProgressionRepository {
    private final DataSource dataSource; private final Executor executor;
    public JdbcProgressionRepository(DataSource dataSource,Executor executor){this.dataSource=dataSource;this.executor=executor;}
    @Override public CompletionStage<PlayerProgressRecord> load(final UUID id){return CompletableFuture.supplyAsync(()->{try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("SELECT total_experience,coins,updated_at FROM cw_player_progress WHERE player_id=?")){p.setString(1,id.toString());try(ResultSet r=p.executeQuery()){return r.next()?new PlayerProgressRecord(id,r.getLong(1),r.getLong(2),r.getLong(3)):new PlayerProgressRecord(id,0,0,0);}}catch(SQLException e){throw fail(e);}},executor);}
    @Override public CompletionStage<Void> save(final PlayerProgressRecord value){return CompletableFuture.runAsync(()->{try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{try(PreparedStatement d=c.prepareStatement("DELETE FROM cw_player_progress WHERE player_id=?")){d.setString(1,value.getPlayerId().toString());d.executeUpdate();}try(PreparedStatement p=c.prepareStatement("INSERT INTO cw_player_progress(player_id,total_experience,coins,updated_at) VALUES(?,?,?,?)")){p.setString(1,value.getPlayerId().toString());p.setLong(2,value.getTotalExperience());p.setLong(3,value.getCoins());p.setLong(4,value.getUpdatedAtEpochMillis());p.executeUpdate();}c.commit();}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}catch(SQLException e){throw fail(e);}},executor);}
    private PersistenceException fail(SQLException e){return new PersistenceException("Progressione JDBC fallita",e);}
    @Override public CompletionStage<Void> close(){return CompletableFuture.completedFuture(null);}
}
