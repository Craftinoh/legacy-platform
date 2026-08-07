package it.legacynetwork.chickenwars.progression;

/**
 * Curva dei livelli globali ChickenWars.
 *
 * <p>Ogni ciclo di cento livelli riparte con quattro livelli introduttivi da
 * 500, 1000, 2000 e 3500 XP; i restanti richiedono 5000 XP.</p>
 */
public final class ExperienceCurve {

    private static final long CYCLE_EXPERIENCE = 487000L;

    private ExperienceCurve() {
    }

    public static int levelForTotalExperience(long totalExperience) {
        long remaining = Math.max(0L, totalExperience);
        long completedCycles = remaining / CYCLE_EXPERIENCE;
        long baseLevel = completedCycles * 100L;
        remaining %= CYCLE_EXPERIENCE;

        int levelInCycle = 0;
        while (levelInCycle < 100) {
            long required = experienceRequiredForLevel(levelInCycle);
            if (remaining < required) {
                break;
            }
            remaining -= required;
            levelInCycle++;
        }

        long result = baseLevel + levelInCycle;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    public static long experienceAtStartOfLevel(int level) {
        int safeLevel = Math.max(0, level);
        int cycles = safeLevel / 100;
        int remainder = safeLevel % 100;
        long total = cycles * CYCLE_EXPERIENCE;
        for (int current = 0; current < remainder; current++) {
            total += experienceRequiredForLevel(current);
        }
        return total;
    }

    public static long experienceRequiredForLevel(int currentLevel) {
        int position = Math.max(0, currentLevel) % 100;
        switch (position) {
            case 0:
                return 500L;
            case 1:
                return 1000L;
            case 2:
                return 2000L;
            case 3:
                return 3500L;
            default:
                return 5000L;
        }
    }

    public static long experienceIntoCurrentLevel(long totalExperience) {
        int level = levelForTotalExperience(totalExperience);
        return Math.max(0L, totalExperience - experienceAtStartOfLevel(level));
    }

    public static float progressToNextLevel(long totalExperience) {
        int level = levelForTotalExperience(totalExperience);
        long required = experienceRequiredForLevel(level);
        if (required <= 0L) {
            return 0.0F;
        }
        double progress = (double) experienceIntoCurrentLevel(totalExperience)
                / (double) required;
        return (float) Math.max(0.0D, Math.min(1.0D, progress));
    }
}
