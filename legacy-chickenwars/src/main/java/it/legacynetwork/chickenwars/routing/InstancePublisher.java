package it.legacynetwork.chickenwars.routing;

import java.util.concurrent.CompletionStage;

public interface InstancePublisher {
    CompletionStage<Void> heartbeat(GameInstanceDescriptor descriptor);
    CompletionStage<Void> offline(String instanceId,long now);
}
