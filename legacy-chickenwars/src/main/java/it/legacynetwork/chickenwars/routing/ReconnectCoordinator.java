package it.legacynetwork.chickenwars.routing;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ReconnectCoordinator {
    CompletionStage<Void> remember(UUID playerId,String instanceId,long expiresAt);
    CompletionStage<RouteResult> reconnect(UUID playerId,String idempotencyKey,long now);
    CompletionStage<Integer> cleanup(long now);
}
