package it.legacynetwork.chickenwars.progression;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceCurveTest {

    @Test
    void introductoryLevelsUseReducedRequirements() {
        assertEquals(500L, ExperienceCurve.experienceRequiredForLevel(0));
        assertEquals(1000L, ExperienceCurve.experienceRequiredForLevel(1));
        assertEquals(2000L, ExperienceCurve.experienceRequiredForLevel(2));
        assertEquals(3500L, ExperienceCurve.experienceRequiredForLevel(3));
        assertEquals(5000L, ExperienceCurve.experienceRequiredForLevel(4));
    }

    @Test
    void curveRepeatsAtEachPrestigeBoundary() {
        assertEquals(487000L, ExperienceCurve.experienceAtStartOfLevel(100));
        assertEquals(500L, ExperienceCurve.experienceRequiredForLevel(100));
        assertEquals(100, ExperienceCurve.levelForTotalExperience(487000L));
        assertEquals(101, ExperienceCurve.levelForTotalExperience(487500L));
    }

    @Test
    void progressReportsLevelUpsAndBarProgress() {
        ChickenWarsProgress progress = new ChickenWarsProgress(
                UUID.randomUUID(), 0L, 0L);

        ProgressionUpdate first = progress.addExperience(250L);
        assertFalse(first.hasLevelledUp());
        assertEquals(0, first.getCurrentLevel());
        assertEquals(0.5F, first.getProgressToNextLevel(), 0.0001F);

        ProgressionUpdate second = progress.addExperience(250L);
        assertTrue(second.hasLevelledUp());
        assertEquals(1, second.getCurrentLevel());
        assertEquals(1, second.getLevelsGained());
        assertEquals(0.0F, second.getProgressToNextLevel(), 0.0001F);
    }
}
