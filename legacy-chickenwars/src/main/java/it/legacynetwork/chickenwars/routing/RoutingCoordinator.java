package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface RoutingCoordinator {
    CompletionStage<RouteResult> route(UUID playerId, MatchMode mode,
            List<UUID> party, String idempotencyKey, long now);
    CompletionStage<Boolean> claim(String reservationId, UUID playerId, long now);
    CompletionStage<Boolean> cancel(String reservationId);
    CompletionStage<Integer> cleanup(long now);
}
