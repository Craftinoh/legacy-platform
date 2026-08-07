package it.legacynetwork.chickenwars.model;

import java.util.Locale;

/**
 * Ciclo di vita di un'arena.
 */
public enum ArenaState {

    /** Arena disattivata manualmente o non valida. */
    DISABLED(false),
    /** Arena in configurazione, non accessibile ai giocatori. */
    SETUP(false),
    /** Arena aperta, in attesa del numero minimo di giocatori. */
    WAITING(true),
    /** Conto alla rovescia in corso. */
    STARTING(true),
    /** Partita in corso. */
    IN_GAME(false),
    /** Partita conclusa, riepilogo in corso. */
    ENDING(false),
    /** Ripristino della mappa in corso. */
    RESTARTING(false);

    private final boolean joinable;

    ArenaState(boolean joinable) {
        this.joinable = joinable;
    }

    /**
     * Converte un nome di stato, ignorando maiuscole e spazi.
     *
     * @return lo stato, oppure {@code null} se il nome non corrisponde
     */
    public static ArenaState fromString(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (ArenaState state : values()) {
            if (state.name().equals(normalized)) {
                return state;
            }
        }
        return null;
    }

    /**
     * Indica se un giocatore puo' entrare nell'arena in questo stato.
     */
    public boolean isJoinable() {
        return joinable;
    }
}
