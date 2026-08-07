package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class DeferredRoutingCoordinator implements RoutingCoordinator {
    private final CompletionStage<RoutingCoordinator> ready;
    public DeferredRoutingCoordinator(CompletionStage<RoutingCoordinator> ready){this.ready=ready;}
    @Override public CompletionStage<RouteResult> route(final UUID p,final MatchMode m,final List<UUID> party,final String k,final long n){return ready.thenCompose(r->r.route(p,m,party,k,n));}
    @Override public CompletionStage<Boolean> claim(final String id,final UUID p,final long n){return ready.thenCompose(r->r.claim(id,p,n));}
    @Override public CompletionStage<Boolean> cancel(final String id){return ready.thenCompose(r->r.cancel(id));}
    @Override public CompletionStage<Integer> cleanup(final long n){return ready.thenCompose(r->r.cleanup(n));}
}
