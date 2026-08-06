package it.legacynetwork.language.backend;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageChangeListener;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageStateServiceTest {

    private final LanguageStateService service = new LanguageStateService("en");

    @Test
    void getLanguageReturnsFallbackWhenNotCached() {
        assertEquals(Language.ENGLISH, service.getLanguage(UUID.randomUUID()));
    }

    @Test
    void updateStateUpdatesCache() {
        UUID id = UUID.randomUUID();
        service.updateState(id, Language.ITALIAN, "it_IT");
        assertEquals(Language.ITALIAN, service.getLanguage(id));
    }

    @Test
    void getStateReturnsNullForUnknownPlayer() {
        assertNull(service.getState(UUID.randomUUID()));
    }

    @Test
    void getStateReturnsStateAfterUpdate() {
        UUID id = UUID.randomUUID();
        service.updateState(id, Language.GERMAN, "de_DE");
        LanguageStateService.LanguageState state = service.getState(id);
        assertNotNull(state);
        assertEquals(Language.GERMAN, state.language);
        assertEquals("de_DE", state.locale);
    }

    @Test
    void newerRevisionOverwritesCache() {
        UUID id = UUID.randomUUID();
        service.updateState(id, Language.ENGLISH, "en_US");
        service.updateState(id, Language.SPANISH, "es_ES");
        assertEquals(Language.SPANISH, service.getLanguage(id));
    }

    @Test
    void removeStateClearsCache() {
        UUID id = UUID.randomUUID();
        service.updateState(id, Language.FRENCH, "fr_FR");
        service.removeState(id);
        assertEquals(Language.ENGLISH, service.getLanguage(id));
    }

    @Test
    void registerListenerReceivesEvent() {
        UUID id = UUID.randomUUID();
        final AtomicBoolean fired = new AtomicBoolean(false);
        final AtomicReference<Language> received = new AtomicReference<>();

        service.registerListener(new PlayerLanguageChangeListener() {
            @Override
            public void onLanguageChanged(UUID playerId, Language prev, Language curr) {
                fired.set(true);
                received.set(curr);
            }
        });

        service.fireLanguageChanged(id, Language.ENGLISH, Language.ITALIAN);
        assertTrue(fired.get());
        assertEquals(Language.ITALIAN, received.get());
    }

    @Test
    void unregisterListenerStopsEvents() {
        final AtomicBoolean fired = new AtomicBoolean(false);
        PlayerLanguageChangeListener listener = new PlayerLanguageChangeListener() {
            @Override
            public void onLanguageChanged(UUID playerId, Language prev, Language curr) {
                fired.set(true);
            }
        };
        service.registerListener(listener);
        service.unregisterListener(listener);
        service.fireLanguageChanged(UUID.randomUUID(), Language.ENGLISH, Language.GERMAN);
        assertFalse(fired.get());
    }

    @Test
    void listenerExceptionDoesNotAffectOthers() {
        final AtomicBoolean secondFired = new AtomicBoolean(false);
        service.registerListener(new PlayerLanguageChangeListener() {
            @Override
            public void onLanguageChanged(UUID playerId, Language prev, Language curr) {
                throw new RuntimeException("test exception");
            }
        });
        service.registerListener(new PlayerLanguageChangeListener() {
            @Override
            public void onLanguageChanged(UUID playerId, Language prev, Language curr) {
                secondFired.set(true);
            }
        });
        service.fireLanguageChanged(UUID.randomUUID(), Language.ENGLISH, Language.ITALIAN);
        assertTrue(secondFired.get());
    }

    @Test
    void portugueseBrazilCodeIsPreserved() {
        assertEquals("pt_br", Language.PORTUGUESE_BRAZIL.getCode());
    }

    @Test
    void italianFallbackWorks() {
        LanguageStateService itService = new LanguageStateService("it");
        assertEquals(Language.ITALIAN, itService.getLanguage(UUID.randomUUID()));
    }

    @Test
    void clearRemovesAllStatesAndListeners() {
        UUID id = UUID.randomUUID();
        service.updateState(id, Language.FRENCH, "fr_FR");
        final AtomicBoolean fired = new AtomicBoolean(false);
        service.registerListener(new PlayerLanguageChangeListener() {
            @Override
            public void onLanguageChanged(UUID playerId, Language prev, Language curr) {
                fired.set(true);
            }
        });
        service.clear();
        assertEquals(Language.ENGLISH, service.getLanguage(id));
    }
}
