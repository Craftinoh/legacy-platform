package it.legacynetwork.chickenwars.upgrade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stato autorevole degli upgrade di una squadra.
 */
class TeamUpgradeStateTest {

    private TeamUpgradeState state;

    @BeforeEach
    void setUp() {
        state = new TeamUpgradeState("red");
    }

    @Test
    void loStatoIniziale() {
        for (TeamUpgradeType type : TeamUpgradeType.values()) {
            assertEquals(0, state.getLevel(type), type.name());
        }
        for (RoyalUpgradeType type : RoyalUpgradeType.values()) {
            assertEquals(0, state.getRoyalLevel(type), type.name());
        }
        assertEquals(0, state.getTrapCount());
        assertFalse(state.hasTraps());
    }

    @Test
    void iLivelliAvanzanoUnoAllaVolta() {
        assertEquals(1, state.reserveNextLevel(TeamUpgradeType.PROTECTION, 4));
        assertEquals(2, state.reserveNextLevel(TeamUpgradeType.PROTECTION, 4));
        assertEquals(3, state.reserveNextLevel(TeamUpgradeType.PROTECTION, 4));
        assertEquals(4, state.reserveNextLevel(TeamUpgradeType.PROTECTION, 4));

        assertEquals(4, state.getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void oltreIlMassimoLaRiservaFallisce() {
        state.reserveNextLevel(TeamUpgradeType.SHARPNESS, 1);

        assertEquals(0, state.reserveNextLevel(TeamUpgradeType.SHARPNESS, 1));
        assertEquals(1, state.getLevel(TeamUpgradeType.SHARPNESS));
        assertTrue(state.isMaxed(TeamUpgradeType.SHARPNESS, 1));
    }

    @Test
    void ilRollbackRiportaAlLivelloPrecedente() {
        state.reserveNextLevel(TeamUpgradeType.HASTE, 2);
        int second = state.reserveNextLevel(TeamUpgradeType.HASTE, 2);

        state.rollbackLevel(TeamUpgradeType.HASTE, second);

        assertEquals(1, state.getLevel(TeamUpgradeType.HASTE));
    }

    @Test
    void ilRollbackDelPrimoLivelloAzzera() {
        int first = state.reserveNextLevel(TeamUpgradeType.HEAL_POOL, 1);

        state.rollbackLevel(TeamUpgradeType.HEAL_POOL, first);

        assertEquals(0, state.getLevel(TeamUpgradeType.HEAL_POOL));
    }

    @Test
    void unRollbackObsoletoNonAlteraNulla() {
        state.reserveNextLevel(TeamUpgradeType.PROTECTION, 4);
        state.reserveNextLevel(TeamUpgradeType.PROTECTION, 4);

        // Chi tenta di annullare un livello ormai superato non deve toccare
        // il valore corrente.
        state.rollbackLevel(TeamUpgradeType.PROTECTION, 1);

        assertEquals(2, state.getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void iLivelliRealiSonoIndipendenti() {
        state.reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_VITALITY, 3);
        state.reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_VITALITY, 3);

        assertEquals(2, state.getRoyalLevel(RoyalUpgradeType.ROYAL_VITALITY));
        assertEquals(0, state.getRoyalLevel(RoyalUpgradeType.ROYAL_ARMOR));
        assertEquals(0, state.getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void laCodaTrappoleRispettaIlLimite() {
        assertEquals(0, state.queueTrap("intruder", 3));
        assertEquals(1, state.queueTrap("alarm", 3));
        assertEquals(2, state.queueTrap("intruder", 3));

        assertEquals(-1, state.queueTrap("alarm", 3));
        assertEquals(3, state.getTrapCount());
    }

    @Test
    void laCodaTrappoleEuFifo() {
        state.queueTrap("intruder", 3);
        state.queueTrap("alarm", 3);
        state.queueTrap("miner_fatigue", 3);

        assertEquals("intruder", state.pollTrap());
        assertEquals("alarm", state.pollTrap());
        assertEquals("miner_fatigue", state.pollTrap());
        assertEquals(null, state.pollTrap());
    }

    @Test
    void laCodaRestituisceLOrdineCorrente() {
        state.queueTrap("intruder", 3);
        state.queueTrap("alarm", 3);

        List<String> queue = state.getTrapQueue();

        assertEquals(Arrays.asList("intruder", "alarm"), queue);
        // La copia non altera lo stato.
        queue.clear();
        assertEquals(2, state.getTrapCount());
    }

    @Test
    void rimuovereLUltimaTrappolaAnnullaLAccodamento() {
        state.queueTrap("intruder", 3);
        state.queueTrap("alarm", 3);

        state.removeLastTrap();

        assertEquals(Arrays.asList("intruder"), state.getTrapQueue());
    }

    @Test
    void laPuliziaAzzeraTutto() {
        state.reserveNextLevel(TeamUpgradeType.PROTECTION, 4);
        state.reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_ARMOR, 3);
        state.queueTrap("alarm", 3);

        state.clear();

        assertEquals(0, state.getLevel(TeamUpgradeType.PROTECTION));
        assertEquals(0, state.getRoyalLevel(RoyalUpgradeType.ROYAL_ARMOR));
        assertEquals(0, state.getTrapCount());
    }
}
