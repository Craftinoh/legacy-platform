package it.legacynetwork.screenshare.reports;

import it.legacynetwork.reports.api.LegacyReportsApi;
import it.legacynetwork.reports.api.ReportOperationResult;
import it.legacynetwork.reports.api.Report;
import it.legacynetwork.reports.api.ReportEventType;
import it.legacynetwork.reports.api.ReportId;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Collegamento a LegacyReports.
 *
 * <p>Usa direttamente {@link LegacyReportsApi} e i suoi tipi: qui non esiste
 * alcuna copia locale di {@code Report}, {@code ReportId} o
 * {@code ReportStatus}. L'unica cosa che questa classe aggiunge e' la
 * distinzione fra "l'API ha risposto" e "l'API non c'e'", perche' sono due
 * situazioni che meritano due messaggi diversi.</p>
 */
public final class ReportLink {

    private final LegacyReportsApi api;

    public ReportLink(LegacyReportsApi api) {
        this.api = api;
    }

    /**
     * Collegamento assente: LegacyReports non ha ancora pubblicato la sua API.
     */
    public static ReportLink unavailable() {
        return new ReportLink(null);
    }

    public boolean isAvailable() {
        return api != null;
    }

    /**
     * Cerca il report indicato dallo staff.
     *
     * @return vuoto se l'API non c'e' o se il riferimento non corrisponde
     */
    public CompletableFuture<Optional<Report>> findReport(String reference) {
        if (api == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return api.findReportByReference(reference)
                .exceptionally(failure -> Optional.empty());
    }

    /**
     * Segna il report come passato al controllo.
     *
     * @return vuoto se l'API non e' disponibile
     */
    public CompletableFuture<Optional<ReportOperationResult>> markStarted(
            ReportId reportId, UUID staffId, UUID sessionId) {
        if (api == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return api.markScreenshareStarted(reportId, staffId, sessionId)
                .<Optional<ReportOperationResult>>thenApply(Optional::of)
                .exceptionally(failure -> Optional.empty());
    }

    /**
     * Riporta il report all'indagine, registrando come e' finito il controllo.
     *
     * <p>Il report non viene mai chiuso da qui: nessun esito di screenshare
     * equivale a un provvedimento.</p>
     */
    public CompletableFuture<Optional<ReportOperationResult>> markEnded(
            ReportId reportId, UUID staffId, UUID sessionId, String outcomeKey,
            ReportEventType auditType) {
        if (api == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return api.markScreenshareEnded(reportId, staffId, sessionId,
                        outcomeKey, auditType)
                .<Optional<ReportOperationResult>>thenApply(Optional::of)
                .exceptionally(failure -> Optional.empty());
    }

    /**
     * Aggiunge una riga di storico al report senza toccarne lo stato.
     */
    public CompletableFuture<Optional<ReportOperationResult>> addAudit(
            ReportId reportId, UUID actorId, String actorName,
            ReportEventType type, String message) {
        if (api == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return api.addAuditEvent(reportId, actorId, actorName, type, message)
                .<Optional<ReportOperationResult>>thenApply(Optional::of)
                .exceptionally(failure -> Optional.empty());
    }
}
