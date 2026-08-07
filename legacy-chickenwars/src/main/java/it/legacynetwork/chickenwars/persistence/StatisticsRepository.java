package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.statistics.ModeStatistics;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Persistence contract for Solo, Doubles and Trio statistics. */
public interface StatisticsRepository {

    CompletionStage<List<ModeStatistics>> load(UUID playerId);

    CompletionStage<Void> save(ModeStatistics statistics);

    CompletionStage<Void> close();
}
