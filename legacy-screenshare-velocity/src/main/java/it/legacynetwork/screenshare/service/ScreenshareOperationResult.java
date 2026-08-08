package it.legacynetwork.screenshare.service;

import it.legacynetwork.screenshare.model.ScreenshareSession;

import java.util.Optional;

/**
 * Risultato di un'operazione sulle sessioni.
 *
 * <p>Non contiene testo: porta lo stato, la chiave da mostrare e, quando
 * esiste, la sessione aggiornata.</p>
 */
public final class ScreenshareOperationResult {

    private final ScreenshareOperationStatus status;
    private final ScreenshareSession session;
    private final String messageKey;

    private ScreenshareOperationResult(ScreenshareOperationStatus status,
                                       ScreenshareSession session,
                                       String messageKey) {
        if (status == null) {
            throw new IllegalArgumentException("Esito operazione mancante");
        }
        this.status = status;
        this.session = session;
        this.messageKey = messageKey == null || messageKey.trim().isEmpty()
                ? status.messageKey() : messageKey.trim();
    }

    public static ScreenshareOperationResult success(
            ScreenshareSession session, String messageKey) {
        return new ScreenshareOperationResult(
                ScreenshareOperationStatus.SUCCESS, session, messageKey);
    }

    public static ScreenshareOperationResult unchanged(
            ScreenshareSession session, String messageKey) {
        return new ScreenshareOperationResult(
                ScreenshareOperationStatus.UNCHANGED, session, messageKey);
    }

    public static ScreenshareOperationResult failure(
            ScreenshareOperationStatus status) {
        return new ScreenshareOperationResult(status, null, null);
    }

    public static ScreenshareOperationResult failure(
            ScreenshareOperationStatus status, ScreenshareSession session) {
        return new ScreenshareOperationResult(status, session, null);
    }

    public ScreenshareOperationStatus getStatus() {
        return status;
    }

    public Optional<ScreenshareSession> getSession() {
        return Optional.ofNullable(session);
    }

    public String getMessageKey() {
        return messageKey;
    }

    public boolean isApplied() {
        return status.isApplied();
    }

    @Override
    public String toString() {
        return "ScreenshareOperationResult[" + status + "]";
    }
}
