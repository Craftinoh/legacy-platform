package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

/** Sessione reconnect consumata e convertita in prenotazione nella stessa transazione. */
public final class JdbcReconnectCoordinator implements ReconnectCoordinator {
    private final DataSource dataSource;private final Executor executor;
    private final long heartbeatTimeout,reservationTtl;
    public JdbcReconnectCoordinator(DataSource ds,Executor ex,long heartbeatTimeout,long reservationTtl){dataSource=ds;executor=ex;this.heartbeatTimeout=heartbeatTimeout;this.reservationTtl=reservationTtl;}
    @Override public CompletionStage<Void> remember(final UUID player,final String instance,final long expires){return CompletableFuture.runAsync(()->{try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{try(PreparedStatement d=c.prepareStatement("DELETE FROM cw_reconnect_sessions WHERE player_id=?")){d.setString(1,player.toString());d.executeUpdate();}try(PreparedStatement p=c.prepareStatement("INSERT INTO cw_reconnect_sessions(player_id,instance_id,expires_at,consumed) VALUES(?,?,?,FALSE)")){p.setString(1,player.toString());p.setString(2,instance);p.setLong(3,expires);p.executeUpdate();}c.commit();}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}catch(SQLException e){throw new PersistenceException("Salvataggio reconnect fallito",e);}},executor);}
    @Override public CompletionStage<RouteResult> reconnect(final UUID player,final String key,final long now){return CompletableFuture.supplyAsync(()->{try{return reconnectNow(player,key,now);}catch(SQLException e){throw new PersistenceException("Reconnect JDBC fallito",e);}},executor);}
    private RouteResult reconnectNow(UUID player,String key,long now)throws SQLException{try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{String instanceId;try(PreparedStatement p=c.prepareStatement("SELECT instance_id FROM cw_reconnect_sessions WHERE player_id=? AND consumed=FALSE AND expires_at>? FOR UPDATE")){p.setString(1,player.toString());p.setLong(2,now);try(ResultSet r=p.executeQuery()){if(!r.next()){c.rollback();return RouteResult.failure(RouteFailure.STALE_INSTANCE);}instanceId=r.getString(1);}}
        GameInstanceDescriptor instance=null;try(PreparedStatement p=c.prepareStatement("SELECT instance_id,server_name,mode,arena_id,status,players,capacity,heartbeat_at,accepting_joins FROM cw_game_instances WHERE instance_id=? AND heartbeat_at>=? FOR UPDATE")){p.setString(1,instanceId);p.setLong(2,now-heartbeatTimeout);try(ResultSet r=p.executeQuery()){if(r.next()){InstanceStatus status=InstanceStatus.valueOf(r.getString(5));if(status!=InstanceStatus.ENDING&&status!=InstanceStatus.OFFLINE)instance=new GameInstanceDescriptor(r.getString(1),r.getString(2),MatchMode.valueOf(r.getString(3)),r.getString(4),status,r.getInt(6),r.getInt(7),r.getLong(8),r.getBoolean(9));}}}
        if(instance==null){c.rollback();return RouteResult.failure(RouteFailure.STALE_INSTANCE);}try(PreparedStatement p=c.prepareStatement("UPDATE cw_reconnect_sessions SET consumed=TRUE WHERE player_id=?")){p.setString(1,player.toString());p.executeUpdate();}
        GameReservation reservation=new GameReservation(UUID.randomUUID().toString(),player,instance.getMode(),instanceId,now+reservationTtl,key+":"+player);try(PreparedStatement p=c.prepareStatement("INSERT INTO cw_reservations(reservation_id,player_id,mode,instance_id,status,expires_at,idempotency_key) VALUES(?,?,?,?,?,?,?)")){p.setString(1,reservation.getReservationId());p.setString(2,player.toString());p.setString(3,instance.getMode().name());p.setString(4,instanceId);p.setString(5,ReservationStatus.CREATED.name());p.setLong(6,reservation.getExpiresAt());p.setString(7,reservation.getIdempotencyKey());p.executeUpdate();}c.commit();return RouteResult.success(instance,Collections.singletonList(reservation));
    }catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}}
    @Override public CompletionStage<Integer> cleanup(final long now){return CompletableFuture.supplyAsync(()->{try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM cw_reconnect_sessions WHERE consumed=TRUE OR expires_at<=?")){p.setLong(1,now);return Integer.valueOf(p.executeUpdate());}catch(SQLException e){throw new PersistenceException("Cleanup reconnect fallito",e);}},executor);}
}
