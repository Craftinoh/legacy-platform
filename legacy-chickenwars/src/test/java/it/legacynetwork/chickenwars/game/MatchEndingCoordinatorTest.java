package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.persistence.MatchFinalizationResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchEndingCoordinatorTest {

    @Test
    void endingWaitsForOneSuccessfulFinalization() {
        CompletableFuture<MatchFinalizationResult> future =
                new CompletableFuture<MatchFinalizationResult>();
        MatchEndingCoordinator coordinator = new MatchEndingCoordinator();

        assertTrue(coordinator.start(future));
        assertFalse(coordinator.start(future));
        assertFalse(coordinator.isSettled());

        future.complete(new MatchFinalizationResult(true));
        assertTrue(coordinator.isSettled());
        assertTrue(coordinator.isSuccessful());
    }

    @Test
    void failedDatabaseSettlementUnblocksCleanupWithoutClaimingRewards() {
        CompletableFuture<MatchFinalizationResult> future =
                new CompletableFuture<MatchFinalizationResult>();
        MatchEndingCoordinator coordinator = new MatchEndingCoordinator();
        coordinator.start(future);

        future.completeExceptionally(new IllegalStateException("db offline"));

        assertTrue(coordinator.isSettled());
        assertFalse(coordinator.isSuccessful());
        assertNotNull(coordinator.getFailure());
    }
}
