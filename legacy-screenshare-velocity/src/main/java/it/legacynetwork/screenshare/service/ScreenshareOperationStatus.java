package it.legacynetwork.screenshare.service;

import java.util.Locale;

/**
 * Esito di un'operazione sulle sessioni di controllo.
 *
 * <p>Ogni valore porta la propria chiave di traduzione: chi chiama mostra la
 * chiave nella lingua del destinatario, non costruisce frasi.</p>
 */
public enum ScreenshareOperationStatus {

    SUCCESS,
    /** Nulla da cambiare: l'operazione era gia' stata applicata. */
    UNCHANGED,
    /** Nessuna sessione con quell'identificatore. */
    NOT_FOUND,
    /** Il giocatore non e' collegato al proxy. */
    TARGET_NOT_FOUND,
    /** Nessun controllo aperto su quel giocatore. */
    NO_SESSION,
    /** Il giocatore e' gia' sotto controllo. */
    TARGET_BUSY,
    /** Lo staffer sta gia' conducendo un controllo. */
    STAFF_BUSY,
    /** Non ci si puo' controllare da soli. */
    SELF_TARGET,
    /** Il server di controllo non e' configurato. */
    SERVER_NOT_CONFIGURED,
    /** Il server di controllo non risulta registrato sul proxy. */
    SERVER_NOT_REGISTERED,
    /** Il report indicato non esiste. */
    REPORT_NOT_FOUND,
    /** Il report indicato riguarda un altro giocatore. */
    REPORT_TARGET_MISMATCH,
    /** Il report indicato e' gia' chiuso. */
    REPORT_FINAL,
    /** LegacyReports non e' disponibile. */
    REPORTS_UNAVAILABLE,
    /** Il trasferimento non e' riuscito. */
    TRANSFER_FAILED,
    /** Passaggio di stato non previsto. */
    INVALID_TRANSITION,
    /** Qualcun altro ha modificato la sessione nel frattempo. */
    CONCURRENT_MODIFICATION,
    /** Il controllo appartiene a un altro staffer. */
    NOT_OWNER,
    /** Lo storage non ha risposto. */
    REPOSITORY_ERROR;

    public boolean isApplied() {
        return this == SUCCESS || this == UNCHANGED;
    }

    public String messageKey() {
        if (this == SUCCESS || this == UNCHANGED) {
            return "screenshare.success." + name().toLowerCase(Locale.ROOT);
        }
        return "screenshare.error." + name().toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }
}
