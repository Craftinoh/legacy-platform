package it.legacynetwork.reports.repository;

/**
 * Errore non recuperabile dello storage dei report.
 *
 * <p>Viene propagata dentro il {@code CompletableFuture}: il servizio la
 * traduce in {@code REPOSITORY_ERROR} e il comando mostra un messaggio
 * localizzato, senza mai far vedere un dettaglio SQL in chat.</p>
 */
public final class ReportRepositoryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReportRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReportRepositoryException(String message) {
        super(message);
    }
}
