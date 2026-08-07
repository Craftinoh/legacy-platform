package it.legacynetwork.chickenwars.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Ledger thread-safe usato nei test e come fallback esplicito. */
public final class InMemoryCoinTransactionRepository
        implements CoinTransactionRepository {
    private final Object lock = new Object();
    private final Map<UUID, Long> balances = new HashMap<UUID, Long>();
    private final Map<UUID, List<CoinTransactionRecord>> history =
            new HashMap<UUID, List<CoinTransactionRecord>>();
    private final Map<String, CoinTransactionRecord> idempotency =
            new HashMap<String, CoinTransactionRecord>();

    @Override
    public CompletionStage<CoinTransactionRecord> credit(UUID playerId,
                                                          long amount,
                                                          String key) {
        return mutate(playerId, amount, key);
    }

    @Override
    public CompletionStage<CoinTransactionRecord> debit(UUID playerId,
                                                         long amount,
                                                         String key) {
        if (amount <= 0L) {
            return failed(new IllegalArgumentException("Addebito non positivo"));
        }
        return mutate(playerId, -amount, key);
    }

    private CompletionStage<CoinTransactionRecord> mutate(UUID playerId,
                                                            long signedAmount,
                                                            String key) {
        if (playerId == null || signedAmount == 0L || key == null
                || key.trim().isEmpty()) {
            return failed(new IllegalArgumentException("Transazione non valida"));
        }
        synchronized (lock) {
            CoinTransactionRecord existing = idempotency.get(key);
            if (existing != null) {
                if (!existing.getPlayerId().equals(playerId)
                        || existing.getAmount() != signedAmount) {
                    return failed(new IllegalStateException(
                            "Idempotency key riutilizzata con dati diversi"));
                }
                return CompletableFuture.completedFuture(existing);
            }
            long current = balances.containsKey(playerId)
                    ? balances.get(playerId).longValue() : 0L;
            if (signedAmount < 0L && current < -signedAmount) {
                return failed(new InsufficientCoinsException());
            }
            long next = signedAmount > 0L && Long.MAX_VALUE - current < signedAmount
                    ? Long.MAX_VALUE : current + signedAmount;
            CoinTransactionRecord record = new CoinTransactionRecord(playerId,
                    signedAmount, next, key, System.currentTimeMillis());
            balances.put(playerId, Long.valueOf(next));
            List<CoinTransactionRecord> entries = history.get(playerId);
            if (entries == null) {
                entries = new ArrayList<CoinTransactionRecord>();
                history.put(playerId, entries);
            }
            entries.add(record);
            idempotency.put(key, record);
            return CompletableFuture.completedFuture(record);
        }
    }

    @Override
    public CompletionStage<Long> balance(UUID playerId) {
        synchronized (lock) {
            Long value = balances.get(playerId);
            return CompletableFuture.completedFuture(
                    Long.valueOf(value == null ? 0L : value.longValue()));
        }
    }

    @Override
    public CompletionStage<List<CoinTransactionRecord>> history(UUID playerId) {
        synchronized (lock) {
            List<CoinTransactionRecord> entries = history.get(playerId);
            return CompletableFuture.completedFuture(entries == null
                    ? Collections.<CoinTransactionRecord>emptyList()
                    : new ArrayList<CoinTransactionRecord>(entries));
        }
    }

    @Override
    public CompletionStage<Void> close() {
        synchronized (lock) {
            balances.clear(); history.clear(); idempotency.clear();
        }
        return CompletableFuture.completedFuture(null);
    }

    private <T> CompletionStage<T> failed(Throwable failure) {
        CompletableFuture<T> result = new CompletableFuture<T>();
        result.completeExceptionally(failure);
        return result;
    }
}
