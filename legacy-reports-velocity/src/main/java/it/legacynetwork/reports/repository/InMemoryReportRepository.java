package it.legacynetwork.reports.repository;

import it.legacynetwork.reports.api.Report;
import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.api.ReportStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage in memoria dei report.
 *
 * <p>Esiste per i test e per far girare il plugin quando il database non e'
 * configurato: non e' lo storage di produzione e non sopravvive a un riavvio.
 * Riproduce fedelmente il contratto condizionale del repository JDBC, altrimenti
 * i test verificherebbero un'altra cosa.</p>
 */
public final class InMemoryReportRepository implements ReportRepository {

    private final Map<UUID, Report> reports =
            new LinkedHashMap<>();

    @Override
    public CompletableFuture<Report> insert(Report report) {
        synchronized (reports) {
            if (reports.containsKey(report.getId().value())) {
                return failed(new ReportRepositoryException(
                        "Report gia' presente: " + report.getId()));
            }
            reports.put(report.getId().value(), report);
        }
        return CompletableFuture.completedFuture(report);
    }

    @Override
    public CompletableFuture<Optional<Report>> find(ReportId id) {
        synchronized (reports) {
            return CompletableFuture.completedFuture(
                    Optional.ofNullable(reports.get(id.value())));
        }
    }

    @Override
    public CompletableFuture<Optional<Report>> findByReference(
            String reference) {
        Optional<String> normalized = ReportId.normalizeReference(reference);
        if (!normalized.isPresent()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        String prefix = normalized.get();
        synchronized (reports) {
            Report found = null;
            for (Report report : reports.values()) {
                if (report.getId().storageValue().startsWith(prefix)) {
                    if (found != null) {
                        // Prefisso ambiguo: nessuna scelta arbitraria.
                        return CompletableFuture.completedFuture(
                                Optional.empty());
                    }
                    found = report;
                }
            }
            return CompletableFuture.completedFuture(Optional.ofNullable(found));
        }
    }

    @Override
    public CompletableFuture<ReportPage> listByStatuses(
            Set<ReportStatus> statuses, int page, int pageSize) {
        return CompletableFuture.completedFuture(paginate(
                snapshot(report -> statuses == null || statuses.isEmpty()
                        || statuses.contains(report.getStatus())),
                page, pageSize));
    }

    @Override
    public CompletableFuture<ReportPage> listByTarget(UUID targetId, int page,
                                                      int pageSize) {
        return CompletableFuture.completedFuture(paginate(
                snapshot(report -> report.getTargetId().equals(targetId)),
                page, pageSize));
    }

    @Override
    public CompletableFuture<Integer> countByReporter(
            UUID reporterId, Set<ReportStatus> statuses) {
        int count = snapshot(report -> report.getReporterId().equals(reporterId)
                && (statuses == null || statuses.isEmpty()
                || statuses.contains(report.getStatus()))).size();
        return CompletableFuture.completedFuture(count);
    }

    @Override
    public CompletableFuture<Optional<Report>> findRecentDuplicate(
            UUID reporterId, UUID targetId, Instant notBefore) {
        List<Report> matches = snapshot(report ->
                report.getReporterId().equals(reporterId)
                        && report.getTargetId().equals(targetId)
                        && !report.getCreatedAt().isBefore(notBefore));
        return CompletableFuture.completedFuture(matches.isEmpty()
                ? Optional.empty() : Optional.of(matches.get(0)));
    }

    @Override
    public CompletableFuture<Boolean> update(Report updated,
                                             ReportStatus expectedStatus,
                                             long expectedRevision) {
        synchronized (reports) {
            Report current = reports.get(updated.getId().value());
            if (current == null || current.getStatus() != expectedStatus
                    || current.getRevision() != expectedRevision) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
            reports.put(updated.getId().value(), updated);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }
    }

    private List<Report> snapshot(java.util.function.Predicate<Report> filter) {
        List<Report> matches = new ArrayList<>();
        synchronized (reports) {
            for (Report report : reports.values()) {
                if (filter.test(report)) {
                    matches.add(report);
                }
            }
        }
        matches.sort(Comparator.comparing(Report::getCreatedAt).reversed()
                .thenComparing(report -> report.getId().storageValue()));
        return matches;
    }

    private static ReportPage paginate(List<Report> matches, int page,
                                       int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, pageSize);
        int from = (safePage - 1) * safeSize;
        if (from >= matches.size()) {
            return new ReportPage(java.util.Collections.emptyList(), safePage,
                    safeSize, matches.size());
        }
        int to = Math.min(matches.size(), from + safeSize);
        return new ReportPage(matches.subList(from, to), safePage, safeSize,
                matches.size());
    }

    private static <T> CompletableFuture<T> failed(Throwable cause) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(cause);
        return future;
    }
}
