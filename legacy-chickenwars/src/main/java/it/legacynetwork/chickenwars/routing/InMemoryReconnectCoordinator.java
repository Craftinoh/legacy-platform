package it.legacynetwork.chickenwars.routing;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class InMemoryReconnectCoordinator implements ReconnectCoordinator {
    private final DistributedReconnectService sessions;private final InstanceRegistry instances;
    private final ReservationService reservations;private final long heartbeatTimeout,reservationTtl;
    public InMemoryReconnectCoordinator(DistributedReconnectService sessions,InstanceRegistry instances,ReservationService reservations,long heartbeatTimeout,long reservationTtl){this.sessions=sessions;this.instances=instances;this.reservations=reservations;this.heartbeatTimeout=heartbeatTimeout;this.reservationTtl=reservationTtl;}
    @Override public CompletionStage<Void> remember(UUID player,String instance,long expires){sessions.remember(new DistributedReconnectService.Session(player,instance,expires));return CompletableFuture.completedFuture(null);}
    @Override public CompletionStage<RouteResult> reconnect(UUID player,String key,long now){GameInstanceDescriptor instance=sessions.consume(player,now,instances,heartbeatTimeout);if(instance==null)return CompletableFuture.completedFuture(RouteResult.failure(RouteFailure.STALE_INSTANCE));GameReservation reservation=reservations.create(player,instance.getMode(),instance.getInstanceId(),now+reservationTtl,key+":"+player);return CompletableFuture.completedFuture(RouteResult.success(instance,Collections.singletonList(reservation)));}
    @Override public CompletionStage<Void> forget(UUID player){sessions.forget(player);return CompletableFuture.completedFuture(null);}
    @Override public CompletionStage<Integer> cleanup(long now){return CompletableFuture.completedFuture(Integer.valueOf(0));}
}
