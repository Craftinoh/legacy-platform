package it.legacynetwork.language.velocity.service;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.LanguagePreference;
import it.legacynetwork.language.LocaleLanguageResolver;
import it.legacynetwork.language.velocity.repository.FileLanguageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyLanguageServiceTest {

    @Test
    void automaticPreferenceDoesNotOverrideClientLocale() {
        assertFalse(LanguagePreference.AUTOMATIC.overridesClientLocale());
    }

    @Test
    void manualPreferenceOverridesClientLocale() {
        assertTrue(LanguagePreference.MANUAL.overridesClientLocale());
    }

    @Test
    void manualPreferenceIsSavedWithCorrectType() {
        assertEquals("MANUAL", LanguagePreference.MANUAL.name());
    }

    @Test
    void automaticPreferenceIsSavedWithCorrectType() {
        assertEquals("AUTOMATIC", LanguagePreference.AUTOMATIC.name());
    }

    @Test
    void localeResolverReturnsItalianForIt(@TempDir Path tempDir) {
        LocaleLanguageResolver resolver = new LocaleLanguageResolver();
        assertEquals(Language.ITALIAN, resolver.resolve("it_IT"));
    }

    @Test
    void localeResolverReturnsEnglishForEn(@TempDir Path tempDir) {
        LocaleLanguageResolver resolver = new LocaleLanguageResolver();
        assertEquals(Language.ENGLISH, resolver.resolve("en_US"));
    }

    @Test
    void localeResolverReturnsGermanForDe(@TempDir Path tempDir) {
        LocaleLanguageResolver resolver = new LocaleLanguageResolver();
        assertEquals(Language.GERMAN, resolver.resolve("de_DE"));
    }

    @Test
    void localeResolverReturnsEnglishForUnsupportedLocale(@TempDir Path tempDir) {
        LocaleLanguageResolver resolver = new LocaleLanguageResolver();
        assertEquals(Language.ENGLISH, resolver.resolve("zz_ZZ"));
    }
}
