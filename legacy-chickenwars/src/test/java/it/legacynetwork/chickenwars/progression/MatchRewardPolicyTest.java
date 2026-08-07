package it.legacynetwork.chickenwars.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchRewardPolicyTest {
    @Test
    void onlyRecordedNaturalResourcesProduceResourceExperience() {
        MatchRewardPolicy policy = new MatchRewardPolicy(10, 20, 2, 5,
                1, 3, 1, 2, 4);

        MatchRewards natural = policy.calculate(false, 0, 0, 3);
        MatchRewards transferred = policy.calculate(false, 0, 0, 0);

        assertEquals(22, natural.getExperience());
        assertEquals(10, transferred.getExperience());
        assertEquals(transferred.getCoins(), natural.getCoins());
    }

    @Test
    void negativeBalancingValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new MatchRewardPolicy(0, 0, 0, 0, 0, 0, 0, 0, -1));
    }
}
