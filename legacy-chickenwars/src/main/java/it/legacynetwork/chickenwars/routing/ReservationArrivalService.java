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
    private final Executor mainThread;private final RejoinVerdictSink verdicts;
    private final Map<UUID,Pending> pending=
            new ConcurrentHashMap<UUID,Pending>();
    public ReservationArrivalService(ArenaManager arenas,RoutingCoordinator routing,
            ProfileLifecycleService profiles,MessageService messages,Executor mainThread,
            RejoinVerdictSink verdicts){this.arenas=arenas;this.routing=routing;this.profiles=profiles;this.messages=messages;this.mainThread=mainThread;this.verdicts=verdicts;}
    @Override public void onPluginMessageReceived(String channel,Player carrier,byte[] message){if(!"BungeeCord".equals(channel)||message==null)return;try{DataInputStream input=new DataInputStream(new ByteArrayInputStream(message));if(!SUBCHANNEL.equals(input.readUTF()))return;int length=input.readUnsignedShort();byte[] payload=new byte[length];input.readFully(payload);DataInputStream data=new DataInputStream(new ByteArrayInputStream(payload));UUID playerId=UUID.fromString(data.readUTF());Pending value=new Pending(data.readUTF(),data.readUTF());pending.put(playerId,value);Player target=org.bukkit.Bukkit.getPlayer(playerId);if(target!=null&&target.isOnline())attempt(target,value);}catch(IOException|IllegalArgumentException ignored){/* payload proxy estraneo o malformato */}}
    @EventHandler public void onJoin(PlayerJoinEvent event){Pending value=pending.get(event.getPlayer().getUniqueId());if(value!=null)attempt(event.getPlayer(),value);}
    private void attempt(final Player player,final Pending value){
        final UUID id=player.getUniqueId();
        Game game=arenas.getGame(value.arena);
        if(game==null){
            // Arena sconosciuta: la prenotazione non corrisponde a una partita.
            reject(player,value,RejoinVerdictCodec.REASON_WRONG_MATCH);
            return;
        }
        java.util.concurrent.CompletionStage<?> ready=game.getDefinition().getModeProfile().isTracked()?profiles.load(id):java.util.concurrent.CompletableFuture.completedFuture(null);
        ready.whenCompleteAsync((profile,error)->{
            if(error!=null||(game.getDefinition().getModeProfile().isTracked()&&!profiles.mayEnterTracked(id))){
                pending.remove(id,value);
                messages.send(player,"persistence.profile-unavailable");
                report(id,false,RejoinVerdictCodec.REASON_PROFILE_UNAVAILABLE,value.arena);
                return;
            }
            routing.claim(value.reservation,id,System.currentTimeMillis()).whenCompleteAsync((claimed,claimError)->{
                pending.remove(id,value);
                if(claimError!=null||!Boolean.TRUE.equals(claimed)){
                    // Assente, scaduta oppure gia' reclamata: il claim e' uno solo.
                    messages.send(player,"routing.transfer-failed");
                    report(id,false,RejoinVerdictCodec.REASON_NO_RESERVATION,value.arena);
                    return;
                }
                boolean entered=game.getState()==it.legacynetwork.chickenwars.model.ArenaState.IN_GAME?game.rejoin(player):game.join(player);
                if(!entered){
                    messages.send(player,"routing.transfer-failed");
                    report(id,false,RejoinVerdictCodec.REASON_RECONNECT_REFUSED,value.arena);
                    return;
                }
                report(id,true,"",value.arena);
            },mainThread);
        },mainThread);
    }
    private void reject(Player player,Pending value,String reason){
        pending.remove(player.getUniqueId(),value);
        messages.send(player,"routing.transfer-failed");
        report(player.getUniqueId(),false,reason,value.arena);
    }
    /** L'esito raggiunge il proxy solo se un sink e' configurato. */
    private void report(UUID playerId,boolean accepted,String reason,String arena){
        if(verdicts!=null)verdicts.report(playerId,accepted,reason,arena);
    }
    public void clear(){pending.clear();}
}
