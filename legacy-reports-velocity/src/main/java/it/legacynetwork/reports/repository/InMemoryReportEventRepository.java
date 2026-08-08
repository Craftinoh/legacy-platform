package it.legacynetwork.reports.repository;

import it.legacynetwork.reports.model.ReportEvent;
import it.legacynetwork.reports.model.ReportId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Storico in memoria.
 *
 * <p>Come la controparte JDBC non espone alcuna rimozione.</p>
 */
public final class InMemoryReportEventRepository
        implements ReportEventRepository {

    private final List<ReportEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public CompletableFuture<ReportEvent> append(ReportEvent event) {
        events.add(event);
        return CompletableFuture.completedFuture(event);
    }

    @Override
    public CompletableFuture<List<ReportEvent>> findByReport(ReportId reportId,
                                                             int limit) {
        List<ReportEvent> matches = new ArrayList<>();
        for (ReportEvent event : events) {
            if (event.getReportId().equals(reportId)) {
                matches.add(event);
            }
        }
        matches.sort(Comparator.comparing(ReportEvent::getCreatedAt).reversed());
        int safeLimit = Math.max(1, limit);
        if (matches.size() > safeLimit) {
            matches = new ArrayList<>(matches.subList(0, safeLimit));
        }
        return CompletableFuture.completedFuture(matches);
    }

    /**
     * Tutti gli eventi registrati, in ordine di inserimento.
     */
    public List<ReportEvent> all() {
        return new ArrayList<>(events);
    }
}
