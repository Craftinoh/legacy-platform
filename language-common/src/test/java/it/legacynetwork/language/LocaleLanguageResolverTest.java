package it.legacynetwork.language;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocaleLanguageResolverTest {
    private final LocaleLanguageResolver resolver = new LocaleLanguageResolver();

    @Test
    void resolvesItalianLocales() {
        assertEquals(Language.ITALIAN, resolver.resolve("it_IT"));
        assertEquals(Language.ITALIAN, resolver.resolve("it-CH"));
    }

    @Test
    void resolvesEnglishLocales() {
        assertEquals(Language.ENGLISH, resolver.resolve("en_US"));
        assertEquals(Language.ENGLISH, resolver.resolve("en-GB"));
    }

    @Test
    void fallsBackToEnglish() {
        assertEquals(Language.ENGLISH, resolver.resolve("de_DE"));
    }
}
