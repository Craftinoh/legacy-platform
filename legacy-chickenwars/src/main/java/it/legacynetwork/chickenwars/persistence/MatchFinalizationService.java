package it.legacynetwork.chickenwars.persistence;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Retry limitato della singola finalizzazione idempotente. */
public final class MatchFinalizationService {
    private final MatchPersistence persistence;
    private final int maximumRetries;

    public MatchFinalizationService(MatchPersistence persistence,
                                    int maximumRetries) {
        if (persistence == null || maximumRetries < 0) {
            throw new IllegalArgumentException("Finalizzatore incompleto");
        }
        this.persistence = persistence;
        this.maximumRetries = maximumRetries;
    }

    public CompletionStage<MatchFinalizationResult> finalizeMatch(
            MatchFinalizationRequest request) {
        CompletableFuture<MatchFinalizationResult> result =
                new CompletableFuture<MatchFinalizationResult>();
        attempt(request, 0, result);
        return result;
    }

    private void attempt(final MatchFinalizationRequest request,
                         final int retry,
                         final CompletableFuture<MatchFinalizationResult> result) {
        persistence.finalizeMatch(request).whenComplete((value, failure) -> {
            if (failure == null) {
                result.complete(value);
            } else if (retry < maximumRetries) {
                attempt(request, retry + 1, result);
            } else {
                result.completeExceptionally(failure);
            }
        });
    }
}
