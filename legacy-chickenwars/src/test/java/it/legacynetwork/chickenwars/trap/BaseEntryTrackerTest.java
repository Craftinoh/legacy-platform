package it.legacynetwork.chickenwars.trap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rilevamento edge-triggered degli ingressi in base.
 */
class BaseEntryTrackerTest {

    private static final String ARENA = "farm";
    private static final String TEAM = "red";

    private BaseEntryTracker tracker;
    private UUID player;

    @BeforeEach
    void setUp() {
        tracker = new BaseEntryTracker();
        player = UUID.randomUUID();
    }

    private boolean move(boolean inside) {
        return tracker.update(player, ARENA, TEAM, inside);
    }

    @Test
    void ilPrimoIngressoProduceUnFronte() {
        assertTrue(move(true));
        assertTrue(tracker.isInside(player, ARENA, TEAM));
    }

    @Test
    void restareDentroNonProduceNuoviFronti() {
        assertTrue(move(true));

        assertFalse(move(true));
        assertFalse(move(true));
        assertFalse(move(true));
    }

    @Test
    void uscireERientrareProduceUnNuovoFronte() {
        move(true);
        assertFalse(move(false));
        assertFalse(tracker.isInside(player, ARENA, TEAM));

        assertTrue(move(true));
    }

    @Test
    void restareFuoriNonProduceMaiFronti() {
        assertFalse(move(false));
        assertFalse(move(false));
        assertFalse(tracker.isInside(player, ARENA, TEAM));
    }

    @Test
    void ilMovimentoNelloStessoBloccoNonRiattiva() {
        move(true);

        // Molti eventi di movimento con la stessa condizione: un solo fronte.
        int fronts = 0;
        for (int i = 0; i < 20; i++) {
            if (move(true)) {
                fronts++;
            }
        }

        assertEquals(0, fronts);
    }

    @Test
    void basiDiverseSonoIndipendenti() {
        assertTrue(tracker.update(player, ARENA, "red", true));
        assertTrue(tracker.update(player, ARENA, "blue", true));

        assertFalse(tracker.update(player, ARENA, "red", true));
        assertTrue(tracker.isInside(player, ARENA, "blue"));
    }

    @Test
    void areneDiverseNonSiConfondono() {
        assertTrue(tracker.update(player, "farm", TEAM, true));

        // Stessa squadra, altra arena: e' un ingresso distinto.
        assertTrue(tracker.update(player, "castle", TEAM, true));
        assertTrue(tracker.isInside(player, "farm", TEAM));
    }

    @Test
    void giocatoriDiversiSonoIndipendenti() {
        UUID other = UUID.randomUUID();

        assertTrue(tracker.update(player, ARENA, TEAM, true));
        assertTrue(tracker.update(other, ARENA, TEAM, true));

        assertFalse(tracker.update(player, ARENA, TEAM, true));
        assertTrue(tracker.isInside(other, ARENA, TEAM));
        assertEquals(2, tracker.trackedPlayers());
    }

    @Test
    void dimenticareUnGiocatoreRiarmaIlRilevamento() {
        move(true);

        tracker.forget(player);

        assertFalse(tracker.isInside(player, ARENA, TEAM));
        // Il rientro dopo un disconnect e' un ingresso legittimo.
        assertTrue(move(true));
    }

    @Test
    void laPuliziaDellArenaNonToccaLeAltre() {
        tracker.update(player, "farm", TEAM, true);
        tracker.update(player, "castle", TEAM, true);

        assertEquals(1, tracker.clearArena("farm"));

        assertFalse(tracker.isInside(player, "farm", TEAM));
        assertTrue(tracker.isInside(player, "castle", TEAM));
    }

    @Test
    void laPuliziaCompletaAzzeraTutto() {
        tracker.update(player, ARENA, TEAM, true);
        tracker.update(UUID.randomUUID(), ARENA, TEAM, true);

        tracker.clearAll();

        assertEquals(0, tracker.trackedPlayers());
        assertFalse(tracker.isInside(player, ARENA, TEAM));
    }

    @Test
    void gliIdentificatoriMancantiNonProduconoFronti() {
        assertFalse(tracker.update(null, ARENA, TEAM, true));
        assertFalse(tracker.update(player, null, TEAM, true));
        assertFalse(tracker.update(player, ARENA, null, true));
        assertEquals(0, tracker.trackedPlayers());
    }

    @Test
    void uscireSenzaEssereMaiEntratoNonFallisce() {
        assertFalse(move(false));
        assertEquals(0, tracker.trackedPlayers());
    }
}
