package it.legacynetwork.regions.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionFlagTest {

    @Test
    void fromStringCaseInsensitive() {
        assertEquals(RegionFlag.BUILD, RegionFlag.fromString("build"));
        assertEquals(RegionFlag.BUILD, RegionFlag.fromString("BUILD"));
        assertEquals(RegionFlag.BLOCK_BREAK, RegionFlag.fromString("block-break"));
        assertEquals(RegionFlag.BLOCK_BREAK, RegionFlag.fromString("block_break"));
        assertEquals(RegionFlag.PVP, RegionFlag.fromString("pvp"));
    }

    @Test
    void fromStringInvalidReturnsNull() {
        assertEquals(null, RegionFlag.fromString("nonexistent"));
        assertEquals(null, RegionFlag.fromString(""));
        assertEquals(null, RegionFlag.fromString(null));
    }

    @Test
    void blockBreakIsSpecific() {
        assertTrue(RegionFlag.BLOCK_BREAK.isSpecific());
        assertTrue(RegionFlag.BLOCK_PLACE.isSpecific());
        assertTrue(RegionFlag.FALL_DAMAGE.isSpecific());
    }

    @Test
    void buildIsNotSpecific() {
        assertFalse(RegionFlag.BUILD.isSpecific());
    }
}
