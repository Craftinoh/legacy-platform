package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.mode.MatchMode;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchFinalizationServiceTest {
    private MatchFinalizationRequest request() {
        return new MatchFinalizationRequest("match", MatchMode.SOLO, null,
                Collections.<MatchParticipantRecord>emptyList(), 1L);
    }

    @Test
    void transientFailureRetriesTheSameIdempotentRequest() {
        AtomicInteger calls = new AtomicInteger();
        MatchPersistence persistence = new FakePersistence(calls, 1);
        MatchFinalizationResult result = new MatchFinalizationService(
                persistence, 2).finalizeMatch(request()).toCompletableFuture().join();
        assertTrue(result.isApplied());
        assertEquals(2, calls.get());
    }

    @Test
    void retryLimitPropagatesPersistentFailure() {
        AtomicInteger calls = new AtomicInteger();
        CompletionStage<MatchFinalizationResult> result =
                new MatchFinalizationService(new FakePersistence(calls, 4), 1)
                        .finalizeMatch(request());
        assertThrows(RuntimeException.class, () ->
                result.toCompletableFuture().join());
        assertEquals(2, calls.get());
    }

    private static final class FakePersistence implements MatchPersistence {
        private final AtomicInteger calls;
        private final int failures;
        private FakePersistence(AtomicInteger calls, int failures) {
            this.calls = calls;
            this.failures = failures;
        }
        @Override public CompletionStage<MatchFinalizationResult> finalizeMatch(
                MatchFinalizationRequest request) {
            if (calls.incrementAndGet() <= failures) {
                CompletableFuture<MatchFinalizationResult> failed =
                        new CompletableFuture<MatchFinalizationResult>();
                failed.completeExceptionally(new IllegalStateException("db"));
                return failed;
            }
            return CompletableFuture.completedFuture(
                    new MatchFinalizationResult(true));
        }
        @Override public CompletionStage<Void> close() {
            return CompletableFuture.completedFuture(null);
        }
    }
}
