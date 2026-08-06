package it.legacynetwork.language;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
    void resolvesNewLanguageCodes() {
        assertEquals(Language.GERMAN, Language.findByInput("de").get());
        assertEquals(Language.FRENCH, Language.findByInput("fr").get());
        assertEquals(Language.SPANISH, Language.findByInput("es").get());
        assertEquals(Language.DUTCH, Language.findByInput("nl").get());
        assertEquals(Language.POLISH, Language.findByInput("pl").get());
        assertEquals(Language.RUSSIAN, Language.findByInput("ru").get());
        assertEquals(Language.PORTUGUESE_BRAZIL, Language.findByInput("pt_br").get());
        assertEquals(Language.PORTUGUESE, Language.findByInput("pt").get());
    }

    @Test
    void resolvesRegionalVariants() {
        assertEquals(Language.GERMAN, Language.findByInput("de_at").get());
        assertEquals(Language.FRENCH, Language.findByInput("fr_ca").get());
        assertEquals(Language.ENGLISH, Language.findByInput("en_gb").get());
        assertEquals(Language.SPANISH, Language.findByInput("es_mx").get());
        assertEquals(Language.DUTCH, Language.findByInput("nl_be").get());
        assertEquals(Language.ITALIAN, Language.findByInput("it_ch").get());
    }

    @Test
    void getCodeReturnsCorrectCode() {
        assertEquals("it", Language.ITALIAN.getCode());
        assertEquals("en", Language.ENGLISH.getCode());
        assertEquals("de", Language.GERMAN.getCode());
        assertEquals("pt_br", Language.PORTUGUESE_BRAZIL.getCode());
        assertEquals("pt", Language.PORTUGUESE.getCode());
    }

    @Test
    void getDisplayNameReturnsNativeName() {
        assertEquals("Italiano", Language.ITALIAN.getDisplayName());
        assertEquals("English", Language.ENGLISH.getDisplayName());
        assertEquals("Deutsch", Language.GERMAN.getDisplayName());
        assertEquals("Русский", Language.RUSSIAN.getDisplayName());
    }

    @Test
    void getEnglishNameReturnsEnglishName() {
        assertEquals("Italian", Language.ITALIAN.getEnglishName());
        assertEquals("English", Language.ENGLISH.getEnglishName());
        assertEquals("German", Language.GERMAN.getEnglishName());
        assertEquals("Russian", Language.RUSSIAN.getEnglishName());
    }

    @Test
    void getMenuOrderIsSet() {
        assertEquals(1, Language.ITALIAN.getMenuOrder());
        assertEquals(2, Language.ENGLISH.getMenuOrder());
        assertEquals(5, Language.GERMAN.getMenuOrder());
    }

    @Test
    void getCountryCodeReturnsFlagCode() {
        assertEquals("it", Language.ITALIAN.getCountryCode());
        assertEquals("us", Language.ENGLISH.getCountryCode());
        assertEquals("de", Language.GERMAN.getCountryCode());
        assertEquals("ru", Language.RUSSIAN.getCountryCode());
        assertEquals("br", Language.PORTUGUESE_BRAZIL.getCountryCode());
        assertEquals("pt", Language.PORTUGUESE.getCountryCode());
    }

    @Test
    void findByInputNullReturnsEmpty() {
        assertEquals(Optional.empty(), Language.findByInput(null));
    }

    @Test
    void allThirtyOneLanguagesDefined() {
        assertEquals(31, Language.values().length);
    }

    @Test
    void manualPreferenceOverridesLocale() {
        assertTrue(LanguagePreference.MANUAL.overridesClientLocale());
    }
}
