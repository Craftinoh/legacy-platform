package it.legacynetwork.chickenwars.persistence;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Contratto asincrono per preset Quick Buy su memoria, SQLite o PostgreSQL.
 */
public interface QuickBuyRepository {

    CompletionStage<List<QuickBuyPresetRecord>> loadPresets(UUID playerId);

    CompletionStage<Void> savePreset(QuickBuyPresetRecord preset);

    CompletionStage<Void> deletePreset(UUID playerId, String presetId);

    CompletionStage<Void> selectPreset(UUID playerId, String presetId);

    CompletionStage<Void> close();
}
