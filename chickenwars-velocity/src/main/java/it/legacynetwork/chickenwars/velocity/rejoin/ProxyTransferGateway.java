package it.legacynetwork.chickenwars.velocity.rejoin;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Confine verso il proxy: sposta il giocatore e consegna il token al backend.
 *
 * <p>Isolare l'API Velocity dietro questa interfaccia mantiene il coordinatore
 * verificabile senza un proxy in esecuzione.</p>
 */
public interface ProxyTransferGateway {

    /**
     * Indica se il proxy conosce il server pubblicato dall'istanza.
     *
     * <p>Un'istanza viva ma non registrata sul proxy non e' raggiungibile: va
     * riconosciuta prima di consumare la prenotazione.</p>
     */
    boolean isServerRegistered(String serverName);

    /**
     * Indica se il giocatore si trova gia' su quel server.
     */
    boolean isAlreadyConnectedTo(UUID playerId, String serverName);

    /**
     * Sposta il giocatore e consegna la prenotazione al backend.
     *
     * @return {@code true} se la connessione e' stata accettata dal server
     */
    CompletionStage<Boolean> transfer(UUID playerId, String serverName,
                                      String reservationId, String arenaId);

    /**
     * Allontana il giocatore da un'istanza che lo ha rifiutato.
     *
     * <p>Dopo un rifiuto il giocatore resterebbe su un server ChickenWars
     * senza partita: va riportato su un server di ripiego. Viene richiesto una
     * volta sola per tentativo, quindi non puo' innescare un ciclo di
     * trasferimenti.</p>
     *
     * @param rejectedServerName server da evitare nella scelta del ripiego
     * @return {@code true} se il giocatore e' stato spostato altrove
     */
    CompletionStage<Boolean> evacuate(UUID playerId, String rejectedServerName);
}
