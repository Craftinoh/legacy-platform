package it.legacynetwork.regions.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionFlagTest {

    @Test
    void fromStringCaseInsensitive() {
        assertEquals(RegionFlag.BUILD, RegionFlag.fromString("build"));
        assertEquals(RegionFlag.BUILD, RegionFlag.fromString("BUILD"));
        assertEquals(RegionFlag.BLOCK_BREAK,
                RegionFlag.fromString("block-break"));
        assertEquals(RegionFlag.BLOCK_BREAK,
                RegionFlag.fromString("block_break"));
        assertEquals(RegionFlag.PVP, RegionFlag.fromString("pvp"));
    }

    @Test
    void fromStringInvalidReturnsNull() {
        assertNull(RegionFlag.fromString("nonexistent"));
        assertNull(RegionFlag.fromString(""));
        assertNull(RegionFlag.fromString(null));
    }

    @Test
    void specificFlagsHaveExpectedGeneralFlag() {
        assertTrue(RegionFlag.BLOCK_BREAK.isSpecific());
        assertTrue(RegionFlag.BLOCK_PLACE.isSpecific());
        assertTrue(RegionFlag.FALL_DAMAGE.isSpecific());
        assertTrue(RegionFlag.PVP.isSpecific());
        assertEquals(RegionFlag.BUILD,
                RegionFlag.BLOCK_BREAK.getGeneralFlag());
        assertEquals(RegionFlag.DAMAGE,
                RegionFlag.FALL_DAMAGE.getGeneralFlag());
        assertEquals(RegionFlag.DAMAGE,
                RegionFlag.PVP.getGeneralFlag());
    }

    @Test
    void buildIsNotSpecific() {
        assertFalse(RegionFlag.BUILD.isSpecific());
    }
}
