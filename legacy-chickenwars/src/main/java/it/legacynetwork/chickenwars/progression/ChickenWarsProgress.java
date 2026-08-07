package it.legacynetwork.chickenwars.progression;

import java.util.UUID;

/**
 * Profilo runtime della progressione globale ChickenWars.
 */
public final class ChickenWarsProgress {

    private final UUID playerId;
    private long totalExperience;
    private long coins;

    public ChickenWarsProgress(UUID playerId, long totalExperience, long coins) {
        if (playerId == null) {
            throw new IllegalArgumentException("UUID giocatore mancante");
        }
        this.playerId = playerId;
        this.totalExperience = Math.max(0L, totalExperience);
        this.coins = Math.max(0L, coins);
    }

    public synchronized ProgressionUpdate addExperience(long amount) {
        long previousExperience = totalExperience;
        int previousLevel = ExperienceCurve.levelForTotalExperience(
                previousExperience);
        if (amount > 0L) {
            totalExperience = safeAdd(totalExperience, amount);
        }
        int currentLevel = ExperienceCurve.levelForTotalExperience(
                totalExperience);
        return new ProgressionUpdate(previousExperience, totalExperience,
                previousLevel, currentLevel);
    }

    public synchronized void addCoins(long amount) {
        if (amount > 0L) {
            coins = safeAdd(coins, amount);
        }
    }

    public synchronized boolean spendCoins(long amount) {
        if (amount <= 0L || amount > coins) {
            return false;
        }
        coins -= amount;
        return true;
    }

    private long safeAdd(long current, long amount) {
        if (Long.MAX_VALUE - current < amount) {
            return Long.MAX_VALUE;
        }
        return current + amount;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public synchronized long getTotalExperience() {
        return totalExperience;
    }

    public synchronized int getLevel() {
        return ExperienceCurve.levelForTotalExperience(totalExperience);
    }

    public synchronized long getExperienceIntoLevel() {
        return ExperienceCurve.experienceIntoCurrentLevel(totalExperience);
    }

    public synchronized long getExperienceRequiredForNextLevel() {
        return ExperienceCurve.experienceRequiredForLevel(getLevel());
    }

    public synchronized float getProgressToNextLevel() {
        return ExperienceCurve.progressToNextLevel(totalExperience);
    }

    public synchronized long getCoins() {
        return coins;
    }
}
