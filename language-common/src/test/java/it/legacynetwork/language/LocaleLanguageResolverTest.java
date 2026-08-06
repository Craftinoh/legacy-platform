package it.legacynetwork.language;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
    void resolvesGermanLocales() {
        assertEquals(Language.GERMAN, resolver.resolve("de_DE"));
        assertEquals(Language.GERMAN, resolver.resolve("de_AT"));
        assertEquals(Language.GERMAN, resolver.resolve("de-CH"));
    }

    @Test
    void resolvesFrenchLocales() {
        assertEquals(Language.FRENCH, resolver.resolve("fr_FR"));
        assertEquals(Language.FRENCH, resolver.resolve("fr_CA"));
        assertEquals(Language.FRENCH, resolver.resolve("fr-CH"));
    }

    @Test
    void resolvesSpanishLocales() {
        assertEquals(Language.SPANISH, resolver.resolve("es_ES"));
        assertEquals(Language.SPANISH, resolver.resolve("es_MX"));
        assertEquals(Language.SPANISH, resolver.resolve("es-AR"));
    }

    @Test
    void resolvesDutchLocales() {
        assertEquals(Language.DUTCH, resolver.resolve("nl_NL"));
        assertEquals(Language.DUTCH, resolver.resolve("nl_BE"));
    }

    @Test
    void resolvesPortugueseLocales() {
        assertEquals(Language.PORTUGUESE, resolver.resolve("pt_PT"));
        assertEquals(Language.PORTUGUESE_BRAZIL, resolver.resolve("pt_BR"));
    }

    @Test
    void resolvesPolish() {
        assertEquals(Language.POLISH, resolver.resolve("pl_PL"));
    }

    @Test
    void resolvesRussian() {
        assertEquals(Language.RUSSIAN, resolver.resolve("ru_RU"));
    }

    @Test
    void fallsBackToEnglishForNull() {
        assertEquals(Language.ENGLISH, resolver.resolve(null));
    }

    @Test
    void fallsBackToEnglishForEmpty() {
        assertEquals(Language.ENGLISH, resolver.resolve(""));
    }

    @Test
    void fallsBackToEnglishForUnknownLocale() {
        assertEquals(Language.ENGLISH, resolver.resolve("zz_ZZ"));
    }

    @Test
    void resolveOrDefaultWorks() {
        assertEquals(Language.ITALIAN, resolver.resolveOrDefault("it_IT"));
        assertEquals(Language.ENGLISH, resolver.resolveOrDefault(null));
        assertEquals(Language.ENGLISH, resolver.resolveOrDefault("zz_ZZ"));
    }
}
