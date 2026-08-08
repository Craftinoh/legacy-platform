package it.legacynetwork.reports.api;

import java.util.Locale;

/**
 * Esito di un'operazione sul report.
 *
 * <p>Ogni valore porta la propria chiave di traduzione: chi chiama non
 * costruisce frasi, mostra la chiave nella lingua del destinatario.</p>
 */
public enum ReportOperationStatus {

    /** Operazione applicata. */
    SUCCESS,
    /** Il report era gia' nello stato richiesto: nulla da fare. */
    UNCHANGED,
    /** Nessun report con quell'identificatore. */
    NOT_FOUND,
    /** Passaggio di stato non previsto dalla tabella delle transizioni. */
    INVALID_TRANSITION,
    /** Il report e' in carico a un altro staffer. */
    ALREADY_ASSIGNED,
    /** Il report non e' in carico a nessuno. */
    NOT_ASSIGNED,
    /** Il report e' gia' chiuso. */
    ALREADY_RESOLVED,
    /** Qualcun altro ha modificato il report nel frattempo. */
    CONCURRENT_MODIFICATION,
    /** Il report riguarda un altro giocatore. */
    TARGET_MISMATCH,
    /** Lo storage non ha risposto. */
    REPOSITORY_ERROR;

    /**
     * Indica se l'operazione ha lasciato il report nello stato voluto.
     */
    public boolean isApplied() {
        return this == SUCCESS || this == UNCHANGED;
    }

    /**
     * Chiave di traduzione dell'esito.
     */
    public String messageKey() {
        if (this == SUCCESS || this == UNCHANGED) {
            return "reports.success." + name().toLowerCase(Locale.ROOT);
        }
        return "reports.error." + name().toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }
}
