package it.legacynetwork.chickenwars.persistence;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class DeferredProgressionRepository implements ProgressionRepository {
    private final CompletionStage<ProgressionRepository> ready;
    public DeferredProgressionRepository(CompletionStage<ProgressionRepository> ready){this.ready=ready;}
    @Override public CompletionStage<PlayerProgressRecord> load(final UUID id){return ready.thenCompose(r->r.load(id));}
    @Override public CompletionStage<Void> save(final PlayerProgressRecord v){return ready.thenCompose(r->r.save(v));}
    @Override public CompletionStage<Void> close(){return ready.thenCompose(ProgressionRepository::close);}
}
