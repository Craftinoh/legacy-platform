package it.legacynetwork.chickenwars.routing;

import java.util.concurrent.CompletionStage;

public final class DeferredInstancePublisher implements InstancePublisher {
    private final CompletionStage<InstancePublisher> ready;
    public DeferredInstancePublisher(CompletionStage<InstancePublisher> ready){this.ready=ready;}
    @Override public CompletionStage<Void> heartbeat(final GameInstanceDescriptor d){return ready.thenCompose(p->p.heartbeat(d));}
    @Override public CompletionStage<Void> offline(final String id,final long now){return ready.thenCompose(p->p.offline(id,now));}
}
