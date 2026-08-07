package it.legacynetwork.chickenwars.progression;

/**
 * Risultato immutabile dell'assegnazione di XP.
 */
public final class ProgressionUpdate {

    private final long previousExperience;
    private final long currentExperience;
    private final int previousLevel;
    private final int currentLevel;

    public ProgressionUpdate(long previousExperience, long currentExperience,
                             int previousLevel, int currentLevel) {
        this.previousExperience = previousExperience;
        this.currentExperience = currentExperience;
        this.previousLevel = previousLevel;
        this.currentLevel = currentLevel;
    }

    public boolean hasLevelledUp() {
        return currentLevel > previousLevel;
    }

    public int getLevelsGained() {
        return Math.max(0, currentLevel - previousLevel);
    }

    public long getPreviousExperience() {
        return previousExperience;
    }

    public long getCurrentExperience() {
        return currentExperience;
    }

    public int getPreviousLevel() {
        return previousLevel;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public long getExperienceIntoLevel() {
        return ExperienceCurve.experienceIntoCurrentLevel(currentExperience);
    }

    public long getExperienceRequiredForNextLevel() {
        return ExperienceCurve.experienceRequiredForLevel(currentLevel);
    }

    public float getProgressToNextLevel() {
        return ExperienceCurve.progressToNextLevel(currentExperience);
    }
}
