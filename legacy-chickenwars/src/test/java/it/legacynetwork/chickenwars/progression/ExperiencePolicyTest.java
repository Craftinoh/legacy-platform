package it.legacynetwork.chickenwars.progression;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExperiencePolicyTest {
    @Test void supportsMultipleLevelsOverflowAndCap(){ExperiencePolicy curve=new ExperiencePolicy(Arrays.asList(10L,20L,30L));ChickenWarsProgress progress=new ChickenWarsProgress(UUID.randomUUID(),0,0,curve);ProgressionUpdate update=progress.addExperience(100);assertEquals(3,update.getCurrentLevel());assertEquals(60,progress.getTotalExperience());assertEquals(1.0F,progress.getProgressToNextLevel());}
    @Test void exposesSnapshotWithoutRoundingLoss(){ExperiencePolicy curve=new ExperiencePolicy(Arrays.asList(10L,20L));ChickenWarsProgress p=new ChickenWarsProgress(UUID.randomUUID(),0,2,curve);p.addExperience(19);ProgressSnapshot s=p.snapshot();assertEquals(1,s.getLevel());assertEquals(9,s.getExperienceIntoLevel());assertEquals(20,s.getExperienceRequired());}
    @Test void rejectsNegativeExperience(){ChickenWarsProgress p=new ChickenWarsProgress(UUID.randomUUID(),0,0);assertThrows(IllegalArgumentException.class,()->p.addExperience(-1));}
    @Test void rejectsNonMonotonicCurve(){assertThrows(IllegalArgumentException.class,()->new ExperiencePolicy(Arrays.asList(10L,9L)));}
}
