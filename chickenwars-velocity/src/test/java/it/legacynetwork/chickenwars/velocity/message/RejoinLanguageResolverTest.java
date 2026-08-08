package it.legacynetwork.chickenwars.velocity.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.language.PlayerLanguageProvider;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Risoluzione della lingua tramite il provider reale della rete.
 */
class RejoinLanguageResolverTest {

    /** Provider programmabile, con la stessa forma di quello del proxy. */
    private static final class FakeProvider implements PlayerLanguageProvider {

        private final Map<UUID, Language> known = new HashMap<UUID, Language>();
        private RuntimeException failure;

        FakeProvider with(UUID playerId, Language language) {
            known.put(playerId, language);
            return this;
        }

        FakeProvider throwing(RuntimeException failure) {
            this.failure = failure;
            return this;
        }

        @Override
        public Language getLanguage(UUID playerId) {
            if (failure != null) {
                throw failure;
            }
            return known.get(playerId);
        }
    }

    private final UUID player = UUID.randomUUID();

    @Test
    void laLinguaItalianaVieneRispettata() {
        RejoinLanguageResolver resolver = new RejoinLanguageResolver(
                new FakeProvider().with(player, Language.ITALIAN),
                Language.ENGLISH);

        assertSame(Language.ITALIAN, resolver.resolve(player));
    }

    @Test
    void laLinguaIngleseVieneRispettata() {
        RejoinLanguageResolver resolver = new RejoinLanguageResolver(
                new FakeProvider().with(player, Language.ENGLISH),
                Language.ITALIAN);

        assertSame(Language.ENGLISH, resolver.resolve(player));
    }

    @Test
    void unaTerzaLinguaSupportataNonVieneRidottaAdInglese() {
        Language other = thirdLanguage();
        RejoinLanguageResolver resolver = new RejoinLanguageResolver(
                new FakeProvider().with(player, other), Language.ENGLISH);

        // La rete supporta molte lingue: collassarle sarebbe una perdita.
        assertSame(other, resolver.resolve(player));
    }

    @Test
    void unGiocatoreSenzaLinguaNotaUsaIlFallback() {
        RejoinLanguageResolver resolver = new RejoinLanguageResolver(
                new FakeProvider(), Language.ITALIAN);

        assertSame(Language.ITALIAN, resolver.resolve(UUID.randomUUID()));
    }

    @Test
    void senzaProviderSiUsaIlFallback() {
        RejoinLanguageResolver resolver =
                new RejoinLanguageResolver(null, Language.ITALIAN);

        assertFalse(resolver.hasProvider());
        assertSame(Language.ITALIAN, resolver.resolve(player));
    }

    @Test
    void unProviderInErroreNonPropagaLEccezione() {
        RejoinLanguageResolver resolver = new RejoinLanguageResolver(
                new FakeProvider().throwing(
                        new IllegalStateException("storage giu'")),
                Language.ENGLISH);

        assertSame(Language.ENGLISH, resolver.resolve(player));
    }

    @Test
    void laConsoleUsaIlFallback() {
        RejoinLanguageResolver resolver = new RejoinLanguageResolver(
                new FakeProvider().with(player, Language.ITALIAN),
                Language.ENGLISH);

        assertSame(Language.ENGLISH, resolver.resolve(null));
    }

    @Test
    void unProviderCollegatoVieneSegnalato() {
        assertTrue(new RejoinLanguageResolver(new FakeProvider(),
                Language.ENGLISH).hasProvider());
    }

    @Test
    void iMessaggiUsanoLaLinguaRisoltaEuIPlaceholder() {
        RejoinMessages messages = RejoinMessages.load(Language.ENGLISH);
        RejoinLanguageResolver resolver = new RejoinLanguageResolver(
                new FakeProvider().with(player, Language.ITALIAN),
                Language.ENGLISH);

        PlaceholderValues values = PlaceholderValues.builder()
                .put("server", "chickenwars-1")
                .put("arena", "farm")
                .put("reason", "posto non riservato")
                .build();
        String text = messages.get(resolver.resolve(player),
                "rejoin.backend-rejected", values);

        assertTrue(text.contains("chickenwars-1"), text);
        assertTrue(text.contains("farm"), text);
        assertTrue(text.contains("posto non riservato"), text);
        assertFalse(text.contains("{server}"), text);
        assertFalse(text.contains("{arena}"), text);
        assertFalse(text.contains("{reason}"), text);
    }

    @Test
    void ilFallbackDichiaratoVieneConservato() {
        assertEquals(Language.ITALIAN, new RejoinLanguageResolver(
                null, Language.ITALIAN).getFallback());
    }

    /**
     * Una lingua supportata diversa da italiano e inglese.
     */
    private Language thirdLanguage() {
        for (Language language : Language.values()) {
            if (language != Language.ITALIAN && language != Language.ENGLISH) {
                return language;
            }
        }
        throw new IllegalStateException("la rete supporta piu' lingue");
    }
}
