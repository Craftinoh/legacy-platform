package it.legacynetwork.chickenwars.routing;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class InMemoryInstancePublisher implements InstancePublisher {
    private final InstanceRegistry registry;
    public InMemoryInstancePublisher(InstanceRegistry registry){this.registry=registry;}
    @Override public CompletionStage<Void> heartbeat(GameInstanceDescriptor d){registry.heartbeat(d);return CompletableFuture.completedFuture(null);}
    @Override public CompletionStage<Void> offline(String id,long now){registry.remove(id);return CompletableFuture.completedFuture(null);}
}
