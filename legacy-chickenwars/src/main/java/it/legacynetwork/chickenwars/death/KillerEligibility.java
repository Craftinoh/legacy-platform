package it.legacynetwork.chickenwars.death;

import java.util.UUID;

/**
 * Verifica se un candidato uccisore puo' ricevere le ricompense della morte.
 *
 * <p>Isola dal calcolo deterministico i controlli che dipendono dallo stato di
 * gioco (giocatore online, sessione presente e attiva, partita ancora in
 * corso), cosi' che l'orchestratore resti verificabile senza un server.</p>
 */
public interface KillerEligibility {

    /** Nessun uccisore e' mai valido. */
    KillerEligibility NONE = new KillerEligibility() {
        @Override
        public boolean isEligible(UUID killerId) {
            return false;
        }
    };

    /** Ogni uccisore indicato e' considerato valido. */
    KillerEligibility ALWAYS = new KillerEligibility() {
        @Override
        public boolean isEligible(UUID killerId) {
            return killerId != null;
        }
    };

    /**
     * Indica se l'uccisore puo' essere premiato.
     *
     * @param killerId candidato, eventualmente nullo
     * @return {@code true} se le risorse possono essergli trasferite
     */
    boolean isEligible(UUID killerId);
}
