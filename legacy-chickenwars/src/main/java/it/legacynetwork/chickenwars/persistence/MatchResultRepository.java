package it.legacynetwork.chickenwars.persistence;

import java.util.concurrent.CompletionStage;

/** Registro idempotente delle partite finalizzate. */
public interface MatchResultRepository {
    CompletionStage<Boolean> exists(String matchId);
    CompletionStage<Void> save(MatchResultRecord result);
    CompletionStage<Void> close();
}
