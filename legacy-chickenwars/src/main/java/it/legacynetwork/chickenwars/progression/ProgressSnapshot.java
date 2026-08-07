package it.legacynetwork.chickenwars.progression;

import java.util.UUID;

/** Snapshot immutabile della progressione mostrabile a lobby e scoreboard. */
public final class ProgressSnapshot {
    private final UUID playerId;
    private final long totalExperience;
    private final int level;
    private final long experienceIntoLevel;
    private final long experienceRequired;
    private final long coins;

    public ProgressSnapshot(UUID playerId, long totalExperience, int level,
                            long experienceIntoLevel, long experienceRequired,
                            long coins) {
        this.playerId = playerId;
        this.totalExperience = totalExperience;
        this.level = level;
        this.experienceIntoLevel = experienceIntoLevel;
        this.experienceRequired = experienceRequired;
        this.coins = coins;
    }

    public UUID getPlayerId() { return playerId; }
    public long getTotalExperience() { return totalExperience; }
    public int getLevel() { return level; }
    public long getExperienceIntoLevel() { return experienceIntoLevel; }
    public long getExperienceRequired() { return experienceRequired; }
    public long getCoins() { return coins; }
}
