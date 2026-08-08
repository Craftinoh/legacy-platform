package it.legacynetwork.chickenwars.routing;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Lettura diagnostica della sessione di reconnect di un giocatore.
 *
 * <p>Esiste per un solo motivo: distinguere il messaggio da mostrare quando
 * {@link ReconnectCoordinator#reconnect} rifiuta la richiesta. Non prenota,
 * non consuma e non instrada, quindi non costituisce un secondo percorso di
 * routing.</p>
 */
public interface ReconnectSessionInspector {

    /**
     * Descrive lo stato della sessione senza modificarla.
     *
     * @param playerId giocatore osservato
     * @param now      istante di riferimento
     * @return la diagnosi, mai nulla
     */
    CompletionStage<ReconnectDiagnosis> inspect(UUID playerId, long now);
}
