package it.legacynetwork.regions.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuboidRegionTest {

    @Test
    void normalizesCoordinates() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        flags.put(RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion r = new CuboidRegion("test", "world", "", 100, 0, 100, 0, 100, 0, 10, flags);
        assertEquals(0, r.getMinX());
        assertEquals(100, r.getMaxX());
        assertEquals(0, r.getMinY());
        assertEquals(100, r.getMaxY());
    }

    @Test
    void containsInside() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        CuboidRegion r = new CuboidRegion("test", "world", "", 0, 0, 0, 10, 10, 10, 10, flags);
        assertTrue(r.contains(5, 5, 5));
    }

    @Test
    void containsBoundary() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        CuboidRegion r = new CuboidRegion("test", "world", "", 0, 0, 0, 10, 10, 10, 10, flags);
        assertTrue(r.contains(0, 0, 0));
        assertTrue(r.contains(10, 10, 10));
    }

    @Test
    void doesNotContainOutside() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        CuboidRegion r = new CuboidRegion("test", "world", "", 0, 0, 0, 10, 10, 10, 10, flags);
        assertFalse(r.contains(-1, 5, 5));
        assertFalse(r.contains(11, 5, 5));
    }

    @Test
    void negativeCoordinates() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        CuboidRegion r = new CuboidRegion("test", "world", "", -100, 0, -100, -50, 100, -50, 10, flags);
        assertEquals(-100, r.getMinX());
        assertEquals(-50, r.getMaxX());
    }

    @Test
    void getFlagReturnsDefaultInherit() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        CuboidRegion r = new CuboidRegion("test", "world", "", 0, 0, 0, 10, 10, 10, 10, flags);
        assertEquals(FlagState.INHERIT, r.getFlag(RegionFlag.PVP));
    }

    @Test
    void getFlagReturnsSetValue() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        flags.put(RegionFlag.PVP, FlagState.DENY);
        CuboidRegion r = new CuboidRegion("test", "world", "", 0, 0, 0, 10, 10, 10, 10, flags);
        assertEquals(FlagState.DENY, r.getFlag(RegionFlag.PVP));
    }

    @Test
    void deterministicIdTiebreak() {
        Map<RegionFlag, FlagState> flags = new HashMap<>();
        CuboidRegion a = new CuboidRegion("alpha", "world", "", 0, 0, 0, 10, 10, 10, 10, flags);
        CuboidRegion b = new CuboidRegion("beta", "world", "", 0, 0, 0, 10, 10, 10, 10, flags);
        assertTrue(a.getId().compareTo(b.getId()) < 0);
    }
}
