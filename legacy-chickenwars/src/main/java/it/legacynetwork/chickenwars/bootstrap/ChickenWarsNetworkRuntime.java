package it.legacynetwork.chickenwars.bootstrap;

import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.lobby.LobbyRoutingService;
import it.legacynetwork.chickenwars.lobby.LobbySelectorService;
import it.legacynetwork.chickenwars.lobby.ReturnLobbyService;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.persistence.DatabaseSettings;
import it.legacynetwork.chickenwars.persistence.PersistenceRuntime;
import it.legacynetwork.chickenwars.persistence.ProgressionServices;
import it.legacynetwork.chickenwars.persistence.QuickBuyRepository;
import it.legacynetwork.chickenwars.progression.ExperiencePolicy;
import it.legacynetwork.chickenwars.progression.MatchRewardPolicy;
import it.legacynetwork.chickenwars.routing.BukkitProxyTransferGateway;
import it.legacynetwork.chickenwars.routing.InstanceHeartbeatTask;
import it.legacynetwork.chickenwars.routing.RoutingServices;
import it.legacynetwork.chickenwars.routing.ReservationArrivalService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Possiede tutta l'infrastruttura Opus 3 e il relativo cleanup. */
public final class ChickenWarsNetworkRuntime {
    private final JavaPlugin plugin;private final FileConfiguration config;
    private final MessageService messages;
    private final PersistenceRuntime persistence;private final LobbyRoutingService lobby;
    private final ReturnLobbyService returnLobby;
    private final LobbySelectorService selector;private final Executor mainThread;
    private InstanceHeartbeatTask heartbeat;private ReservationArrivalService arrivals;
    public ChickenWarsNetworkRuntime(final JavaPlugin plugin,MessageService messages,
            FileConfiguration config){this.plugin=plugin;this.config=config;
        this.messages=messages;
        persistence=PersistenceRuntime.start(databaseSettings(),getClass().getClassLoader(),
                plugin.getLogger(),config.getLong("database.profile-timeout-millis",5000L),
                config.getInt("database.maximum-retries",2),
                config.getLong("routing.heartbeat-timeout-millis",15000L),
                config.getLong("routing.reservation-timeout-millis",10000L),
                config.getString("routing.instance-id",""),
                config.getLong("routing.reconnect-timeout-millis",120000L));
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin,"BungeeCord");
        BukkitProxyTransferGateway gateway=new BukkitProxyTransferGateway(plugin,
                new BukkitProxyTransferGateway.OnlinePlayerLookup(){@Override public Player find(UUID id){return Bukkit.getPlayer(id);}});
        mainThread=new Executor(){
            @Override public void execute(Runnable command){if(Bukkit.isPrimaryThread())command.run();else Bukkit.getScheduler().runTask(plugin,command);}};
        lobby=new LobbyRoutingService(persistence.routing().getCoordinator(),
                persistence.routing().getReconnect(),gateway,messages,mainThread);
        returnLobby=new ReturnLobbyService(gateway,
                config.getString("routing.lobby-server", ""));
        selector=new LobbySelectorService(lobby,messages,persistence.getProfiles());
    }
    private DatabaseSettings databaseSettings(){return new DatabaseSettings(
            config.getBoolean("database.enabled",false),config.getString("database.jdbc-url","jdbc:postgresql://localhost/chickenwars"),
            config.getString("database.username",""),config.getString("database.password",""),
            config.getInt("database.maximum-pool-size",4),config.getLong("database.connection-timeout-millis",5000L));}
    public ProgressionServices progression(){return persistence.services(rewards(),experience(),config.getInt("database.maximum-retries",2));}
    public RoutingServices routing(){return persistence.routing();}
    public QuickBuyRepository quickBuy(){return persistence.getQuickBuy();}
    public LobbyRoutingService lobby(){return lobby;}
    public LobbySelectorService selector(){return selector;}
    public ReturnLobbyService returnLobby(){return returnLobby;}
    public void startHeartbeat(ArenaManager arenas){heartbeat=new InstanceHeartbeatTask(arenas,routing().getPublisher(),config.getString("routing.instance-id",""),config.getString("routing.server-name",""));heartbeat.start(plugin);arrivals=new ReservationArrivalService(arenas,routing().getCoordinator(),persistence.getProfiles(),messages,mainThread);plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin,"BungeeCord",arrivals);plugin.getServer().getPluginManager().registerEvents(arrivals,plugin);}
    public void close(){if(heartbeat!=null){heartbeat.stop();heartbeat=null;}if(arrivals!=null){arrivals.clear();plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin,"BungeeCord",arrivals);org.bukkit.event.HandlerList.unregisterAll(arrivals);arrivals=null;}lobby.clear();persistence.close();plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin,"BungeeCord");}
    private MatchRewardPolicy rewards(){return new MatchRewardPolicy(
            config.getLong("rewards.experience.participation",25),config.getLong("rewards.experience.win",100),
            config.getLong("rewards.experience.kill",5),config.getLong("rewards.experience.final-kill",10),
            config.getLong("rewards.coins.participation",5),config.getLong("rewards.coins.win",20),
            config.getLong("rewards.coins.kill",1),config.getLong("rewards.coins.final-kill",2),
            config.getLong("rewards.experience.natural-resource",1));}
    private ExperiencePolicy experience(){int maximum=Math.max(1,config.getInt("progression.maximum-level",100));List<Long> values=config.getLongList("progression.experience-requirements");List<Long> result=new ArrayList<Long>();for(int level=0;level<maximum;level++){long value=level<values.size()?values.get(level).longValue():(values.isEmpty()?5000L:values.get(values.size()-1).longValue());result.add(Long.valueOf(value));}return new ExperiencePolicy(result);}
}
