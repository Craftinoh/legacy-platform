package it.legacynetwork.chickenwars.persistence;

import java.util.concurrent.CompletionStage;

public final class DeferredMatchPersistence implements MatchPersistence {
    private final CompletionStage<MatchPersistence> ready;
    public DeferredMatchPersistence(CompletionStage<MatchPersistence> ready){this.ready=ready;}
    @Override public CompletionStage<MatchFinalizationResult> finalizeMatch(final MatchFinalizationRequest v){return ready.thenCompose(r->r.finalizeMatch(v));}
    @Override public CompletionStage<Void> close(){return ready.thenCompose(MatchPersistence::close);}
}
