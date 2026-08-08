package it.legacynetwork.screenshare.repository;

import it.legacynetwork.screenshare.model.ScreenshareSession;
import it.legacynetwork.screenshare.model.ScreenshareSessionId;
import it.legacynetwork.screenshare.model.ScreenshareStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Storage in memoria delle sessioni.
 *
 * <p>Serve ai test e alla modalita' senza database: riproduce fedelmente il
 * contratto condizionale della controparte JDBC, altrimenti i test
 * verificherebbero un'altra cosa. Non e' storage di produzione.</p>
 */
public final class InMemoryScreenshareRepository
        implements ScreenshareRepository {

    private final Map<UUID, ScreenshareSession> sessions =
            new LinkedHashMap<>();

    @Override
    public CompletableFuture<ScreenshareSession> insert(
            ScreenshareSession session) {
        synchronized (sessions) {
            if (sessions.containsKey(session.getId().value())) {
                CompletableFuture<ScreenshareSession> failed =
                        new CompletableFuture<>();
                failed.completeExceptionally(
                        new ScreenshareRepositoryException(
                                "Sessione gia' presente: " + session.getId()));
                return failed;
            }
            sessions.put(session.getId().value(), session);
        }
        return CompletableFuture.completedFuture(session);
    }

    @Override
    public CompletableFuture<Optional<ScreenshareSession>> find(
            ScreenshareSessionId id) {
        synchronized (sessions) {
            return CompletableFuture.completedFuture(
                    Optional.ofNullable(sessions.get(id.value())));
        }
    }

    @Override
    public CompletableFuture<Optional<ScreenshareSession>> findOpenByTarget(
            UUID targetId) {
        List<ScreenshareSession> matches = snapshot(session ->
                session.getTargetId().equals(targetId)
                        && !session.getStatus().isFinal());
        return CompletableFuture.completedFuture(matches.isEmpty()
                ? Optional.empty() : Optional.of(matches.get(0)));
    }

    @Override
    public CompletableFuture<List<ScreenshareSession>> findOpenByStaff(
            UUID staffId) {
        return CompletableFuture.completedFuture(snapshot(session ->
                session.getStaffId().equals(staffId)
                        && !session.getStatus().isFinal()));
    }

    @Override
    public CompletableFuture<List<ScreenshareSession>> findOpen() {
        return CompletableFuture.completedFuture(
                snapshot(session -> !session.getStatus().isFinal()));
    }

    @Override
    public CompletableFuture<ScreensharePage> listByStatuses(
            Set<ScreenshareStatus> statuses, int page, int pageSize) {
        List<ScreenshareSession> matches = snapshot(session ->
                statuses == null || statuses.isEmpty()
                        || statuses.contains(session.getStatus()));
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, pageSize);
        int from = (safePage - 1) * safeSize;
        if (from >= matches.size()) {
            return CompletableFuture.completedFuture(new ScreensharePage(
                    Collections.emptyList(), safePage, safeSize,
                    matches.size()));
        }
        int to = Math.min(matches.size(), from + safeSize);
        return CompletableFuture.completedFuture(new ScreensharePage(
                matches.subList(from, to), safePage, safeSize, matches.size()));
    }

    @Override
    public CompletableFuture<Boolean> update(ScreenshareSession updated,
                                             ScreenshareStatus expectedStatus,
                                             long expectedRevision) {
        synchronized (sessions) {
            ScreenshareSession current = sessions.get(updated.getId().value());
            if (current == null || current.getStatus() != expectedStatus
                    || current.getRevision() != expectedRevision) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
            sessions.put(updated.getId().value(), updated);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }
    }

    private List<ScreenshareSession> snapshot(
            Predicate<ScreenshareSession> filter) {
        List<ScreenshareSession> matches = new ArrayList<>();
        synchronized (sessions) {
            for (ScreenshareSession session : sessions.values()) {
                if (filter.test(session)) {
                    matches.add(session);
                }
            }
        }
        matches.sort(Comparator.comparing(ScreenshareSession::getCreatedAt)
                .reversed()
                .thenComparing(session -> session.getId().storageValue()));
        return matches;
    }
}
