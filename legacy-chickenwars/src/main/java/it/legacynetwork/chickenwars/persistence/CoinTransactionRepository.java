package it.legacynetwork.chickenwars.persistence;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Ledger persistente delle Chicken Coins. */
public interface CoinTransactionRepository {
    CompletionStage<CoinTransactionRecord> credit(UUID playerId, long amount,
                                                   String idempotencyKey);
    CompletionStage<CoinTransactionRecord> debit(UUID playerId, long amount,
                                                  String idempotencyKey);
    CompletionStage<Long> balance(UUID playerId);
    CompletionStage<List<CoinTransactionRecord>> history(UUID playerId);
    CompletionStage<Void> close();
}
