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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionIndexTest {

    @Test
    void buildsIndexFromRegions() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        flags.put(RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion r = new CuboidRegion("lobby", "world", "", 0, 0, 0, 15, 15, 15, 100, flags);

        List<CuboidRegion> regions = new ArrayList<>();
        regions.add(r);
        RegionIndex index = new RegionIndex();
        index.build(regions);

        List<CuboidRegion> candidates = index.getCandidates(8, 8);
        assertEquals(1, candidates.size());
        assertEquals("lobby", candidates.get(0).getId());
    }

    @Test
    void emptyIndexReturnsEmpty() {
        RegionIndex index = new RegionIndex();
        index.build(new ArrayList<CuboidRegion>());
        List<CuboidRegion> candidates = index.getCandidates(0, 0);
        assertTrue(candidates.isEmpty());
    }

    @Test
    void multipleRegionsInSameChunk() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        CuboidRegion r1 = new CuboidRegion("a", "world", "", 0, 0, 0, 10, 10, 10, 100, flags);
        CuboidRegion r2 = new CuboidRegion("b", "world", "", 5, 5, 5, 15, 15, 15, 50, flags);

        List<CuboidRegion> regions = new ArrayList<>();
        regions.add(r1);
        regions.add(r2);
        RegionIndex index = new RegionIndex();
        index.build(regions);

        List<CuboidRegion> candidates = index.getCandidates(7, 7);
        assertEquals(2, candidates.size());
    }
}
