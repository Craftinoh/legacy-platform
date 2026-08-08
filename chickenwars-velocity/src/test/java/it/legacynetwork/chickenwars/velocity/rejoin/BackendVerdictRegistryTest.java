package it.legacynetwork.chickenwars.velocity.rejoin;

import it.legacynetwork.chickenwars.routing.RejoinVerdictCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Attesa e consegna dell'esito prodotto dal backend.
 */
class BackendVerdictRegistryTest {

    private RejoinTestSupport.ManualDelayer delayer;
    private BackendVerdictRegistry registry;
    private UUID player;

    @BeforeEach
    void setUp() {
        delayer = new RejoinTestSupport.ManualDelayer();
        registry = new BackendVerdictRegistry(delayer, 5000L);
        player = UUID.randomUUID();
    }

    @Test
    void unEsitoAccettatoChiudeLAttesa() {
        CompletableFuture<BackendVerdict> pending = registry.await(player);

        assertTrue(registry.complete(new RejoinVerdictCodec.Verdict(
                player, true, "", "farm")));

        assertTrue(pending.join().isAccepted());
        assertEquals(0, registry.size());
    }

    @Test
    void unRifiutoPortaMotivoEuArena() {
        CompletableFuture<BackendVerdict> pending = registry.await(player);

        registry.complete(new RejoinVerdictCodec.Verdict(player, false,
                RejoinVerdictCodec.REASON_NO_RESERVATION, "farm"));

        BackendVerdict verdict = pending.join();
        assertFalse(verdict.isAccepted());
        assertEquals(RejoinVerdictCodec.REASON_NO_RESERVATION,
                verdict.getReason());
        assertEquals("farm", verdict.getArenaId());
    }

    @Test
    void senzaRispostaScattaIlTimeout() {
        CompletableFuture<BackendVerdict> pending = registry.await(player);
        assertEquals(1, delayer.pending());

        delayer.fire();

        BackendVerdict verdict = pending.join();
        assertFalse(verdict.isAccepted());
        assertTrue(verdict.isTimeout());
        assertEquals(0, registry.size());
    }

    @Test
    void unEsitoInRitardoDopoIlTimeoutNonRiapreNulla() {
        CompletableFuture<BackendVerdict> pending = registry.await(player);
        delayer.fire();

        assertFalse(registry.complete(new RejoinVerdictCodec.Verdict(
                player, true, "", "farm")));

        assertTrue(pending.join().isTimeout());
    }

    @Test
    void unEsitoPerUnGiocatoreNonAttesoVieneIgnorato() {
        assertFalse(registry.complete(new RejoinVerdictCodec.Verdict(
                UUID.randomUUID(), true, "", "farm")));
    }

    @Test
    void unEsitoNulloVieneIgnorato() {
        assertFalse(registry.complete(null));
    }

    @Test
    void unaSecondaAttesaChiudeLaPrecedente() {
        CompletableFuture<BackendVerdict> first = registry.await(player);
        CompletableFuture<BackendVerdict> second = registry.await(player);

        // Nessun futuro puo' restare appeso per sempre.
        assertTrue(first.join().isTimeout());
        assertFalse(second.isDone());
        assertEquals(1, registry.size());
    }

    @Test
    void laCancellazioneLiberaLAttesa() {
        CompletableFuture<BackendVerdict> pending = registry.await(player);

        assertTrue(registry.cancel(player));

        assertTrue(pending.join().isTimeout());
        assertEquals(0, registry.size());
    }

    @Test
    void loSpegnimentoChiudeTutteLeAttese() {
        CompletableFuture<BackendVerdict> first = registry.await(player);
        CompletableFuture<BackendVerdict> second =
                registry.await(UUID.randomUUID());

        registry.clear();

        assertTrue(first.join().isTimeout());
        assertTrue(second.join().isTimeout());
        assertEquals(0, registry.size());
    }

    @Test
    void unGiocatoreNulloProduceSubitoUnTimeout() {
        assertTrue(registry.await(null).join().isTimeout());
    }

    // ------------------------------------------------------------------
    // Protocollo
    // ------------------------------------------------------------------

    @Test
    void ilProtocolloConservaOgniCampo() {
        byte[] encoded = RejoinVerdictCodec.encode(player, false,
                RejoinVerdictCodec.REASON_WRONG_MATCH, "castle");

        RejoinVerdictCodec.Verdict decoded = RejoinVerdictCodec.decode(encoded);

        assertNotNull(decoded);
        assertEquals(player, decoded.getPlayerId());
        assertFalse(decoded.isAccepted());
        assertEquals(RejoinVerdictCodec.REASON_WRONG_MATCH, decoded.getReason());
        assertEquals("castle", decoded.getArenaId());
    }

    @Test
    void unPayloadMalformatoNonSollevaEccezioni() {
        assertNull(RejoinVerdictCodec.decode(null));
        assertNull(RejoinVerdictCodec.decode(new byte[0]));
        assertNull(RejoinVerdictCodec.decode(new byte[]{1, 2, 3}));
    }

    @Test
    void unAccettazioneNonPortaMotivo() {
        RejoinVerdictCodec.Verdict decoded = RejoinVerdictCodec.decode(
                RejoinVerdictCodec.encode(player, true, "", "farm"));

        assertTrue(decoded.isAccepted());
        assertEquals("", decoded.getReason());
    }
}
