package it.legacynetwork.reports.service;

import it.legacynetwork.reports.api.LegacyReportsApi;
import it.legacynetwork.reports.api.ReportOperationResult;
import it.legacynetwork.reports.api.Report;
import it.legacynetwork.reports.api.ReportEventType;
import it.legacynetwork.reports.api.ReportId;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementazione dell'API pubblica.
 *
 * <p>Adapter sottile sopra {@link ReportService}: non aggiunge regole, si limita
 * a offrire agli altri plugin del proxy la porzione di dominio che serve loro.
 * Le transizioni restano una responsabilita' del servizio.</p>
 */
public final class DefaultLegacyReportsApi implements LegacyReportsApi {

    private final ReportService service;

    public DefaultLegacyReportsApi(ReportService service) {
        if (service == null) {
            throw new IllegalArgumentException("Servizio report mancante");
        }
        this.service = service;
    }

    @Override
    public CompletableFuture<Optional<Report>> findReport(ReportId id) {
        return service.find(id);
    }

    @Override
    public CompletableFuture<Optional<Report>> findReportByReference(
            String reference) {
        return service.findByReference(reference);
    }

    @Override
    public CompletableFuture<ReportOperationResult> markScreenshareStarted(
            ReportId id, UUID staffId, UUID screenshareSessionId) {
        return service.markScreenshareStarted(id, staffId,
                screenshareSessionId);
    }

    @Override
    public CompletableFuture<ReportOperationResult> markScreenshareEnded(
            ReportId id, UUID staffId, UUID screenshareSessionId,
            String outcomeKey) {
        return service.markScreenshareEnded(id, staffId, screenshareSessionId,
                outcomeKey, ReportEventType.SCREENSHARE_ENDED);
    }

    @Override
    public CompletableFuture<ReportOperationResult> markScreenshareEnded(
            ReportId id, UUID staffId, UUID screenshareSessionId,
            String outcomeKey, ReportEventType auditType) {
        return service.markScreenshareEnded(id, staffId, screenshareSessionId,
                outcomeKey, auditType);
    }

    @Override
    public CompletableFuture<ReportOperationResult> addAuditEvent(
            ReportId id, UUID actorId, String actorName, ReportEventType type,
            String message) {
        return service.addAuditEvent(id, actorId, actorName, type, message);
    }
}
