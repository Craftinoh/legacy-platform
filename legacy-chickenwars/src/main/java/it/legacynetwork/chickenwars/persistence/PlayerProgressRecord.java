package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.progression.ChickenWarsProgress;
import it.legacynetwork.chickenwars.progression.ExperiencePolicy;

import java.util.UUID;

/**
 * Rappresentazione persistibile di XP e Chicken Coins.
 */
public final class PlayerProgressRecord {

    private final UUID playerId;
    private final long totalExperience;
    private final long coins;
    private final long updatedAtEpochMillis;

    public PlayerProgressRecord(UUID playerId, long totalExperience, long coins,
                                long updatedAtEpochMillis) {
        if (playerId == null) {
            throw new IllegalArgumentException("UUID giocatore mancante");
        }
        this.playerId = playerId;
        this.totalExperience = Math.max(0L, totalExperience);
        this.coins = Math.max(0L, coins);
        this.updatedAtEpochMillis = Math.max(0L, updatedAtEpochMillis);
    }

    public static PlayerProgressRecord from(ChickenWarsProgress progress,
                                            long updatedAtEpochMillis) {
        if (progress == null) {
            throw new IllegalArgumentException("Progressione mancante");
        }
        return new PlayerProgressRecord(progress.getPlayerId(),
                progress.getTotalExperience(), progress.getCoins(),
                updatedAtEpochMillis);
    }

    public ChickenWarsProgress toProgress() {
        return new ChickenWarsProgress(playerId, totalExperience, coins);
    }

    public ChickenWarsProgress toProgress(ExperiencePolicy policy) {
        return new ChickenWarsProgress(playerId, totalExperience, coins, policy);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getTotalExperience() {
        return totalExperience;
    }

    public long getCoins() {
        return coins;
    }

    public long getUpdatedAtEpochMillis() {
        return updatedAtEpochMillis;
    }
}
