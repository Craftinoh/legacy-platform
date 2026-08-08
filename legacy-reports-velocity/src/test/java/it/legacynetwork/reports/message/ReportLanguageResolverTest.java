package it.legacynetwork.reports.message;

import it.legacynetwork.language.Language;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La lingua restituita da NetworkLanguage va conservata cosi' com'e'.
 */
class ReportLanguageResolverTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    void laLinguaDelProviderVieneConservata() {
        ReportLanguageResolver resolver = new ReportLanguageResolver(
                id -> Language.UKRAINIAN, Language.ITALIAN);

        assertEquals(Language.UKRAINIAN, resolver.resolve(playerId));
        assertTrue(resolver.hasProvider());
    }

    @Test
    void ogniLinguaSupportataAttraversaIlResolverIntatta() {
        for (Language language : Language.values()) {
            ReportLanguageResolver resolver = new ReportLanguageResolver(
                    id -> language, Language.ENGLISH);

            assertEquals(language, resolver.resolve(playerId),
                    "lingua alterata: " + language.getCode());
        }
    }

    @Test
    void senzaProviderSiUsaIlFallback() {
        ReportLanguageResolver resolver =
                new ReportLanguageResolver(null, Language.GERMAN);

        assertEquals(Language.GERMAN, resolver.resolve(playerId));
        assertFalse(resolver.hasProvider());
    }

    @Test
    void laConsoleUsaIlFallback() {
        ReportLanguageResolver resolver = new ReportLanguageResolver(
                id -> Language.POLISH, Language.ITALIAN);

        assertEquals(Language.ITALIAN, resolver.resolve(null));
    }

    @Test
    void unProviderInErroreNonInterrompeIlComando() {
        ReportLanguageResolver resolver = new ReportLanguageResolver(id -> {
            throw new IllegalStateException("storage irraggiungibile");
        }, Language.FRENCH);

        assertEquals(Language.FRENCH, resolver.resolve(playerId));
    }

    @Test
    void unProviderCheNonSaRispondereRicadeSulFallback() {
        ReportLanguageResolver resolver =
                new ReportLanguageResolver(id -> null, Language.SPANISH);

        assertEquals(Language.SPANISH, resolver.resolve(playerId));
    }

    @Test
    void ilFallbackEuObbligatorio() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReportLanguageResolver(id -> Language.ITALIAN, null));
    }
}
