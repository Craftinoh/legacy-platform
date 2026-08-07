package it.legacynetwork.chickenwars.routing;

import java.util.UUID;

/** Servizi condivisi di registry, prenotazione e reconnect distribuito. */
public final class RoutingServices {
    private final RoutingCoordinator coordinator;private final InstancePublisher publisher;
    private final ReconnectCoordinator reconnect;
    private final String localInstancePrefix;private final long reconnectTtl;
    public RoutingServices(RoutingCoordinator coordinator,InstancePublisher publisher,ReconnectCoordinator reconnect,String localInstancePrefix,long reconnectTtl){if(coordinator==null||publisher==null||reconnect==null)throw new IllegalArgumentException("Routing incompleto");this.coordinator=coordinator;this.publisher=publisher;this.reconnect=reconnect;this.localInstancePrefix=localInstancePrefix;this.reconnectTtl=reconnectTtl;}
    public RoutingCoordinator getCoordinator(){return coordinator;}
    public InstancePublisher getPublisher(){return publisher;}
    public ReconnectCoordinator getReconnect(){return reconnect;}
    public void remember(UUID playerId,String arenaId,long now){if(localInstancePrefix!=null&&!localInstancePrefix.trim().isEmpty())reconnect.remember(playerId,localInstancePrefix+":"+arenaId,now+reconnectTtl);}
}
