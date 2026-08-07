package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.persistence.MatchFinalizationResult;

import java.util.concurrent.CompletionStage;

/** Barriera idempotente fra ENDING e il riuso dell'arena. */
public final class MatchEndingCoordinator {
    private boolean started;
    private volatile boolean settled;
    private volatile boolean successful;
    private volatile Throwable failure;

    public synchronized boolean start(
            CompletionStage<MatchFinalizationResult> finalization) {
        if (started || finalization == null) {
            return false;
        }
        started = true;
        finalization.whenComplete((result, error) -> {
            failure = error;
            successful = error == null;
            settled = true;
        });
        return true;
    }

    public boolean isStarted() { return started; }
    public boolean isSettled() { return settled; }
    public boolean isSuccessful() { return successful; }
    public Throwable getFailure() { return failure; }
}
