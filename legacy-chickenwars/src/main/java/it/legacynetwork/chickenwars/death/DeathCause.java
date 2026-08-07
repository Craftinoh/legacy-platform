package it.legacynetwork.chickenwars.death;

/**
 * Origine di una morte elaborata dall'orchestratore.
 *
 * <p>Serve solo a descrivere l'evento: le conseguenze sono identiche per tutte
 * le cause, cosi' non esistono percorsi separati per morte normale, caduta nel
 * vuoto e abbandono in combattimento.</p>
 */
public enum DeathCause {

    /** Colpo di un altro giocatore. */
    COMBAT,
    /** Caduta nel vuoto dell'arena. */
    VOID,
    /** Danno ambientale senza aggressore. */
    ENVIRONMENT,
    /** Morte causata dal giocatore stesso. */
    SUICIDE,
    /** Disconnessione mentre il giocatore era in combattimento. */
    COMBAT_LOGOUT,
    /** Causa non determinabile. */
    UNKNOWN;

    /**
     * Indica se la causa chiude definitivamente la sessione del giocatore.
     *
     * <p>L'abbandono in combattimento non ha respawn: la morte va chiusa
     * subito, mentre le altre cause restano aperte fino al respawn o al
     * passaggio a spettatore.</p>
     */
    public boolean closesSession() {
        return this == COMBAT_LOGOUT;
    }
}
