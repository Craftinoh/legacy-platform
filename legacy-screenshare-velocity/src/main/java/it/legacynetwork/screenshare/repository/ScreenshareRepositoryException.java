package it.legacynetwork.screenshare.repository;

/**
 * Errore non recuperabile dello storage delle sessioni.
 *
 * <p>Viene propagata dentro il {@code CompletableFuture}: il servizio la
 * traduce in un esito di errore e il comando mostra un messaggio localizzato,
 * senza mai far vedere un dettaglio SQL in chat.</p>
 */
public final class ScreenshareRepositoryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ScreenshareRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScreenshareRepositoryException(String message) {
        super(message);
    }
}
