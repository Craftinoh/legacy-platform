package it.legacynetwork.chickenwars.mode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchModeTest {

    @Test
    void parsesAliasesAndInfersLegacyArenas() {
        assertEquals(MatchMode.DUEL, MatchMode.fromString("1v1"));
        assertEquals(MatchMode.DOUBLES, MatchMode.fromString("2v2"));
        assertEquals(MatchMode.TRIO, MatchMode.fromString("3s"));

        assertEquals(MatchMode.DUEL, MatchMode.infer(2, 1));
        assertEquals(MatchMode.SOLO, MatchMode.infer(8, 1));
        assertEquals(MatchMode.DOUBLES, MatchMode.infer(8, 2));
        assertEquals(MatchMode.TRIO, MatchMode.infer(4, 3));
    }

    @Test
    void duelAndSoloSharePricingButNotRewards() {
        ModeProfileRegistry registry = ModeProfileRegistry.defaults();
        ModeProfile duel = registry.get(MatchMode.DUEL);
        ModeProfile solo = registry.get(MatchMode.SOLO);

        assertEquals(solo.getPricingProfile(), duel.getPricingProfile());
        assertFalse(duel.isTracked());
        assertFalse(duel.isRewardsEnabled());
        assertTrue(solo.isTracked());
        assertTrue(solo.isRewardsEnabled());
    }

    @Test
    void defaultCapacityMatchesRequestedModes() {
        ModeProfileRegistry registry = ModeProfileRegistry.defaults();
        assertEquals(2, registry.get(MatchMode.DUEL).getMaximumPlayers());
        assertEquals(8, registry.get(MatchMode.SOLO).getMaximumPlayers());
        assertEquals(16, registry.get(MatchMode.DOUBLES).getMaximumPlayers());
        assertEquals(12, registry.get(MatchMode.TRIO).getMaximumPlayers());
    }
}
