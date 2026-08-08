package it.legacynetwork.reports.support;

import it.legacynetwork.reports.api.Report;
import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.api.ReportStatus;
import it.legacynetwork.reports.repository.ReportPage;
import it.legacynetwork.reports.repository.ReportRepository;
import it.legacynetwork.reports.repository.ReportRepositoryException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage che non risponde mai: serve a verificare che l'errore diventi un
 * messaggio localizzato e non un'eccezione in chat.
 */
public final class FailingReportRepository implements ReportRepository {

    @Override
    public CompletableFuture<Report> insert(Report report) {
        return failed();
    }

    @Override
    public CompletableFuture<Optional<Report>> find(ReportId id) {
        return failed();
    }

    @Override
    public CompletableFuture<Optional<Report>> findByReference(
            String reference) {
        return failed();
    }

    @Override
    public CompletableFuture<ReportPage> listByStatuses(
            Set<ReportStatus> statuses, int page, int pageSize) {
        return failed();
    }

    @Override
    public CompletableFuture<ReportPage> listByTarget(UUID targetId, int page,
                                                      int pageSize) {
        return failed();
    }

    @Override
    public CompletableFuture<Integer> countByReporter(
            UUID reporterId, Set<ReportStatus> statuses) {
        return failed();
    }

    @Override
    public CompletableFuture<Optional<Report>> findRecentDuplicate(
            UUID reporterId, UUID targetId, Instant notBefore) {
        return failed();
    }

    @Override
    public CompletableFuture<Boolean> update(Report updated,
                                             ReportStatus expectedStatus,
                                             long expectedRevision) {
        return failed();
    }

    private static <T> CompletableFuture<T> failed() {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(new ReportRepositoryException(
                "storage non raggiungibile"));
        return future;
    }
}
