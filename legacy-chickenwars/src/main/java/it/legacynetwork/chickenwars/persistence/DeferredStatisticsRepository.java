package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.statistics.ModeStatistics;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class DeferredStatisticsRepository implements StatisticsRepository {
    private final CompletionStage<StatisticsRepository> ready;
    public DeferredStatisticsRepository(CompletionStage<StatisticsRepository> ready){this.ready=ready;}
    @Override public CompletionStage<List<ModeStatistics>> load(final UUID id){return ready.thenCompose(r->r.load(id));}
    @Override public CompletionStage<Void> save(final ModeStatistics v){return ready.thenCompose(r->r.save(v));}
    @Override public CompletionStage<Void> close(){return ready.thenCompose(StatisticsRepository::close);}
}
