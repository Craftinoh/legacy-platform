package it.legacynetwork.chickenwars.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementazione volatile dei preset Quick Buy per sviluppo e test.
 */
public final class InMemoryQuickBuyRepository implements QuickBuyRepository {

    private final ConcurrentMap<UUID, Map<String, QuickBuyPresetRecord>> records =
            new ConcurrentHashMap<UUID, Map<String, QuickBuyPresetRecord>>();

    @Override
    public CompletionStage<List<QuickBuyPresetRecord>> loadPresets(UUID playerId) {
        if (playerId == null) {
            return failedFuture(new IllegalArgumentException(
                    "UUID giocatore mancante"));
        }
        Map<String, QuickBuyPresetRecord> playerRecords = records.get(playerId);
        if (playerRecords == null) {
            return CompletableFuture.completedFuture(
                    Collections.<QuickBuyPresetRecord>emptyList());
        }
        synchronized (playerRecords) {
            return CompletableFuture.completedFuture(
                    new ArrayList<QuickBuyPresetRecord>(playerRecords.values()));
        }
    }

    @Override
    public CompletionStage<Void> savePreset(QuickBuyPresetRecord preset) {
        if (preset == null) {
            return failedFuture(new IllegalArgumentException(
                    "Preset mancante"));
        }
        Map<String, QuickBuyPresetRecord> playerRecords = records.get(
                preset.getPlayerId());
        if (playerRecords == null) {
            Map<String, QuickBuyPresetRecord> created =
                    Collections.synchronizedMap(
                            new LinkedHashMap<String, QuickBuyPresetRecord>());
            Map<String, QuickBuyPresetRecord> existing = records.putIfAbsent(
                    preset.getPlayerId(), created);
            playerRecords = existing == null ? created : existing;
        }
        playerRecords.put(preset.getPresetId(), preset);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> deletePreset(UUID playerId, String presetId) {
        Map<String, QuickBuyPresetRecord> playerRecords = records.get(playerId);
        if (playerRecords != null && presetId != null) {
            playerRecords.remove(presetId);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> selectPreset(UUID playerId, String presetId) {
        Map<String, QuickBuyPresetRecord> playerRecords = records.get(playerId);
        if (playerRecords == null || presetId == null
                || !playerRecords.containsKey(presetId)) {
            return failedFuture(new IllegalArgumentException(
                    "Preset non trovato: " + presetId));
        }
        synchronized (playerRecords) {
            Map<String, QuickBuyPresetRecord> replacements =
                    new LinkedHashMap<String, QuickBuyPresetRecord>();
            for (QuickBuyPresetRecord record : playerRecords.values()) {
                replacements.put(record.getPresetId(), new QuickBuyPresetRecord(
                        record.getPlayerId(), record.getPresetId(),
                        record.getDisplayName(),
                        record.getPresetId().equals(presetId),
                        record.getSlots()));
            }
            playerRecords.clear();
            playerRecords.putAll(replacements);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> close() {
        records.clear();
        return CompletableFuture.completedFuture(null);
    }

    private <T> CompletionStage<T> failedFuture(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<T>();
        future.completeExceptionally(throwable);
        return future;
    }
}
