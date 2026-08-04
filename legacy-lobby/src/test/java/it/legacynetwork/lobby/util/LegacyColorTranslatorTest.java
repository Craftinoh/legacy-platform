package it.legacynetwork.lobby.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyColorTranslatorTest {
    @Test
    void translatesLegacyColors() {
        assertEquals("\u00A7aWelcome \u00A7fPlayer",
                LegacyColorTranslator.translate("&aWelcome &fPlayer"));
    }
}
