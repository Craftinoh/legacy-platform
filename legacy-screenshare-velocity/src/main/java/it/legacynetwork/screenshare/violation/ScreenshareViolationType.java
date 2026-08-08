package it.legacynetwork.screenshare.violation;

import java.util.Locale;

/**
 * Motivo per cui un controllo si e' chiuso con una violazione.
 *
 * <p>Sono fatti osservabili dal proxy, non giudizi: "si e' scollegato" non
 * significa "e' colpevole", e la valutazione resta allo staff. Per questo non
 * esiste un valore per il fallimento tecnico di un trasferimento: quello e' un
 * guasto, non una violazione.</p>
 */
public enum ScreenshareViolationType {

    /** Il giocatore si e' scollegato a controllo avviato. */
    TARGET_DISCONNECTED,
    /** Il giocatore se n'e' andato prima di raggiungere il server. */
    TARGET_LEFT_DURING_TRANSFER,
    /** Il bersaglio non e' rientrato entro la finestra dopo un restart. */
    TARGET_MISSING_AFTER_RECOVERY,
    /** Lo staffer ha chiuso il controllo dichiarando una violazione. */
    STAFF_DECLARED;

    public String messageKey() {
        return "screenshare.violation." + name().toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }
}
