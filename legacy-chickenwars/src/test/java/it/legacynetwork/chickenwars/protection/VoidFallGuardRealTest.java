package it.legacynetwork.chickenwars.protection;

import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regola anti-drop nel vuoto esercitata sul metodo usato dal listener.
 *
 * <p>Il listener chiama {@code isFallingIntoVoid}: la variante con
 * {@code Player} estrae soltanto mondo e quota, quindi qui viene verificata
 * l'intera decisione, controllo del mondo compreso.</p>
 */
class VoidFallGuardRealTest {

    private static final String ARENA_WORLD = "cw_arena";

    private ArenaDefinition arena(int voidY) {
        ArenaDefinition definition = new ArenaDefinition("arena");
        definition.setWorld(ARENA_WORLD);
        definition.setPos1(new SimpleLocation(ARENA_WORLD,
                -50.0D, 0.0D, -50.0D, 0.0F, 0.0F));
        definition.setPos2(new SimpleLocation(ARENA_WORLD,
                50.0D, 128.0D, 50.0D, 0.0F, 0.0F));
        definition.setVoidY(voidY);
        return definition;
    }

    // ------------------------------------------------------------------
    // Condizione attiva
    // ------------------------------------------------------------------

    @Test
    void sottoLaSogliaLaProtezioneEuAttiva() {
        VoidFallGuard guard = new VoidFallGuard(5.0D);

        assertTrue(guard.isFallingIntoVoid(ARENA_WORLD, -10.0D, arena(0)));
    }

    @Test
    void esattamenteSullaSogliaLaProtezioneEuAttiva() {
        VoidFallGuard guard = new VoidFallGuard(5.0D);

        assertTrue(guard.isFallingIntoVoid(ARENA_WORLD, 5.0D, arena(0)));
    }

    @Test
    void appenaSopraLaSogliaIlDropRestaConsentito() {
        VoidFallGuard guard = new VoidFallGuard(5.0D);

        assertFalse(guard.isFallingIntoVoid(ARENA_WORLD, 5.001D, arena(0)));
    }

    @Test
    void inSuperficieIlDropRestaConsentito() {
        VoidFallGuard guard = new VoidFallGuard(5.0D);

        // Il listener non deve essere restrittivo fuori dalla caduta.
        assertFalse(guard.isFallingIntoVoid(ARENA_WORLD, 64.0D, arena(0)));
    }

    @Test
    void laSogliaSegueIlVoidYDellArena() {
        VoidFallGuard guard = new VoidFallGuard(2.0D);

        assertTrue(guard.isFallingIntoVoid(ARENA_WORLD, 12.0D, arena(10)));
        assertFalse(guard.isFallingIntoVoid(ARENA_WORLD, 12.001D, arena(10)));
        // Lo stesso giocatore, con un voidY piu' basso, non e' protetto.
        assertFalse(guard.isFallingIntoVoid(ARENA_WORLD, 12.0D, arena(0)));
    }

    // ------------------------------------------------------------------
    // Condizione inattiva
    // ------------------------------------------------------------------

    @Test
    void unMondoDiversoNonAttivaMaiLaProtezione() {
        VoidFallGuard guard = new VoidFallGuard(5.0D);

        // Quota bassissima, ma in un altro mondo: nessuna protezione.
        assertFalse(guard.isFallingIntoVoid("altro_mondo", -100.0D, arena(0)));
    }

    @Test
    void ilConfrontoSulMondoIgnoraLeMaiuscole() {
        VoidFallGuard guard = new VoidFallGuard(5.0D);

        assertTrue(guard.isFallingIntoVoid("CW_ARENA", -1.0D, arena(0)));
    }

    @Test
    void senzaArenaNonEsisteProtezione() {
        VoidFallGuard guard = new VoidFallGuard(5.0D);

        assertFalse(guard.isFallingIntoVoid(ARENA_WORLD, -100.0D, null));
    }

    @Test
    void senzaMondoDellArenaNonEsisteProtezione() {
        VoidFallGuard guard = new VoidFallGuard(5.0D);
        ArenaDefinition incomplete = new ArenaDefinition("incompleta");

        assertFalse(guard.isFallingIntoVoid(ARENA_WORLD, -100.0D, incomplete));
    }

    @Test
    void senzaMondoDelGiocatoreNonEsisteProtezione() {
        VoidFallGuard guard = new VoidFallGuard(5.0D);

        assertFalse(guard.isFallingIntoVoid(null, -100.0D, arena(0)));
    }

    // ------------------------------------------------------------------
    // Tolleranza
    // ------------------------------------------------------------------

    @Test
    void laTolleranzaAmpliaLaFasciaProtetta() {
        ArenaDefinition arena = arena(0);

        assertFalse(new VoidFallGuard(0.0D)
                .isFallingIntoVoid(ARENA_WORLD, 4.0D, arena));
        assertTrue(new VoidFallGuard(10.0D)
                .isFallingIntoVoid(ARENA_WORLD, 4.0D, arena));
    }

    @Test
    void unaTolleranzaNegativaVieneAzzerata() {
        VoidFallGuard guard = new VoidFallGuard(-3.0D);

        assertEquals(0.0D, guard.getTolerance(), 0.0001D);
        assertTrue(guard.isFallingIntoVoid(ARENA_WORLD, 0.0D, arena(0)));
        assertFalse(guard.isFallingIntoVoid(ARENA_WORLD, 0.001D, arena(0)));
    }

    @Test
    void laTolleranzaPuoEssereAggiornataARuntime() {
        VoidFallGuard guard = new VoidFallGuard(0.0D);
        assertFalse(guard.isFallingIntoVoid(ARENA_WORLD, 3.0D, arena(0)));

        guard.setTolerance(5.0D);

        assertEquals(5.0D, guard.getTolerance(), 0.0001D);
        assertTrue(guard.isFallingIntoVoid(ARENA_WORLD, 3.0D, arena(0)));

        guard.setTolerance(-1.0D);
        assertEquals(0.0D, guard.getTolerance(), 0.0001D);
    }
}
