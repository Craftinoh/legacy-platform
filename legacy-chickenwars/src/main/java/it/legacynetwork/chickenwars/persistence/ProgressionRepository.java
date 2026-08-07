package it.legacynetwork.chickenwars.persistence;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Contratto asincrono condiviso dalle future implementazioni SQLite e
 * PostgreSQL.
 */
public interface ProgressionRepository {

    CompletionStage<PlayerProgressRecord> load(UUID playerId);

    CompletionStage<Void> save(PlayerProgressRecord record);

    CompletionStage<Void> close();
}
