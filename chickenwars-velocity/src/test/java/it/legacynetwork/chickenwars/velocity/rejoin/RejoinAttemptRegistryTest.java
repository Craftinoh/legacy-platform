package it.legacynetwork.chickenwars.velocity.rejoin;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protezione dalle richieste concorrenti.
 */
class RejoinAttemptRegistryTest {

    private final UUID player = UUID.randomUUID();

    @Test
    void ilPrimoTentativoVieneAperto() {
        RejoinAttemptRegistry registry = new RejoinAttemptRegistry(1000L);

        assertTrue(registry.begin(player, 0L));
        assertTrue(registry.isInProgress(player, 0L));
        assertEquals(1, registry.size());
    }

    @Test
    void unSecondoTentativoRavvicinatoVieneRifiutato() {
        RejoinAttemptRegistry registry = new RejoinAttemptRegistry(1000L);
        registry.begin(player, 0L);

        assertFalse(registry.begin(player, 500L));
        assertEquals(1, registry.size());
    }

    @Test
    void dopoLaChiusuraUnNuovoTentativoEuConsentito() {
        RejoinAttemptRegistry registry = new RejoinAttemptRegistry(1000L);
        registry.begin(player, 0L);

        assertTrue(registry.finish(player));
        assertEquals(0, registry.size());
        assertTrue(registry.begin(player, 10L));
    }

    @Test
    void unTentativoAbbandonatoScadeDaSolo() {
        RejoinAttemptRegistry registry = new RejoinAttemptRegistry(1000L);
        registry.begin(player, 0L);

        // Senza scadenza un errore che impedisse finish() bloccherebbe
        // il giocatore per sempre.
        assertFalse(registry.isInProgress(player, 1000L));
        assertTrue(registry.begin(player, 1000L));
    }

    @Test
    void giocatoriDiversiNonSiInfluenzano() {
        RejoinAttemptRegistry registry = new RejoinAttemptRegistry(1000L);
        UUID other = UUID.randomUUID();

        assertTrue(registry.begin(player, 0L));
        assertTrue(registry.begin(other, 0L));
        assertEquals(2, registry.size());
    }

    @Test
    void chiudereUnTentativoInesistenteNonFaNulla() {
        RejoinAttemptRegistry registry = new RejoinAttemptRegistry(1000L);

        assertFalse(registry.finish(player));
        assertFalse(registry.finish(null));
    }

    @Test
    void loSpegnimentoDimenticaTutto() {
        RejoinAttemptRegistry registry = new RejoinAttemptRegistry(1000L);
        registry.begin(player, 0L);
        registry.begin(UUID.randomUUID(), 0L);

        registry.clear();

        assertEquals(0, registry.size());
        assertTrue(registry.begin(player, 0L));
    }

    @Test
    void unTimeoutNonPositivoRicadeSuUnValoreUsabile() {
        RejoinAttemptRegistry registry = new RejoinAttemptRegistry(0L);

        assertTrue(registry.getTimeoutMillis() > 0L);
    }
}
