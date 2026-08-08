package it.legacynetwork.screenshare.platform;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Spostamento di un giocatore fra i server del proxy.
 *
 * <p>Unico punto in cui il dominio tocca la rete: rende verificabile senza
 * proxy sia il percorso felice sia il fallimento del trasferimento.</p>
 */
public interface TransferGateway {

    /**
     * Indica se il server e' registrato sul proxy.
     */
    boolean isRegistered(String serverName);

    /**
     * Sposta il giocatore sul server indicato.
     *
     * @return {@code true} se il giocatore e' arrivato
     */
    CompletableFuture<Boolean> transfer(UUID playerId, String serverName);
}
