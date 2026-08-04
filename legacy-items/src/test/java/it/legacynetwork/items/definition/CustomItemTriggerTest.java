package it.legacynetwork.items.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomItemTriggerTest {

    @Test
    void fromStringValid() {
        assertEquals(CustomItemTrigger.JOIN, CustomItemTrigger.fromString("JOIN"));
        assertEquals(CustomItemTrigger.RESPAWN, CustomItemTrigger.fromString("respawn"));
        assertEquals(CustomItemTrigger.WORLD_CHANGE,
                CustomItemTrigger.fromString("World_Change"));
    }

    @Test
    void fromStringInvalidReturnsNull() {
        assertNull(CustomItemTrigger.fromString("DEATH"));
        assertNull(CustomItemTrigger.fromString(null));
    }
}
