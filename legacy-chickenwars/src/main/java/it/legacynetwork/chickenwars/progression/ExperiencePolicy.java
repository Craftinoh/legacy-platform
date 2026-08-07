package it.legacynetwork.chickenwars.progression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Curva XP configurabile, monotona e limitata a un livello massimo. */
public final class ExperiencePolicy {

    private final List<Long> requirements;
    private final long maximumExperience;

    public ExperiencePolicy(List<Long> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            throw new IllegalArgumentException("La curva XP non puo' essere vuota");
        }
        List<Long> copy = new ArrayList<Long>();
        long total = 0L;
        long previous = 0L;
        for (Long value : requirements) {
            if (value == null || value.longValue() <= 0L
                    || value.longValue() < previous) {
                throw new IllegalArgumentException(
                        "La curva XP deve essere positiva e monotona");
            }
            previous = value.longValue();
            copy.add(value);
            total = safeAdd(total, value.longValue());
        }
        this.requirements = Collections.unmodifiableList(copy);
        this.maximumExperience = total;
    }

    public static ExperiencePolicy defaultPolicy() {
        List<Long> values = new ArrayList<Long>();
        for (int level = 0; level < 100; level++) {
            values.add(Long.valueOf(
                    ExperienceCurve.experienceRequiredForLevel(level)));
        }
        return new ExperiencePolicy(values);
    }

    public int levelFor(long totalExperience) {
        long remaining = Math.max(0L, totalExperience);
        int level = 0;
        while (level < requirements.size()
                && remaining >= requirements.get(level).longValue()) {
            remaining -= requirements.get(level).longValue();
            level++;
        }
        return level;
    }

    public long experienceIntoLevel(long totalExperience) {
        int level = levelFor(totalExperience);
        if (level >= requirements.size()) {
            return 0L;
        }
        long consumed = 0L;
        for (int index = 0; index < level; index++) {
            consumed = safeAdd(consumed, requirements.get(index).longValue());
        }
        return Math.max(0L, Math.min(totalExperience, maximumExperience) - consumed);
    }

    public long experienceRequiredFor(int level) {
        return level >= requirements.size() ? 0L
                : requirements.get(Math.max(0, level)).longValue();
    }

    public float progress(long totalExperience) {
        int level = levelFor(totalExperience);
        long required = experienceRequiredFor(level);
        return required == 0L ? 1.0F : (float) Math.min(1.0D,
                (double) experienceIntoLevel(totalExperience) / required);
    }

    public int getMaximumLevel() {
        return requirements.size();
    }

    public long getMaximumExperience() {
        return maximumExperience;
    }

    private static long safeAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
