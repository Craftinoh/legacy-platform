package it.legacynetwork.chickenwars.progression;

import java.util.UUID;

/**
 * Profilo runtime della progressione globale ChickenWars.
 */
public final class ChickenWarsProgress {

    private final UUID playerId;
    private final ExperiencePolicy experiencePolicy;
    private long totalExperience;
    private long coins;

    public ChickenWarsProgress(UUID playerId, long totalExperience, long coins) {
        this(playerId, totalExperience, coins, ExperiencePolicy.defaultPolicy());
    }

    public ChickenWarsProgress(UUID playerId, long totalExperience, long coins,
                               ExperiencePolicy experiencePolicy) {
        if (playerId == null) {
            throw new IllegalArgumentException("UUID giocatore mancante");
        }
        this.playerId = playerId;
        if (experiencePolicy == null) {
            throw new IllegalArgumentException("Curva esperienza mancante");
        }
        this.experiencePolicy = experiencePolicy;
        this.totalExperience = Math.max(0L, totalExperience);
        this.coins = Math.max(0L, coins);
    }

    public synchronized ProgressionUpdate addExperience(long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("Gli XP non possono essere negativi");
        }
        long previousExperience = totalExperience;
        int previousLevel = experiencePolicy.levelFor(previousExperience);
        if (amount > 0L) {
            totalExperience = safeAdd(totalExperience, amount);
        }
        totalExperience = Math.min(totalExperience,
                experiencePolicy.getMaximumExperience());
        int currentLevel = experiencePolicy.levelFor(totalExperience);
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
        return experiencePolicy.levelFor(totalExperience);
    }

    public synchronized long getExperienceIntoLevel() {
        return experiencePolicy.experienceIntoLevel(totalExperience);
    }

    public synchronized long getExperienceRequiredForNextLevel() {
        return experiencePolicy.experienceRequiredFor(getLevel());
    }

    public synchronized float getProgressToNextLevel() {
        return experiencePolicy.progress(totalExperience);
    }

    public synchronized long getCoins() {
        return coins;
    }

    public synchronized ProgressSnapshot snapshot() {
        return new ProgressSnapshot(playerId, totalExperience, getLevel(),
                getExperienceIntoLevel(), getExperienceRequiredForNextLevel(),
                coins);
    }
}
