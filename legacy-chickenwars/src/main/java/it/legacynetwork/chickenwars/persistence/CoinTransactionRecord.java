package it.legacynetwork.chickenwars.persistence;

import java.util.UUID;

/** Movimento immutabile del ledger; l'importo firmato determina il tipo. */
public final class CoinTransactionRecord {
    private final UUID playerId;
    private final long amount;
    private final long balanceAfter;
    private final String idempotencyKey;
    private final long createdAtEpochMillis;

    public CoinTransactionRecord(UUID playerId, long amount, long balanceAfter,
                                 String idempotencyKey, long createdAtEpochMillis) {
        if (playerId == null || idempotencyKey == null
                || idempotencyKey.trim().isEmpty() || amount == 0L
                || balanceAfter < 0L) {
            throw new IllegalArgumentException("Movimento coins non valido");
        }
        this.playerId = playerId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.idempotencyKey = idempotencyKey;
        this.createdAtEpochMillis = createdAtEpochMillis;
    }

    public UUID getPlayerId() { return playerId; }
    public long getAmount() { return amount; }
    public long getBalanceAfter() { return balanceAfter; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public long getCreatedAtEpochMillis() { return createdAtEpochMillis; }
}
