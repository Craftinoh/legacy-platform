package it.legacynetwork.items.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomItemActionTypeTest {

    @Test
    void fromStringValid() {
        assertEquals(CustomItemActionType.PLAYER_COMMAND,
                CustomItemActionType.fromString("PLAYER_COMMAND"));
        assertEquals(CustomItemActionType.CONSOLE_COMMAND,
                CustomItemActionType.fromString("console_command"));
        assertEquals(CustomItemActionType.MESSAGE,
                CustomItemActionType.fromString("message"));
        assertEquals(CustomItemActionType.CONNECT_SERVER,
                CustomItemActionType.fromString("CONNECT_SERVER"));
    }

    @Test
    void fromStringInvalidReturnsNull() {
        assertNull(CustomItemActionType.fromString("unknown"));
        assertNull(CustomItemActionType.fromString(null));
    }
}
