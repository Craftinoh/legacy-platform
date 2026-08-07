package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.model.ArenaState;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Pubblica tutte le arene con un solo task e query asincrone. */
public final class InstanceHeartbeatTask implements Runnable {
    private final ArenaManager arenas;private final InstancePublisher publisher;
    private final String instanceId;private final String serverName;
    private final List<String> published=new ArrayList<String>();private BukkitTask task;
    public InstanceHeartbeatTask(ArenaManager arenas,InstancePublisher publisher,String instanceId,String serverName){this.arenas=arenas;this.publisher=publisher;this.instanceId=instanceId;this.serverName=serverName;}
    public void start(JavaPlugin plugin){if(task!=null||blank(instanceId)||blank(serverName))return;task=plugin.getServer().getScheduler().runTaskTimer(plugin,this,1L,100L);}
    @Override public void run(){long now=System.currentTimeMillis();for(Game game:arenas.getGames()){String id=instanceId+":"+game.getDefinition().getId();if(!published.contains(id))published.add(id);InstanceStatus status=map(game.getState());publisher.heartbeat(new GameInstanceDescriptor(id,serverName,game.getDefinition().getMode(),game.getDefinition().getId(),status,game.getPlayerCount(),game.getDefinition().getMaximumPlayers(),now,game.canJoin()));}}
    public void stop(){if(task!=null){task.cancel();task=null;}long now=System.currentTimeMillis();for(String id:published)publisher.offline(id,now);published.clear();}
    private InstanceStatus map(ArenaState state){switch(state){case WAITING:return InstanceStatus.WAITING;case STARTING:return InstanceStatus.STARTING;case IN_GAME:return InstanceStatus.INGAME;case ENDING:return InstanceStatus.ENDING;default:return InstanceStatus.OFFLINE;}}
    private boolean blank(String value){return value==null||value.trim().isEmpty();}
}
