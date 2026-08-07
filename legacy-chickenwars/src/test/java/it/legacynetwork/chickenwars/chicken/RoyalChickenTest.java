package it.legacynetwork.chickenwars.chicken;

import it.legacynetwork.chickenwars.model.ChickenState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoyalChickenTest {

    private RoyalChicken chicken() {
        return new RoyalChicken("red", null, ChickenSettings.fromSection(null));
    }

    private RoyalChicken activeChicken() {
        RoyalChicken royal = chicken();
        royal.releaseProtection();
        return royal;
    }

    @Test
    void nasceProtettaEQuindiImmuneAlDanno() {
        RoyalChicken royal = chicken();

        assertSame(ChickenState.PROTECTED, royal.getState());
        assertTrue(royal.damage(50.0D, UUID.randomUUID()).isNoop());
        assertTrue(royal.isAlive());
    }

    @Test
    void doPoLaProtezionePuoSubireDanno() {
        RoyalChicken royal = activeChicken();
        UUID attacker = UUID.randomUUID();

        DamageOutcome outcome = royal.damage(10.0D, attacker);

        assertFalse(outcome.isNoop());
        assertSame(attacker, royal.getLastAttacker());
        assertSame(ChickenState.SHIELDED, royal.getState());
    }

    @Test
    void passaAStatoDanneggiatoQuandoLoScudoSiEsaurisce() {
        RoyalChicken royal = activeChicken();

        royal.damage(40.0D, UUID.randomUUID());

        assertSame(ChickenState.DAMAGED, royal.getState());
        assertFalse(royal.getVitals().hasShield());
    }

    @Test
    void registraLUccisoreSoloSulColpoFatale() {
        RoyalChicken royal = activeChicken();
        UUID first = UUID.randomUUID();
        UUID killer = UUID.randomUUID();

        royal.damage(10.0D, first);
        assertNull(royal.getKiller());

        royal.damage(1000.0D, killer);

        assertSame(killer, royal.getKiller());
        assertSame(ChickenState.DEAD, royal.getState());
        assertFalse(royal.isAlive());
    }

    @Test
    void unaGallinaMortaNonSubisceAltroDanno() {
        RoyalChicken royal = activeChicken();
        royal.damage(1000.0D, UUID.randomUUID());

        assertTrue(royal.damage(10.0D, UUID.randomUUID()).isNoop());
    }

    @Test
    void laCuraRiportaLoStatoANormaleQuandoPiena() {
        RoyalChicken royal = activeChicken();
        royal.damage(40.0D, UUID.randomUUID());

        royal.heal(1000.0D);

        assertSame(ChickenState.NORMAL, royal.getState());
        assertTrue(royal.getVitals().isFullHealth());
    }

    @Test
    void ilCooldownDegliAvvisiImpedisceLoSpam() {
        RoyalChicken royal = activeChicken();

        assertTrue(royal.tryAlert(60000L));
        assertFalse(royal.tryAlert(60000L));
    }

    @Test
    void ilCooldownDellAlimentazioneImpedisceCurePerpetue() {
        RoyalChicken royal = activeChicken();

        assertTrue(royal.tryFeed(60000L));
        assertFalse(royal.tryFeed(60000L));
    }

    @Test
    void laRigenerazioneNonAgiscePrimaDelRitardo() {
        ChickenSettings settings = ChickenSettings.fromSection(null);
        RoyalChicken royal = activeChicken();
        royal.damage(40.0D, UUID.randomUUID());

        // Il danno e' appena avvenuto: nessuna rigenerazione deve partire.
        // Scudo 25 assorbito, quindi 15 punti vita persi su 100.
        assertFalse(royal.regenerate(settings));
        assertEquals(85, royal.getVitals().getDisplayHealth());
    }

    @Test
    void unaGallinaMortaNonRigenera() {
        ChickenSettings settings = ChickenSettings.fromSection(null);
        RoyalChicken royal = activeChicken();
        royal.damage(1000.0D, null);

        assertFalse(royal.regenerate(settings));
    }
}
