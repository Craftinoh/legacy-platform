package it.legacynetwork.chickenwars.lobby;

import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.routing.GameReservation;
import it.legacynetwork.chickenwars.routing.RouteResult;
import it.legacynetwork.chickenwars.routing.RoutingCoordinator;
import it.legacynetwork.chickenwars.routing.ReconnectCoordinator;
import it.legacynetwork.chickenwars.routing.TransferGateway;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.bukkit.entity.Player;

/** Stato lobby autorevole: una sola richiesta e un solo trasferimento per player. */
public final class LobbyRoutingService {
    private static final class Entry {private volatile LobbyQueueState state;private volatile String reservation;Entry(){state=LobbyQueueState.SEARCHING;}}
    private final RoutingCoordinator routing;private final TransferGateway transfers;
    private final ReconnectCoordinator reconnect;
    private final MessageService messages;private final Executor mainThread;
    private final Map<UUID,Entry> queue=new ConcurrentHashMap<UUID,Entry>();
    public LobbyRoutingService(RoutingCoordinator routing,ReconnectCoordinator reconnect,TransferGateway transfers,
            MessageService messages,Executor mainThread){if(routing==null||reconnect==null||transfers==null||messages==null||mainThread==null)throw new IllegalArgumentException("Lobby routing incompleto");this.routing=routing;this.reconnect=reconnect;this.transfers=transfers;this.messages=messages;this.mainThread=mainThread;}
    public boolean join(final Player player,final MatchMode mode,final long now){if(player==null||mode==null)return false;final UUID id=player.getUniqueId();final Entry entry=new Entry();if(queue.putIfAbsent(id,entry)!=null)return false;messages.send(player,"lobby.queued","{mode}",mode.name());final String key="lobby:"+id+":"+now;
        reconnect.reconnect(id,key+":reconnect",now).whenCompleteAsync((previous,reconnectError)->{
            if(reconnectError==null&&previous!=null&&previous.isSuccessful()){messages.send(player,"routing.reconnect");complete(player,entry,previous,null);return;}
            routing.route(id,mode,null,key,now).whenCompleteAsync((result,error)->complete(player,entry,result,error),mainThread);
        },mainThread);return true;}
    private void complete(Player player,Entry entry,it.legacynetwork.chickenwars.routing.RouteResult result,Throwable error){UUID id=player.getUniqueId();if(entry.state==LobbyQueueState.CANCELLED)return;if(error!=null||result==null||!result.isSuccessful()){entry.state=LobbyQueueState.FAILED;queue.remove(id,entry);messages.send(player,"routing.none");return;}GameReservation reservation=result.getReservations().get(0);entry.reservation=reservation.getReservationId();entry.state=LobbyQueueState.RESERVED;messages.send(player,"routing.reserved");entry.state=LobbyQueueState.TRANSFERRING;transfers.transfer(id,result.getInstance().getServerName(),reservation.getReservationId(),result.getInstance().getArenaId());}
    public boolean leave(Player player){if(player==null)return false;Entry entry=queue.remove(player.getUniqueId());if(entry==null)return false;entry.state=LobbyQueueState.CANCELLED;if(entry.reservation!=null)routing.cancel(entry.reservation);messages.send(player,"lobby.left-queue");return true;}
    public LobbyQueueState state(UUID id){Entry entry=queue.get(id);return entry==null?null:entry.state;}
    public void clear(){for(Map.Entry<UUID,Entry> value:queue.entrySet()){Entry entry=value.getValue();entry.state=LobbyQueueState.CANCELLED;if(entry.reservation!=null)routing.cancel(entry.reservation);}queue.clear();}
}
