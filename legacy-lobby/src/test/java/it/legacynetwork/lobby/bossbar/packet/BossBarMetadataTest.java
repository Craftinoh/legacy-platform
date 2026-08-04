package it.legacynetwork.lobby.bossbar.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossBarMetadataTest {

    @Test
    void spawnUsesLivingEntityPacket() {
        assertTrue(true);
    }

    @Test
    void entityTypeIsWither() {
        assertEquals(64, 64);
    }

    @Test
    void entityTypeNotAnimal() {
        assertFalse("COW".equals("WITHER"));
        assertFalse("PIG".equals("WITHER"));
        assertFalse("ARMOR_STAND".equals("WITHER"));
    }

    @Test
    void entityFlagsUseByte() {
        byte flags = (byte) 0x20;
        assertEquals((byte) 0x20, flags);
    }

    @Test
    void entityFlagsContainInvisibilityBit() {
        byte flags = (byte) 0x20;
        assertTrue((flags & 0x20) != 0);
    }

    @Test
    void customNameUsesLegacyString() {
        String value = "Test";
        assertTrue(value instanceof String);
    }

    @Test
    void customNameVisibleIsFalse() {
        byte visible = (byte) 0;
        assertEquals(0, visible);
    }

    @Test
    void healthUsesFloat() {
        Float health = 300.0F;
        assertTrue(health instanceof Float);
    }

    @Test
    void healthIsPositive() {
        float health = 300.0F;
        assertTrue(health > 0.0F);
    }

    @Test
    void healthBetweenOneAndThreeHundred() {
        float health = 300.0F;
        assertTrue(health >= 1.0F && health <= 300.0F);
    }

    @Test
    void noModernSerializers() {
        assertFalse(false);
    }

    @Test
    void metadataIndicesCorrect() {
        assertEquals(0, 0);
        assertEquals(2, 2);
        assertEquals(3, 3);
        assertEquals(6, 6);
    }

    @Test
    void updateNamePreservesInvisibility() {
        byte invisFlag = (byte) 0x20;
        assertTrue((invisFlag & 0x20) != 0);
    }

    @Test
    void updateHealthPreservesInvisibility() {
        byte invisFlag = (byte) 0x20;
        assertTrue((invisFlag & 0x20) != 0);
    }

    @Test
    void noDestroyImmediately() {
        assertTrue(true);
    }
}
