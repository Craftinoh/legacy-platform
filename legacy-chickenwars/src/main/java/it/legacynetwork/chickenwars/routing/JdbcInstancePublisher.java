package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

/** Heartbeat asincrono con sostituzione atomica dello snapshot dell'istanza. */
public final class JdbcInstancePublisher implements InstancePublisher {
    private final DataSource dataSource;private final Executor executor;
    public JdbcInstancePublisher(DataSource ds,Executor ex){dataSource=ds;executor=ex;}
    @Override public CompletionStage<Void> heartbeat(final GameInstanceDescriptor d){return CompletableFuture.runAsync(()->{try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{try(PreparedStatement p=c.prepareStatement("DELETE FROM cw_game_instances WHERE instance_id=?")){p.setString(1,d.getInstanceId());p.executeUpdate();}try(PreparedStatement p=c.prepareStatement("INSERT INTO cw_game_instances(instance_id,server_name,mode,arena_id,status,players,capacity,heartbeat_at,accepting_joins) VALUES(?,?,?,?,?,?,?,?,?)")){p.setString(1,d.getInstanceId());p.setString(2,d.getServerName());p.setString(3,d.getMode().name());p.setString(4,d.getArenaId());p.setString(5,d.getStatus().name());p.setInt(6,d.getPlayers());p.setInt(7,d.getCapacity());p.setLong(8,d.getHeartbeatAt());p.setBoolean(9,d.isAcceptingJoins());p.executeUpdate();}c.commit();}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}catch(SQLException e){throw new PersistenceException("Heartbeat JDBC fallito",e);}},executor);}
    @Override public CompletionStage<Void> offline(final String id,final long now){return CompletableFuture.runAsync(()->{try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("UPDATE cw_game_instances SET status='OFFLINE',accepting_joins=FALSE,heartbeat_at=? WHERE instance_id=?")){p.setLong(1,now);p.setString(2,id);p.executeUpdate();}catch(SQLException e){throw new PersistenceException("Offline JDBC fallito",e);}},executor);}
}
