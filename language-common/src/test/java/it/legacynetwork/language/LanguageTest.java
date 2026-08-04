package it.legacynetwork.language;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageTest {
    @Test
    void resolvesCodesAndAliases() {
        assertEquals(Language.ITALIAN, Language.findByInput("it").get());
        assertEquals(Language.ENGLISH, Language.findByInput("en").get());
        assertEquals(Language.ITALIAN, Language.findByInput(" italiano ").get());
        assertEquals(Language.ENGLISH, Language.findByInput("inglese").get());
        assertEquals(Language.ITALIAN, Language.findByInput("ITALIAN").get());
        assertEquals(Language.ENGLISH, Language.findByInput("English").get());
    }

    @Test
    void manualPreferenceOverridesLocale() {
        assertTrue(LanguagePreference.MANUAL.overridesClientLocale());
    }
}
