package it.legacynetwork.regions.core;

import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionDecision;
import it.legacynetwork.regions.model.RegionFlag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionResolverTest {

    private RegionResolver resolver(List<CuboidRegion> regions) {
        RegionIndex index = new RegionIndex();
        index.build(regions);
        Map<RegionFlag, FlagState> defaults = new HashMap<>();
        for (RegionFlag flag : RegionFlag.values()) {
            defaults.put(flag, FlagState.ALLOW);
        }
        return new RegionResolver(index, defaults);
    }

    @Test
    void singleRegionDenyReturnsDenied() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        flags.put(RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion r = new CuboidRegion("lobby", "world", "", 0, 0, 0, 100, 100, 100, 100, flags);
        List<CuboidRegion> regions = new ArrayList<>();
        regions.add(r);

        RegionDecision decision = resolver(regions).resolve(50, 50, 50, RegionFlag.BUILD);
        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals("lobby", decision.getDecidingRegion());
    }

    @Test
    void noRegionsUsesDefaultAllow() {
        RegionDecision decision = resolver(new ArrayList<CuboidRegion>()).resolve(50, 50, 50, RegionFlag.BUILD);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
    }

    @Test
    void inheritFallsThrough() {
        Map<RegionFlag, FlagState> flags1 = new HashMap<>();
        flags1.put(RegionFlag.BUILD, FlagState.INHERIT);
        CuboidRegion r1 = new CuboidRegion("r1", "world", "", 0, 0, 0, 100, 100, 100, 100, flags1);

        Map<RegionFlag, FlagState> flags2 = new HashMap<>();
        flags2.put(RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion r2 = new CuboidRegion("r2", "world", "", 10, 10, 10, 50, 50, 50, 50, flags2);

        List<CuboidRegion> regions = new ArrayList<>();
        regions.add(r1);
        regions.add(r2);

        RegionDecision decision = resolver(regions).resolve(20, 20, 20, RegionFlag.BUILD);
        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals("r2", decision.getDecidingRegion());
    }

    @Test
    void higherPriorityWins() {
        Map<RegionFlag, FlagState> flags1 = new HashMap<>();
        flags1.put(RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion r1 = new CuboidRegion("low", "world", "", 0, 0, 0, 100, 100, 100, 10, flags1);

        Map<RegionFlag, FlagState> flags2 = new HashMap<>();
        flags2.put(RegionFlag.BUILD, FlagState.ALLOW);
        CuboidRegion r2 = new CuboidRegion("high", "world", "", 10, 10, 10, 50, 50, 50, 100, flags2);

        List<CuboidRegion> regions = new ArrayList<>();
        regions.add(r1);
        regions.add(r2);

        RegionDecision decision = resolver(regions).resolve(20, 20, 20, RegionFlag.BUILD);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
    }

    @Test
    void samePriorityUsesAlphabeticalId() {
        Map<RegionFlag, FlagState> flags1 = new HashMap<>();
        flags1.put(RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion r1 = new CuboidRegion("a-region", "world", "", 0, 0, 0, 100, 100, 100, 10, flags1);

        Map<RegionFlag, FlagState> flags2 = new HashMap<>();
        flags2.put(RegionFlag.BUILD, FlagState.ALLOW);
        CuboidRegion r2 = new CuboidRegion("b-region", "world", "", 0, 0, 0, 100, 100, 100, 10, flags2);

        List<CuboidRegion> regions = new ArrayList<>();
        regions.add(r1);
        regions.add(r2);

        RegionDecision decision = resolver(regions).resolve(20, 20, 20, RegionFlag.BUILD);
        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals("a-region", decision.getDecidingRegion());
    }

    @Test
    void outsideAllRegionsReturnsDefault() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        flags.put(RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion r = new CuboidRegion("lobby", "world", "", 0, 0, 0, 10, 10, 10, 100, flags);
        List<CuboidRegion> regions = new ArrayList<>();
        regions.add(r);

        RegionDecision decision = resolver(regions).resolve(100, 100, 100, RegionFlag.BUILD);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
    }
}
