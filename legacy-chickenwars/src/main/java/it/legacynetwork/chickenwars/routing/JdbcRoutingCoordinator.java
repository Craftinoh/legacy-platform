package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

/** Prenotazione distribuita: scelta e occupazione posti nella stessa transazione. */
public final class JdbcRoutingCoordinator implements RoutingCoordinator {
    private final DataSource dataSource;private final Executor executor;
    private final long heartbeatTimeout;private final long reservationTtl;
    public JdbcRoutingCoordinator(DataSource ds,Executor ex,long heartbeatTimeout,long reservationTtl){dataSource=ds;executor=ex;this.heartbeatTimeout=heartbeatTimeout;this.reservationTtl=reservationTtl;}
    @Override public CompletionStage<RouteResult> route(final UUID player,final MatchMode mode,final List<UUID> party,final String key,final long now){return CompletableFuture.supplyAsync(()->{try{return routeNow(player,mode,party,key,now);}catch(SQLException e){throw new PersistenceException("Routing JDBC fallito",e);}},executor);}
    private RouteResult routeNow(UUID player,MatchMode mode,List<UUID> party,String key,long now)throws SQLException{
        List<UUID> members=party==null||party.isEmpty()?Collections.singletonList(player):new ArrayList<UUID>(party);if(!members.contains(player)){members=new ArrayList<UUID>(members);members.add(0,player);}
        try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{
            ExistingBatch prior=existing(c,key);if(!prior.reservations.isEmpty()){
                c.rollback();
                if(!prior.reusable||prior.reservations.size()!=members.size())return RouteResult.failure(RouteFailure.STALE_INSTANCE);
                GameInstanceDescriptor instance=find(c,prior.reservations.get(0).getInstanceId());
                return instance==null?RouteResult.failure(RouteFailure.STALE_INSTANCE):RouteResult.success(instance,prior.reservations);
            }
            GameInstanceDescriptor selected=null;String sql="SELECT instance_id,server_name,arena_id,status,players,capacity,heartbeat_at,accepting_joins FROM cw_game_instances WHERE mode=? AND status IN ('WAITING','STARTING') AND accepting_joins=TRUE AND heartbeat_at>=? ORDER BY players DESC,instance_id FOR UPDATE";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,mode.name());p.setLong(2,now-heartbeatTimeout);try(ResultSet r=p.executeQuery()){while(r.next()){GameInstanceDescriptor candidate=map(r,mode);int reserved=countReserved(c,candidate.getInstanceId(),now);if(candidate.getPlayers()+reserved+members.size()<=candidate.getCapacity()){selected=candidate;break;}}}}
            if(selected==null){c.rollback();return RouteResult.failure(RouteFailure.NO_INSTANCE);}List<GameReservation> created=new ArrayList<GameReservation>();
            try(PreparedStatement p=c.prepareStatement("INSERT INTO cw_reservations(reservation_id,player_id,mode,instance_id,status,expires_at,idempotency_key) VALUES(?,?,?,?,?,?,?)")){for(UUID member:members){GameReservation value=new GameReservation(UUID.randomUUID().toString(),member,mode,selected.getInstanceId(),now+reservationTtl,key+":"+member);created.add(value);p.setString(1,value.getReservationId());p.setString(2,member.toString());p.setString(3,mode.name());p.setString(4,selected.getInstanceId());p.setString(5,ReservationStatus.CREATED.name());p.setLong(6,value.getExpiresAt());p.setString(7,value.getIdempotencyKey());p.addBatch();}p.executeBatch();}c.commit();return RouteResult.success(selected,created);
        }catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}}
    private ExistingBatch existing(Connection c,String key)throws SQLException{ExistingBatch batch=new ExistingBatch();try(PreparedStatement p=c.prepareStatement("SELECT reservation_id,player_id,mode,instance_id,expires_at,idempotency_key,status FROM cw_reservations WHERE idempotency_key LIKE ? ORDER BY player_id")){p.setString(1,key+":%");try(ResultSet r=p.executeQuery()){while(r.next()){String status=r.getString(7);if(!status.equals(ReservationStatus.CREATED.name())&&!status.equals(ReservationStatus.CLAIMED.name()))batch.reusable=false;batch.reservations.add(new GameReservation(r.getString(1),UUID.fromString(r.getString(2)),MatchMode.valueOf(r.getString(3)),r.getString(4),r.getLong(5),r.getString(6)));}}}return batch;}
    private static final class ExistingBatch{private final List<GameReservation> reservations=new ArrayList<GameReservation>();private boolean reusable=true;}
    private int countReserved(Connection c,String instance,long now)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM cw_reservations WHERE instance_id=? AND status='CREATED' AND expires_at>?")){p.setString(1,instance);p.setLong(2,now);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}}}
    private GameInstanceDescriptor find(Connection c,String id)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT instance_id,server_name,arena_id,status,players,capacity,heartbeat_at,accepting_joins,mode FROM cw_game_instances WHERE instance_id=?")){p.setString(1,id);try(ResultSet r=p.executeQuery()){return r.next()?map(r,MatchMode.valueOf(r.getString(9))):null;}}}
    private GameInstanceDescriptor map(ResultSet r,MatchMode mode)throws SQLException{return new GameInstanceDescriptor(r.getString(1),r.getString(2),mode,r.getString(3),InstanceStatus.valueOf(r.getString(4)),r.getInt(5),r.getInt(6),r.getLong(7),r.getBoolean(8));}
    @Override public CompletionStage<Boolean> claim(final String id,final UUID player,final long now){return update("UPDATE cw_reservations SET status='CLAIMED' WHERE reservation_id=? AND player_id=? AND status='CREATED' AND expires_at>?",id,player,now);}
    private CompletionStage<Boolean> update(final String sql,final String id,final UUID player,final long now){return CompletableFuture.supplyAsync(()->{try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,id);p.setString(2,player.toString());p.setLong(3,now);return Boolean.valueOf(p.executeUpdate()==1);}catch(SQLException e){throw new PersistenceException("Prenotazione JDBC fallita",e);}},executor);}
    @Override public CompletionStage<Boolean> cancel(final String id){return CompletableFuture.supplyAsync(()->{try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("UPDATE cw_reservations SET status='CANCELLED' WHERE reservation_id=? AND status='CREATED'")){p.setString(1,id);return Boolean.valueOf(p.executeUpdate()==1);}catch(SQLException e){throw new PersistenceException("Cancellazione JDBC fallita",e);}},executor);}
    @Override public CompletionStage<Integer> cleanup(final long now){return CompletableFuture.supplyAsync(()->{try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("UPDATE cw_reservations SET status='EXPIRED' WHERE status='CREATED' AND expires_at<=?")){p.setLong(1,now);return Integer.valueOf(p.executeUpdate());}catch(SQLException e){throw new PersistenceException("Cleanup prenotazioni fallito",e);}},executor);}
}
