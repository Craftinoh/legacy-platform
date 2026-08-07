package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.statistics.ModeStatistics;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryStatisticsRepository implements StatisticsRepository {
    private final Map<String,ModeStatistics> values=new ConcurrentHashMap<String,ModeStatistics>();
    @Override public CompletionStage<List<ModeStatistics>> load(UUID id){List<ModeStatistics> result=new ArrayList<ModeStatistics>();for(ModeStatistics value:values.values())if(value.getPlayerId().equals(id))result.add(value);return CompletableFuture.completedFuture(result);}
    @Override public CompletionStage<Void> save(ModeStatistics value){values.put(value.getPlayerId()+":"+value.getMode().name(),value);return CompletableFuture.completedFuture(null);}
    @Override public CompletionStage<Void> close(){values.clear();return CompletableFuture.completedFuture(null);}
}
