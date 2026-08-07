package it.legacynetwork.chickenwars.persistence;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Backend esplicitamente offline: impedisce ricompense tracked fantasma. */
public final class UnavailableProgressionRepository implements ProgressionRepository {
    @Override public CompletionStage<PlayerProgressRecord> load(UUID id){return failed();}
    @Override public CompletionStage<Void> save(PlayerProgressRecord r){return failed();}
    @Override public CompletionStage<Void> close(){return CompletableFuture.completedFuture(null);}
    private <T> CompletionStage<T> failed(){CompletableFuture<T> f=new CompletableFuture<T>();f.completeExceptionally(new IllegalStateException("Database disabilitato"));return f;}
}
