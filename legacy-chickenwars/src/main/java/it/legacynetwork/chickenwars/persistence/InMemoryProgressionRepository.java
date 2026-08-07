package it.legacynetwork.chickenwars.persistence;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Repository volatile per sviluppo e fallback controllato.
 */
public final class InMemoryProgressionRepository
        implements ProgressionRepository {

    private final ConcurrentMap<UUID, PlayerProgressRecord> records =
            new ConcurrentHashMap<UUID, PlayerProgressRecord>();

    @Override
    public CompletionStage<PlayerProgressRecord> load(UUID playerId) {
        if (playerId == null) {
            return failedFuture(new IllegalArgumentException(
                    "UUID giocatore mancante"));
        }
        PlayerProgressRecord record = records.get(playerId);
        if (record == null) {
            record = new PlayerProgressRecord(playerId, 0L, 0L,
                    System.currentTimeMillis());
        }
        return CompletableFuture.completedFuture(record);
    }

    @Override
    public CompletionStage<Void> save(PlayerProgressRecord record) {
        if (record == null) {
            return failedFuture(new IllegalArgumentException(
                    "Record progressione mancante"));
        }
        records.put(record.getPlayerId(), record);
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
