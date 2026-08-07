package it.legacynetwork.chickenwars.progression;

import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.mode.ModeProfileRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchRewardAccumulatorTest {

    @Test
    void duelIgnoresPermanentRewards() {
        MatchRewardAccumulator accumulator = new MatchRewardAccumulator(
                ModeProfileRegistry.defaults().get(MatchMode.DUEL));

        accumulator.awardExperience(500L);
        accumulator.awardCoins(100L);

        assertTrue(accumulator.snapshot().isEmpty());
    }

    @Test
    void trackedModesAccumulateAndDrainRewards() {
        MatchRewardAccumulator accumulator = new MatchRewardAccumulator(
                ModeProfileRegistry.defaults().get(MatchMode.SOLO));

        accumulator.awardExperience(125L);
        accumulator.awardCoins(20L);

        MatchRewards rewards = accumulator.drain();
        assertEquals(125L, rewards.getExperience());
        assertEquals(20L, rewards.getCoins());
        assertTrue(accumulator.snapshot().isEmpty());
    }
}
