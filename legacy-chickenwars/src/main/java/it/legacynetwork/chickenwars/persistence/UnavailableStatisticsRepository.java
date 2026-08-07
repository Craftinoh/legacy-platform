package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.statistics.ModeStatistics;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class UnavailableStatisticsRepository implements StatisticsRepository {
    @Override public CompletionStage<List<ModeStatistics>> load(UUID id){return failed();}
    @Override public CompletionStage<Void> save(ModeStatistics r){return failed();}
    @Override public CompletionStage<Void> close(){return CompletableFuture.completedFuture(null);}
    private <T> CompletionStage<T> failed(){CompletableFuture<T> f=new CompletableFuture<T>();f.completeExceptionally(new IllegalStateException("Database disabilitato"));return f;}
}
