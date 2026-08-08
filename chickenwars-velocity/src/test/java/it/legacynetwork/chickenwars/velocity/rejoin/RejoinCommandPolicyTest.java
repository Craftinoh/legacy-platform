package it.legacynetwork.chickenwars.velocity.rejoin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Decisioni del comando prima di toccare il sistema distribuito.
 */
class RejoinCommandPolicyTest {

    private static final String PERMISSION = "chickenwars.command.rejoin";

    @Test
    void laConsoleVieneRifiutata() {
        RejoinCommandPolicy policy = new RejoinCommandPolicy(true, PERMISSION);

        assertSame(RejoinOutcome.PLAYER_ONLY, policy.reject(false, true));
    }

    @Test
    void ilPermessoMancanteVieneRifiutato() {
        RejoinCommandPolicy policy = new RejoinCommandPolicy(true, PERMISSION);

        assertSame(RejoinOutcome.NO_PERMISSION, policy.reject(true, false));
    }

    @Test
    void unGiocatoreAutorizzatoProsegue() {
        RejoinCommandPolicy policy = new RejoinCommandPolicy(true, PERMISSION);

        assertNull(policy.reject(true, true));
    }

    @Test
    void laFunzioneDisabilitataVieneRifiutataPrimaDelPermesso() {
        RejoinCommandPolicy policy = new RejoinCommandPolicy(false, PERMISSION);

        assertSame(RejoinOutcome.DISABLED, policy.reject(true, true));
    }

    @Test
    void laConsolePrecedeOgniAltroControllo() {
        RejoinCommandPolicy policy = new RejoinCommandPolicy(false, PERMISSION);

        // Anche con la funzione spenta il motivo corretto resta "solo giocatori".
        assertSame(RejoinOutcome.PLAYER_ONLY, policy.reject(false, false));
    }

    @Test
    void ilPermessoConfiguratoVieneConservato() {
        RejoinCommandPolicy policy =
                new RejoinCommandPolicy(true, " custom.permission ");

        assertEquals("custom.permission", policy.getPermission());
    }

    @Test
    void unPermessoVuotoNonEuAccettabile() {
        assertThrows(IllegalArgumentException.class,
                () -> new RejoinCommandPolicy(true, "  "));
        assertThrows(IllegalArgumentException.class,
                () -> new RejoinCommandPolicy(true, null));
    }

    @Test
    void ogniEsitoHaUnaChiaveDiMessaggio() {
        for (RejoinOutcome outcome : RejoinOutcome.values()) {
            assertEquals(true, outcome.getMessageKey().startsWith("rejoin."),
                    outcome.name());
        }
    }
}
