package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.statistics.ModeStatistics;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

public final class JdbcStatisticsRepository implements StatisticsRepository {
    private final DataSource dataSource; private final Executor executor;
    public JdbcStatisticsRepository(DataSource dataSource,Executor executor){this.dataSource=dataSource;this.executor=executor;}
    @Override public CompletionStage<List<ModeStatistics>> load(final UUID id){return CompletableFuture.supplyAsync(()->{List<ModeStatistics> result=new ArrayList<ModeStatistics>();try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("SELECT mode,games,wins,kills,deaths,final_kills,chicken_kills,eliminations,play_seconds,resources_collected FROM cw_player_statistics WHERE player_id=?")){p.setString(1,id.toString());try(ResultSet r=p.executeQuery()){while(r.next())result.add(new ModeStatistics(id,MatchMode.valueOf(r.getString(1)),r.getLong(2),r.getLong(3),r.getLong(4),r.getLong(5),r.getLong(6),r.getLong(7),r.getLong(8),r.getLong(9),r.getLong(10)));}}catch(SQLException e){throw fail(e);}return result;},executor);}
    @Override public CompletionStage<Void> save(final ModeStatistics v){return CompletableFuture.runAsync(()->{try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{try(PreparedStatement d=c.prepareStatement("DELETE FROM cw_player_statistics WHERE player_id=? AND mode=?")){d.setString(1,v.getPlayerId().toString());d.setString(2,v.getMode().name());d.executeUpdate();}try(PreparedStatement p=c.prepareStatement("INSERT INTO cw_player_statistics(player_id,mode,games,wins,kills,final_kills,deaths,eliminations,chicken_kills,resources_collected,play_seconds) VALUES(?,?,?,?,?,?,?,?,?,?,?)")){p.setString(1,v.getPlayerId().toString());p.setString(2,v.getMode().name());p.setLong(3,v.getGames());p.setLong(4,v.getWins());p.setLong(5,v.getKills());p.setLong(6,v.getFinalKills());p.setLong(7,v.getDeaths());p.setLong(8,v.getEliminations());p.setLong(9,v.getChickenKills());p.setLong(10,v.getResourcesCollected());p.setLong(11,v.getPlaySeconds());p.executeUpdate();}c.commit();}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}catch(SQLException e){throw fail(e);}},executor);}
    private PersistenceException fail(SQLException e){return new PersistenceException("Statistiche JDBC fallite",e);}
    @Override public CompletionStage<Void> close(){return CompletableFuture.completedFuture(null);}
}
