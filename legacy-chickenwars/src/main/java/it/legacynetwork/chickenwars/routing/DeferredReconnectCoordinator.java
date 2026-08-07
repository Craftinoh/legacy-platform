package it.legacynetwork.chickenwars.routing;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class DeferredReconnectCoordinator implements ReconnectCoordinator {
    private final CompletionStage<ReconnectCoordinator> ready;
    public DeferredReconnectCoordinator(CompletionStage<ReconnectCoordinator> ready){this.ready=ready;}
    @Override public CompletionStage<Void> remember(final UUID p,final String i,final long e){return ready.thenCompose(r->r.remember(p,i,e));}
    @Override public CompletionStage<RouteResult> reconnect(final UUID p,final String k,final long n){return ready.thenCompose(r->r.reconnect(p,k,n));}
    @Override public CompletionStage<Void> forget(final UUID p){return ready.thenCompose(r->r.forget(p));}
    @Override public CompletionStage<Integer> cleanup(final long n){return ready.thenCompose(r->r.cleanup(n));}
}
