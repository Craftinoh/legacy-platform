package it.legacynetwork.chickenwars.velocity.rejoin;

/**
 * Esito osservabile di una richiesta {@code /cw rejoin}.
 *
 * <p>Ogni esito porta con se' la chiave del messaggio: nessun testo visibile
 * vive nel codice Java.</p>
 */
public enum RejoinOutcome {

    /** Il comando e' stato eseguito dalla console. */
    PLAYER_ONLY("rejoin.player-only", false),
    /** Permesso assente. */
    NO_PERMISSION("rejoin.no-permission", false),
    /** Funzione disabilitata dalla configurazione. */
    DISABLED("rejoin.no-session", false),
    /** Una richiesta dello stesso giocatore e' gia' in corso. */
    ALREADY_IN_PROGRESS("rejoin.already-in-progress", false),
    /** Nessuna sessione di reconnect registrata. */
    NO_SESSION("rejoin.no-session", false),
    /** La finestra di reconnect e' scaduta. */
    EXPIRED("rejoin.expired", false),
    /** La partita di quell'istanza si sta concludendo. */
    MATCH_ENDED("rejoin.match-ended", false),
    /** Il giocatore non puo' piu' rientrare in quella partita. */
    ELIMINATED("rejoin.eliminated", false),
    /** L'istanza non e' viva: heartbeat scaduto oppure offline. */
    INSTANCE_OFFLINE("rejoin.instance-offline", false),
    /** L'istanza esiste ma non e' instradabile ora. */
    INSTANCE_UNAVAILABLE("rejoin.instance-unavailable", false),
    /** Il server pubblicato dall'istanza non e' registrato sul proxy. */
    SERVER_NOT_REGISTERED("rejoin.instance-unavailable", false),
    /** La prenotazione non e' stata creata o e' gia' stata reclamata. */
    RESERVATION_FAILED("rejoin.reservation-failed", false),
    /** Trasferimento riuscito e rientro accettato dal backend. */
    TRANSFER_STARTED("rejoin.transfer-started", true),
    /** Il proxy non e' riuscito a spostare il giocatore. */
    TRANSFER_FAILED("rejoin.transfer-failed", false),
    /**
     * Il server e' stato raggiunto ma la validazione ChickenWars ha rifiutato
     * il rientro, oppure non ha risposto entro il tempo previsto.
     */
    BACKEND_REJECTED("rejoin.backend-rejected", false);

    private final String messageKey;
    private final boolean successful;

    RejoinOutcome(String messageKey, boolean successful) {
        this.messageKey = messageKey;
        this.successful = successful;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
