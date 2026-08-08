package it.legacynetwork.screenshare.repository;

import it.legacynetwork.screenshare.model.ScreenshareEvent;
import it.legacynetwork.screenshare.model.ScreenshareSessionId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Storico in memoria: come la controparte JDBC non espone rimozioni.
 */
public final class InMemoryScreenshareEventRepository
        implements ScreenshareEventRepository {

    private final List<ScreenshareEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public CompletableFuture<ScreenshareEvent> append(ScreenshareEvent event) {
        events.add(event);
        return CompletableFuture.completedFuture(event);
    }

    @Override
    public CompletableFuture<List<ScreenshareEvent>> findBySession(
            ScreenshareSessionId sessionId, int limit) {
        List<ScreenshareEvent> matches = new ArrayList<>();
        for (ScreenshareEvent event : events) {
            if (event.getSessionId().equals(sessionId)) {
                matches.add(event);
            }
        }
        matches.sort(Comparator.comparing(ScreenshareEvent::getCreatedAt)
                .reversed());
        int safeLimit = Math.max(1, limit);
        if (matches.size() > safeLimit) {
            matches = new ArrayList<>(matches.subList(0, safeLimit));
        }
        return CompletableFuture.completedFuture(matches);
    }

    /**
     * Tutti gli eventi registrati, in ordine di inserimento.
     */
    public List<ScreenshareEvent> all() {
        return new ArrayList<>(events);
    }
}
