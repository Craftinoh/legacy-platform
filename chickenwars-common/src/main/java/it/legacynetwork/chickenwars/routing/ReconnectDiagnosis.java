package it.legacynetwork.chickenwars.routing;

/**
 * Motivo per cui una sessione di reconnect non e' utilizzabile.
 *
 * <p>Serve unicamente a scegliere il messaggio da mostrare al giocatore: la
 * decisione autorevole resta di {@link ReconnectCoordinator#reconnect}, che
 * valuta sessione, istanza e prenotazione nella stessa transazione. Questa
 * diagnosi e' di sola lettura e non instrada nulla.</p>
 */
public enum ReconnectDiagnosis {

    /** Nessuna sessione registrata per il giocatore. */
    NONE,
    /** La finestra di reconnect e' scaduta. */
    EXPIRED,
    /** La sessione e' gia' stata usata da un rientro precedente. */
    CONSUMED,
    /** L'istanza indicata non e' piu' pubblicata nel registry. */
    INSTANCE_MISSING,
    /** L'istanza non invia heartbeat da troppo tempo, oppure e' offline. */
    INSTANCE_OFFLINE,
    /** La partita di quell'istanza si sta concludendo. */
    MATCH_ENDED,
    /** Sessione e istanza risultano valide. */
    READY
}
