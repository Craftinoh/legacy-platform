package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class InMemoryRoutingCoordinator implements RoutingCoordinator {
    private final GameRouter router;private final ReservationService reservations;
    public InMemoryRoutingCoordinator(InstanceRegistry instances,ReservationService reservations,long heartbeatTimeout,long reservationTtl){this.reservations=reservations;this.router=new GameRouter(instances,reservations,heartbeatTimeout,reservationTtl);}
    @Override public CompletionStage<RouteResult> route(UUID id,MatchMode mode,List<UUID> party,String key,long now){return CompletableFuture.completedFuture(router.route(id,mode,party,key,now));}
    @Override public CompletionStage<Boolean> claim(String id,UUID player,long now){return CompletableFuture.completedFuture(Boolean.valueOf(reservations.claim(id,player,now)));}
    @Override public CompletionStage<Boolean> cancel(String id){return CompletableFuture.completedFuture(Boolean.valueOf(reservations.cancel(id)));}
    @Override public CompletionStage<Integer> cleanup(long now){return CompletableFuture.completedFuture(Integer.valueOf(reservations.cleanup(now)));}
}
