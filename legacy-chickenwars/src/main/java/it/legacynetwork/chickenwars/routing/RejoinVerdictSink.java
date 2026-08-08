package it.legacynetwork.chickenwars.routing;

import java.util.UUID;

/**
 * Comunica al proxy l'esito della validazione di un rientro.
 *
 * <p>Separare l'invio dalla decisione permette di verificare con test reali
 * quale esito viene prodotto, senza un server in esecuzione.</p>
 */
public interface RejoinVerdictSink {

    /**
     * Riferisce l'esito di un tentativo di rientro.
     *
     * @param playerId giocatore arrivato
     * @param accepted indica se il rientro e' stato accettato
     * @param reason   identificatore stabile del motivo, vuoto se accettato
     * @param arenaId  arena coinvolta
     */
    void report(UUID playerId, boolean accepted, String reason, String arenaId);
}
