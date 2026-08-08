package it.legacynetwork.screenshare.message;

import it.legacynetwork.language.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La lingua restituita da NetworkLanguage va conservata cosi' com'e'.
 */
class ScreenshareLanguageResolverTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    void ogniLinguaSupportataAttraversaIlResolverIntatta() {
        for (Language language : Language.values()) {
            ScreenshareLanguageResolver resolver =
                    new ScreenshareLanguageResolver(id -> language,
                            Language.ENGLISH);

            assertEquals(language, resolver.resolve(playerId),
                    "lingua alterata: " + language.getCode());
        }
    }

    @Test
    void senzaProviderSiUsaIlFallback() {
        ScreenshareLanguageResolver resolver =
                new ScreenshareLanguageResolver(null, Language.GERMAN);

        assertEquals(Language.GERMAN, resolver.resolve(playerId));
        assertFalse(resolver.hasProvider());
    }

    @Test
    void laConsoleUsaIlFallback() {
        ScreenshareLanguageResolver resolver =
                new ScreenshareLanguageResolver(id -> Language.POLISH,
                        Language.ITALIAN);

        assertEquals(Language.ITALIAN, resolver.resolve(null));
        assertTrue(resolver.hasProvider());
    }

    @Test
    void unProviderInErroreNonInterrompeIlComando() {
        ScreenshareLanguageResolver resolver =
                new ScreenshareLanguageResolver(id -> {
                    throw new IllegalStateException("storage irraggiungibile");
                }, Language.FRENCH);

        assertEquals(Language.FRENCH, resolver.resolve(playerId));
    }

    @Test
    void unProviderCheNonSaRispondereRicadeSulFallback() {
        ScreenshareLanguageResolver resolver =
                new ScreenshareLanguageResolver(id -> null, Language.SPANISH);

        assertEquals(Language.SPANISH, resolver.resolve(playerId));
    }

    @Test
    void ilFallbackEuObbligatorio() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScreenshareLanguageResolver(id -> Language.ITALIAN,
                        null));
    }

    @Test
    void nessunaRigaDiChatNasceDaTestoScrittoInJava() throws IOException {
        assertTrue(SourceKeys.hardcodedChatText().isEmpty(),
                "testo visibile scritto in Java: "
                        + SourceKeys.hardcodedChatText());
        assertFalse(SourceKeys.literalKeys().isEmpty(),
                "la scansione dei sorgenti deve trovare le chiavi usate");
    }
}
