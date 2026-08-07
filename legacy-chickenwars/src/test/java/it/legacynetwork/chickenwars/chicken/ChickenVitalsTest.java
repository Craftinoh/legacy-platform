package it.legacynetwork.chickenwars.chicken;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChickenVitalsTest {

    private static final double DELTA = 0.0001D;

    @Test
    void nasceConVitaEScudoAlMassimo() {
        ChickenVitals vitals = new ChickenVitals(100.0D, 25.0D);

        assertEquals(100.0D, vitals.getHealth(), DELTA);
        assertEquals(25.0D, vitals.getShield(), DELTA);
        assertTrue(vitals.isFullHealth());
        assertTrue(vitals.isFullShield());
        assertFalse(vitals.isDead());
    }

    @Test
    void loScudoAssorbeIlDannoPrimaDellaVita() {
        ChickenVitals vitals = new ChickenVitals(100.0D, 25.0D);

        DamageOutcome outcome = vitals.applyDamage(10.0D);

        assertEquals(10.0D, outcome.getShieldAbsorbed(), DELTA);
        assertEquals(0.0D, outcome.getHealthLost(), DELTA);
        assertEquals(15.0D, vitals.getShield(), DELTA);
        assertEquals(100.0D, vitals.getHealth(), DELTA);
        assertFalse(outcome.isFatal());
    }

    @Test
    void ilDannoEccedenteLoScudoIntaccaLaVita() {
        ChickenVitals vitals = new ChickenVitals(100.0D, 25.0D);

        DamageOutcome outcome = vitals.applyDamage(40.0D);

        assertEquals(25.0D, outcome.getShieldAbsorbed(), DELTA);
        assertEquals(15.0D, outcome.getHealthLost(), DELTA);
        assertEquals(40.0D, outcome.getTotalApplied(), DELTA);
        assertEquals(0.0D, vitals.getShield(), DELTA);
        assertEquals(85.0D, vitals.getHealth(), DELTA);
    }

    @Test
    void ilColpoLetaleVieneSegnalatoComeFatale() {
        ChickenVitals vitals = new ChickenVitals(20.0D, 0.0D);

        DamageOutcome outcome = vitals.applyDamage(25.0D);

        assertTrue(outcome.isFatal());
        assertTrue(vitals.isDead());
        // Il danno applicato non supera mai la vita realmente disponibile.
        assertEquals(20.0D, outcome.getHealthLost(), DELTA);
    }

    @Test
    void unaGallinaMortaNonSubisceAltroDanno() {
        ChickenVitals vitals = new ChickenVitals(10.0D, 0.0D);
        vitals.applyDamage(10.0D);

        DamageOutcome outcome = vitals.applyDamage(5.0D);

        assertTrue(outcome.isNoop());
        assertFalse(outcome.isFatal());
    }

    @Test
    void ilDannoNonPositivoNonProduceEffetti() {
        ChickenVitals vitals = new ChickenVitals(100.0D, 25.0D);

        assertTrue(vitals.applyDamage(0.0D).isNoop());
        assertTrue(vitals.applyDamage(-5.0D).isNoop());
        assertEquals(25.0D, vitals.getShield(), DELTA);
    }

    @Test
    void laCuraNonSuperaLaVitaMassima() {
        ChickenVitals vitals = new ChickenVitals(100.0D, 0.0D);
        vitals.applyDamage(30.0D);

        assertEquals(30.0D, vitals.heal(50.0D), DELTA);
        assertEquals(100.0D, vitals.getHealth(), DELTA);
        assertEquals(0.0D, vitals.heal(10.0D), DELTA);
    }

    @Test
    void loScudoRipristinatoNonSuperaIlMassimo() {
        ChickenVitals vitals = new ChickenVitals(100.0D, 20.0D);
        vitals.applyDamage(15.0D);

        assertEquals(15.0D, vitals.restoreShield(40.0D), DELTA);
        assertEquals(20.0D, vitals.getShield(), DELTA);
    }

    @Test
    void unaGallinaMortaNonPuoEssereCurata() {
        ChickenVitals vitals = new ChickenVitals(10.0D, 5.0D);
        vitals.kill();

        assertEquals(0.0D, vitals.heal(10.0D), DELTA);
        assertEquals(0.0D, vitals.restoreShield(5.0D), DELTA);
        assertTrue(vitals.isDead());
    }

    @Test
    void sopravvivereLasciaLaVitaIndicata() {
        ChickenVitals vitals = new ChickenVitals(100.0D, 0.0D);
        vitals.applyDamage(100.0D);

        vitals.surviveWith(1.0D);

        assertFalse(vitals.isDead());
        assertEquals(1.0D, vitals.getHealth(), DELTA);
    }

    @Test
    void iValoriVisualizzatiSonoArrotondatiPerEccesso() {
        ChickenVitals vitals = new ChickenVitals(100.0D, 10.0D);
        vitals.applyDamage(10.5D);

        assertEquals(0, vitals.getDisplayShield());
        assertEquals(100, vitals.getDisplayHealth());
        assertEquals(0.995D, vitals.getHealthRatio(), DELTA);
    }

    @Test
    void iValoriMassimiNonValidiVengonoRifiutati() {
        assertThrows(IllegalArgumentException.class,
                () -> new ChickenVitals(0.0D, 10.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new ChickenVitals(100.0D, -1.0D));
    }
}
