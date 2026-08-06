package it.legacynetwork.regions.core;

import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionDecision;
import it.legacynetwork.regions.model.RegionFlag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionResolverTest {

    @Test
    void singleRegionDenyReturnsDenied() {
        CuboidRegion region = region("lobby", "world", 100,
                RegionFlag.BUILD, FlagState.DENY);
        RegionDecision decision = resolver(region).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);

        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals("lobby", decision.getDecidingRegion());
    }

    @Test
    void noRegionsUsesDefaultAllow() {
        RegionDecision decision = resolver().resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
    }

    @Test
    void inheritFallsThroughToLowerPriorityRegion() {
        CuboidRegion inherit = region("high", "world", 100,
                RegionFlag.BUILD, FlagState.INHERIT);
        CuboidRegion deny = region("low", "world", 10,
                RegionFlag.BUILD, FlagState.DENY);

        RegionDecision decision = resolver(inherit, deny).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);
        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals("low", decision.getDecidingRegion());
    }

    @Test
    void higherPriorityWins() {
        CuboidRegion low = region("low", "world", 10,
                RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion high = region("high", "world", 100,
                RegionFlag.BUILD, FlagState.ALLOW);

        RegionDecision decision = resolver(low, high).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
        assertEquals("high", decision.getDecidingRegion());
    }

    @Test
    void samePriorityUsesAlphabeticalId() {
        CuboidRegion alpha = region("alpha", "world", 10,
                RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion beta = region("beta", "world", 10,
                RegionFlag.BUILD, FlagState.ALLOW);

        RegionDecision decision = resolver(beta, alpha).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);
        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals("alpha", decision.getDecidingRegion());
    }

    @Test
    void specificFlagFallsBackToGeneralFlag() {
        CuboidRegion region = region("lobby", "world", 100,
                RegionFlag.BUILD, FlagState.DENY);

        RegionDecision decision = resolver(region).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BLOCK_BREAK);
        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals("lobby", decision.getDecidingRegion());
        assertEquals(RegionFlag.BUILD, decision.getFlag());
    }

    @Test
    void explicitSpecificFlagOverridesGeneralFlag() {
        Map<RegionFlag, FlagState> flags = new HashMap<RegionFlag, FlagState>();
        flags.put(RegionFlag.BUILD, FlagState.DENY);
        flags.put(RegionFlag.BLOCK_BREAK, FlagState.ALLOW);
        CuboidRegion region = new CuboidRegion(
                "lobby", "world", "",
                0, 0, 0, 10, 10, 10, 100, flags);

        RegionDecision decision = resolver(region).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BLOCK_BREAK);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
        assertEquals("lobby", decision.getDecidingRegion());
        assertEquals(RegionFlag.BLOCK_BREAK, decision.getFlag());
    }

    @Test
    void sameCoordinatesInOtherWorldUseDefault() {
        CuboidRegion region = region("lobby", "world", 100,
                RegionFlag.BUILD, FlagState.DENY);
        RegionDecision decision = resolver(region).resolveEffective(
                "world_nether", "", 5, 5, 5, RegionFlag.BUILD);
        assertTrue(decision.isAllowed());
    }

    private RegionResolver resolver(CuboidRegion... regions) {
        List<CuboidRegion> list = new ArrayList<CuboidRegion>();
        for (CuboidRegion region : regions) {
            list.add(region);
        }
        RegionIndex index = new RegionIndex();
        index.build(list);

        EnumMap<RegionFlag, FlagState> defaults =
                new EnumMap<RegionFlag, FlagState>(RegionFlag.class);
        for (RegionFlag flag : RegionFlag.values()) {
            defaults.put(flag,
                    flag.isSpecific() ? FlagState.INHERIT : FlagState.ALLOW);
        }
        return new RegionResolver(index, defaults);
    }

    private CuboidRegion region(String id, String world, int priority,
                                RegionFlag flag, FlagState state) {
        Map<RegionFlag, FlagState> flags = new HashMap<RegionFlag, FlagState>();
        if (state != FlagState.INHERIT) {
            flags.put(flag, state);
        }
        return new CuboidRegion(
                id, world, "",
                0, 0, 0, 10, 10, 10,
                priority, flags);
    }
}
