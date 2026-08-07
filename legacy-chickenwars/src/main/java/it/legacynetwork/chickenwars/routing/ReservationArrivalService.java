package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.persistence.ProfileLifecycleService;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

/** Riceve il token proxy, lo reclama una volta e solo allora entra in arena. */
public final class ReservationArrivalService
        implements PluginMessageListener, Listener {
    private static final String SUBCHANNEL = "ChickenWarsReservation";
    private static final class Pending {private final String reservation,arena;Pending(String reservation,String arena){this.reservation=reservation;this.arena=arena;}}
    private final ArenaManager arenas;private final RoutingCoordinator routing;
    private final ProfileLifecycleService profiles;private final MessageService messages;
    private final Executor mainThread;private final Map<UUID,Pending> pending=
            new ConcurrentHashMap<UUID,Pending>();
    public ReservationArrivalService(ArenaManager arenas,RoutingCoordinator routing,
            ProfileLifecycleService profiles,MessageService messages,Executor mainThread){this.arenas=arenas;this.routing=routing;this.profiles=profiles;this.messages=messages;this.mainThread=mainThread;}
    @Override public void onPluginMessageReceived(String channel,Player carrier,byte[] message){if(!"BungeeCord".equals(channel)||message==null)return;try{DataInputStream input=new DataInputStream(new ByteArrayInputStream(message));if(!SUBCHANNEL.equals(input.readUTF()))return;int length=input.readUnsignedShort();byte[] payload=new byte[length];input.readFully(payload);DataInputStream data=new DataInputStream(new ByteArrayInputStream(payload));UUID playerId=UUID.fromString(data.readUTF());Pending value=new Pending(data.readUTF(),data.readUTF());pending.put(playerId,value);Player target=org.bukkit.Bukkit.getPlayer(playerId);if(target!=null&&target.isOnline())attempt(target,value);}catch(IOException|IllegalArgumentException ignored){/* payload proxy estraneo o malformato */}}
    @EventHandler public void onJoin(PlayerJoinEvent event){Pending value=pending.get(event.getPlayer().getUniqueId());if(value!=null)attempt(event.getPlayer(),value);}
    private void attempt(final Player player,final Pending value){final UUID id=player.getUniqueId();Game game=arenas.getGame(value.arena);if(game==null){pending.remove(id,value);messages.send(player,"routing.transfer-failed");return;}java.util.concurrent.CompletionStage<?> ready=game.getDefinition().getModeProfile().isTracked()?profiles.load(id):java.util.concurrent.CompletableFuture.completedFuture(null);ready.whenCompleteAsync((profile,error)->{if(error!=null||(game.getDefinition().getModeProfile().isTracked()&&!profiles.mayEnterTracked(id))){pending.remove(id,value);messages.send(player,"persistence.profile-unavailable");return;}routing.claim(value.reservation,id,System.currentTimeMillis()).whenCompleteAsync((claimed,claimError)->{pending.remove(id,value);if(claimError!=null||!Boolean.TRUE.equals(claimed)||(game.getState()==it.legacynetwork.chickenwars.model.ArenaState.IN_GAME?!game.rejoin(player):!game.join(player)))messages.send(player,"routing.transfer-failed");},mainThread);},mainThread);}
    public void clear(){pending.clear();}
}
