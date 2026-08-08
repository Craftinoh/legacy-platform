package it.legacynetwork.screenshare.violation;

/**
 * Categoria di provvedimento suggerita a chi ricevera' la violazione.
 *
 * <p>E' un suggerimento e resta tale: questo plugin non applica punizioni e
 * non conosce alcun sistema che lo faccia. Quando LegacyPunishments esistera',
 * un adapter potra' leggere questo valore e decidere.</p>
 */
public enum SuggestedPunishmentCategory {

    /** Nessun suggerimento: la violazione e' solo da registrare. */
    NONE,
    /** Il giocatore si e' sottratto al controllo. */
    SCREENSHARE_EVASION,
    /** Serve una valutazione umana prima di qualunque provvedimento. */
    MANUAL_REVIEW
}
