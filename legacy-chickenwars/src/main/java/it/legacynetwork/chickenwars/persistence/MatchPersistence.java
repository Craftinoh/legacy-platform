package it.legacynetwork.chickenwars.persistence;

import java.util.concurrent.CompletionStage;

/** Confine transazionale: risultato, XP, coins e statistiche insieme. */
public interface MatchPersistence {
    CompletionStage<MatchFinalizationResult> finalizeMatch(
            MatchFinalizationRequest request);
    CompletionStage<Void> close();
}
