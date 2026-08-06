package it.legacynetwork.regions.core;

import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionFlag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionIndexTest {

    @Test
    void buildsIndexFromRegions() {
        Map<RegionFlag, FlagState> flags = new HashMap<RegionFlag, FlagState>();
        flags.put(RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion region = new CuboidRegion(
                "lobby", "world", "",
                0, 0, 0, 15, 15, 15, 100, flags);

        RegionIndex index = index(region);
        List<CuboidRegion> candidates =
                index.getCandidates("world", "", 8, 8);
        assertEquals(1, candidates.size());
        assertEquals("lobby", candidates.get(0).getId());
    }

    @Test
    void emptyIndexReturnsEmpty() {
        RegionIndex index = new RegionIndex();
        index.build(new ArrayList<CuboidRegion>());
        assertTrue(index.getCandidates("world", "", 0, 0).isEmpty());
    }

    @Test
    void multipleRegionsInSameChunkArePriorityOrdered() {
        Map<RegionFlag, FlagState> flags = new HashMap<RegionFlag, FlagState>();
        CuboidRegion low = new CuboidRegion(
                "low", "world", "",
                0, 0, 0, 10, 10, 10, 10, flags);
        CuboidRegion high = new CuboidRegion(
                "high", "world", "",
                5, 5, 5, 15, 15, 15, 100, flags);

        RegionIndex index = index(low, high);
        List<CuboidRegion> candidates =
                index.getCandidates("world", "", 7, 7);
        assertEquals(2, candidates.size());
        assertEquals("high", candidates.get(0).getId());
        assertEquals("low", candidates.get(1).getId());
    }

    @Test
    void sameCoordinatesInDifferentWorldsDoNotMix() {
        Map<RegionFlag, FlagState> flags = new HashMap<RegionFlag, FlagState>();
        CuboidRegion first = new CuboidRegion(
                "first", "world", "",
                0, 0, 0, 15, 15, 15, 10, flags);
        CuboidRegion second = new CuboidRegion(
                "second", "world_nether", "",
                0, 0, 0, 15, 15, 15, 10, flags);

        RegionIndex index = index(first, second);
        List<CuboidRegion> overworld =
                index.getCandidates("world", "", 8, 8);
        List<CuboidRegion> nether =
                index.getCandidates("world_nether", "", 8, 8);

        assertEquals(1, overworld.size());
        assertEquals("first", overworld.get(0).getId());
        assertEquals(1, nether.size());
        assertEquals("second", nether.get(0).getId());
    }

    @Test
    void negativeCoordinatesUseCorrectChunk() {
        CuboidRegion region = new CuboidRegion(
                "negative", "world", "",
                -32, 0, -32, -17, 20, -17, 0,
                new HashMap<RegionFlag, FlagState>());
        RegionIndex index = index(region);
        assertEquals(1,
                index.getCandidates("world", "", -20, -20).size());
        assertTrue(index.getCandidates("world", "", 20, 20).isEmpty());
    }

    private RegionIndex index(CuboidRegion... regions) {
        List<CuboidRegion> list = new ArrayList<CuboidRegion>();
        for (CuboidRegion region : regions) {
            list.add(region);
        }
        RegionIndex index = new RegionIndex();
        index.build(list);
        return index;
    }
}
