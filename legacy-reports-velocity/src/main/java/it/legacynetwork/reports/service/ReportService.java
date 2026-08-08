package it.legacynetwork.reports.service;

import it.legacynetwork.reports.api.ReportOperationResult;
import it.legacynetwork.reports.api.ReportOperationStatus;
import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.model.ReportEvent;
import it.legacynetwork.reports.model.ReportEventType;
import it.legacynetwork.reports.model.ReportId;
import it.legacynetwork.reports.model.ReportStatus;
import it.legacynetwork.reports.model.ReportTransitions;
import it.legacynetwork.reports.repository.ReportEventRepository;
import it.legacynetwork.reports.repository.ReportPage;
import it.legacynetwork.reports.repository.ReportRepository;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Unico punto in cui lo stato di un report cambia.
 *
 * <p>I comandi e l'API pubblica descrivono l'intenzione; qui si verifica che la
 * transizione sia prevista dalla tabella, che il report non sia gia' chiuso, che
 * non appartenga a un altro staffer e che nessuno lo abbia modificato nel
 * frattempo. Ogni cambiamento riuscito lascia una riga di storico.</p>
 */
public final class ReportService {

    /** Stati che contano come "ancora aperto" per i limiti del segnalatore. */
    public static final Set<ReportStatus> ACTIVE_STATUSES = EnumSet.of(
            ReportStatus.OPEN, ReportStatus.CLAIMED,
            ReportStatus.INVESTIGATING, ReportStatus.SCREENSHARE);

    private final ReportRepository reports;
    private final ReportEventRepository events;
    private final Supplier<Instant> clock;
    private final String proxyId;

    public ReportService(ReportRepository reports, ReportEventRepository events,
                         Supplier<Instant> clock, String proxyId) {
        if (reports == null || events == null || clock == null) {
            throw new IllegalArgumentException("Servizio report incompleto");
        }
        this.reports = reports;
        this.events = events;
        this.clock = clock;
        this.proxyId = proxyId == null ? "" : proxyId;
    }

    // ---------------------------------------------------------------- lettura

    public CompletableFuture<Optional<Report>> find(ReportId id) {
        return guard(reports.find(id), Optional.<Report>empty());
    }

    public CompletableFuture<Optional<Report>> findByReference(
            String reference) {
        return guard(reports.findByReference(reference),
                Optional.<Report>empty());
    }

    public CompletableFuture<ReportPage> listActive(int page, int pageSize) {
        return reports.listByStatuses(ACTIVE_STATUSES, page, pageSize);
    }

    public CompletableFuture<ReportPage> listByTarget(UUID targetId, int page,
                                                      int pageSize) {
        return reports.listByTarget(targetId, page, pageSize);
    }

    public CompletableFuture<List<ReportEvent>> history(ReportId id,
                                                        int limit) {
        return events.findByReport(id, limit);
    }

    public CompletableFuture<Integer> countActiveByReporter(UUID reporterId) {
        return reports.countByReporter(reporterId, ACTIVE_STATUSES);
    }

    public CompletableFuture<Optional<Report>> findRecentDuplicate(
            UUID reporterId, UUID targetId, Instant notBefore) {
        return reports.findRecentDuplicate(reporterId, targetId, notBefore);
    }

    // -------------------------------------------------------------- creazione

    /**
     * Registra un report gia' validato e ne apre lo storico.
     */
    public CompletableFuture<ReportOperationResult> create(Report report) {
        return reports.insert(report)
                .thenCompose(stored -> events.append(event(stored,
                                ReportEventType.CREATED,
                                stored.getReporterId(),
                                stored.getReporterName(), null,
                                ReportStatus.OPEN,
                                stored.getDetails().orElse(null)))
                        .thenApply(ignored -> ReportOperationResult.success(
                                stored, "reports.success.created")))
                .exceptionally(ReportService::toRepositoryError);
    }

    // ------------------------------------------------------------ transizioni

    /**
     * Prende in carico un report aperto.
     */
    public CompletableFuture<ReportOperationResult> claim(ReportId id,
                                                          UUID staffId,
                                                          String staffName) {
        return apply(id, current -> {
            if (current.getStatus().isFinal()) {
                return Decision.failure(
                        ReportOperationStatus.ALREADY_RESOLVED, current);
            }
            if (current.getStatus().isAssigned()) {
                return current.isAssignedTo(staffId)
                        ? Decision.unchanged(current, "reports.success.claimed")
                        : Decision.failure(
                                ReportOperationStatus.ALREADY_ASSIGNED, current);
            }
            if (!ReportTransitions.isAllowed(current.getStatus(),
                    ReportStatus.CLAIMED)) {
                return Decision.failure(
                        ReportOperationStatus.INVALID_TRANSITION, current);
            }
            Report updated = next(current)
                    .status(ReportStatus.CLAIMED)
                    .assignedStaff(staffId, staffName)
                    .build();
            return Decision.change(updated, ReportEventType.CLAIMED, staffId,
                    staffName, null, "reports.success.claimed");
        });
    }

    /**
     * Avvia l'indagine, prendendo in carico il report se libero.
     */
    public CompletableFuture<ReportOperationResult> investigate(
            ReportId id, UUID staffId, String staffName) {
        return apply(id, current -> {
            if (current.getStatus().isFinal()) {
                return Decision.failure(
                        ReportOperationStatus.ALREADY_RESOLVED, current);
            }
            if (current.getStatus() == ReportStatus.INVESTIGATING) {
                return current.isAssignedTo(staffId)
                        ? Decision.unchanged(current,
                                "reports.success.investigating")
                        : Decision.failure(
                                ReportOperationStatus.ALREADY_ASSIGNED, current);
            }
            if (current.getStatus().isAssigned()
                    && !current.isAssignedTo(staffId)) {
                return Decision.failure(
                        ReportOperationStatus.ALREADY_ASSIGNED, current);
            }
            if (!ReportTransitions.isAllowed(current.getStatus(),
                    ReportStatus.INVESTIGATING)) {
                return Decision.failure(
                        ReportOperationStatus.INVALID_TRANSITION, current);
            }
            Report updated = next(current)
                    .status(ReportStatus.INVESTIGATING)
                    .assignedStaff(staffId, staffName)
                    .build();
            return Decision.change(updated,
                    ReportEventType.INVESTIGATION_STARTED, staffId, staffName,
                    null, "reports.success.investigating");
        });
    }

    /**
     * Restituisce un report preso in carico alla coda comune.
     */
    public CompletableFuture<ReportOperationResult> release(ReportId id,
                                                            UUID staffId,
                                                            String staffName,
                                                            boolean force) {
        return apply(id, current -> {
            if (current.getStatus().isFinal()) {
                return Decision.failure(
                        ReportOperationStatus.ALREADY_RESOLVED, current);
            }
            if (current.getStatus() == ReportStatus.OPEN) {
                return Decision.unchanged(current, "reports.success.released");
            }
            if (!force && !current.isAssignedTo(staffId)) {
                return Decision.failure(
                        ReportOperationStatus.ALREADY_ASSIGNED, current);
            }
            if (!ReportTransitions.isAllowed(current.getStatus(),
                    ReportStatus.OPEN)) {
                return Decision.failure(
                        ReportOperationStatus.INVALID_TRANSITION, current);
            }
            Report updated = next(current)
                    .status(ReportStatus.OPEN)
                    .assignedStaff(null, null)
                    .build();
            return Decision.change(updated, ReportEventType.RELEASED, staffId,
                    staffName, null, "reports.success.released");
        });
    }

    /**
     * Chiude il report senza provvedimenti.
     */
    public CompletableFuture<ReportOperationResult> dismiss(ReportId id,
                                                            UUID staffId,
                                                            String staffName,
                                                            String reason) {
        return close(id, staffId, staffName, reason, ReportStatus.DISMISSED,
                ReportEventType.DISMISSED, "reports.success.dismissed");
    }

    /**
     * Chiude il report registrando che un provvedimento e' stato preso.
     */
    public CompletableFuture<ReportOperationResult> actionTaken(
            ReportId id, UUID staffId, String staffName, String reason) {
        return close(id, staffId, staffName, reason, ReportStatus.ACTION_TAKEN,
                ReportEventType.ACTION_TAKEN, "reports.success.action-taken");
    }

    private CompletableFuture<ReportOperationResult> close(
            ReportId id, UUID staffId, String staffName, String reason,
            ReportStatus target, ReportEventType eventType,
            String successKey) {
        return apply(id, current -> {
            if (current.getStatus() == target) {
                return Decision.unchanged(current, successKey);
            }
            if (current.getStatus().isFinal()) {
                return Decision.failure(
                        ReportOperationStatus.ALREADY_RESOLVED, current);
            }
            if (!current.getStatus().isAssigned()) {
                return Decision.failure(ReportOperationStatus.NOT_ASSIGNED,
                        current);
            }
            if (!ReportTransitions.isAllowed(current.getStatus(), target)) {
                return Decision.failure(
                        ReportOperationStatus.INVALID_TRANSITION, current);
            }
            Report updated = next(current)
                    .status(target)
                    .assignedStaff(staffId, staffName)
                    .resolution(reason)
                    .build();
            return Decision.change(updated, eventType, staffId, staffName,
                    reason, successKey);
        });
    }

    /**
     * Collega una sessione di screenshare al report.
     */
    public CompletableFuture<ReportOperationResult> markScreenshareStarted(
            ReportId id, UUID staffId, UUID sessionId) {
        if (sessionId == null) {
            return completed(ReportOperationResult.failure(
                    ReportOperationStatus.INVALID_TRANSITION));
        }
        return apply(id, current -> {
            if (current.getStatus() == ReportStatus.SCREENSHARE) {
                return sessionId.equals(current.getScreenshareId().orElse(null))
                        ? Decision.unchanged(current,
                                "reports.success.screenshare-started")
                        : Decision.failure(
                                ReportOperationStatus.INVALID_TRANSITION,
                                current);
            }
            if (current.getStatus().isFinal()) {
                return Decision.failure(
                        ReportOperationStatus.ALREADY_RESOLVED, current);
            }
            if (current.getStatus().isAssigned()
                    && !current.isAssignedTo(staffId)) {
                return Decision.failure(
                        ReportOperationStatus.ALREADY_ASSIGNED, current);
            }
            if (!ReportTransitions.isAllowed(current.getStatus(),
                    ReportStatus.SCREENSHARE)) {
                return Decision.failure(
                        ReportOperationStatus.INVALID_TRANSITION, current);
            }
            Report updated = next(current)
                    .status(ReportStatus.SCREENSHARE)
                    .screenshareId(sessionId)
                    .build();
            return Decision.change(updated, ReportEventType.SCREENSHARE_STARTED,
                    staffId, current.getAssignedStaffName().orElse(""),
                    sessionId.toString(),
                    "reports.success.screenshare-started");
        });
    }

    /**
     * Riporta il report all'indagine al termine di un controllo.
     *
     * <p>Il report non viene mai chiuso da qui: l'esito e' una nota di
     * storico.</p>
     */
    public CompletableFuture<ReportOperationResult> markScreenshareEnded(
            ReportId id, UUID staffId, UUID sessionId, String outcomeKey,
            ReportEventType auditType) {
        if (sessionId == null) {
            return completed(ReportOperationResult.failure(
                    ReportOperationStatus.INVALID_TRANSITION));
        }
        ReportEventType type = auditType == null
                ? ReportEventType.SCREENSHARE_ENDED : auditType;
        return apply(id, current -> {
            if (current.getStatus() == ReportStatus.INVESTIGATING
                    && sessionId.equals(
                            current.getScreenshareId().orElse(null))) {
                // Chiusura gia' registrata: nessun secondo evento.
                return Decision.unchanged(current,
                        "reports.success.screenshare-ended");
            }
            if (current.getStatus().isFinal()) {
                return Decision.failure(
                        ReportOperationStatus.ALREADY_RESOLVED, current);
            }
            if (current.getStatus() != ReportStatus.SCREENSHARE) {
                return Decision.failure(
                        ReportOperationStatus.INVALID_TRANSITION, current);
            }
            if (!sessionId.equals(current.getScreenshareId().orElse(null))) {
                return Decision.failure(
                        ReportOperationStatus.INVALID_TRANSITION, current);
            }
            Report updated = next(current)
                    .status(ReportStatus.INVESTIGATING)
                    .build();
            return Decision.change(updated, type, staffId,
                    current.getAssignedStaffName().orElse(""), outcomeKey,
                    "reports.success.screenshare-ended");
        });
    }

    /**
     * Aggiunge una nota allo storico senza toccare lo stato.
     */
    public CompletableFuture<ReportOperationResult> addAuditEvent(
            ReportId id, UUID actorId, String actorName, ReportEventType type,
            String message) {
        String author = actorName == null || actorName.trim().isEmpty()
                ? "system" : actorName;
        ReportEventType eventType =
                type == null ? ReportEventType.NOTE_ADDED : type;
        return reports.find(id)
                .thenCompose(found -> {
                    if (!found.isPresent()) {
                        return completed(ReportOperationResult.failure(
                                ReportOperationStatus.NOT_FOUND));
                    }
                    Report current = found.get();
                    return events.append(event(current, eventType, actorId,
                                    author, current.getStatus(),
                                    current.getStatus(), message))
                            .thenApply(ignored -> ReportOperationResult.success(
                                    current, "reports.success.note-added"));
                })
                .exceptionally(ReportService::toRepositoryError);
    }

    // -------------------------------------------------------------- infrastr.

    private CompletableFuture<ReportOperationResult> apply(ReportId id,
                                                           Mutation mutation) {
        return reports.find(id)
                .thenCompose(found -> {
                    if (!found.isPresent()) {
                        return completed(ReportOperationResult.failure(
                                ReportOperationStatus.NOT_FOUND));
                    }
                    Report current = found.get();
                    Decision decision = mutation.decide(current);
                    if (decision.isImmediate()) {
                        return completed(decision.result());
                    }
                    Report updated = decision.getUpdated();
                    return reports.update(updated, current.getStatus(),
                                    current.getRevision())
                            .thenCompose(changed -> {
                                if (!Boolean.TRUE.equals(changed)) {
                                    return completed(
                                            ReportOperationResult.failure(
                                                    ReportOperationStatus
                                                            .CONCURRENT_MODIFICATION,
                                                    current));
                                }
                                return events.append(event(updated,
                                                decision.getEventType(),
                                                decision.getActorId(),
                                                decision.getActorName(),
                                                current.getStatus(),
                                                updated.getStatus(),
                                                decision.getMessage()))
                                        .thenApply(ignored ->
                                                ReportOperationResult.success(
                                                        updated,
                                                        decision.getSuccessKey()));
                            });
                })
                .exceptionally(ReportService::toRepositoryError);
    }

    private Report.Builder next(Report current) {
        return current.toBuilder()
                .updatedAt(clock.get())
                .revision(current.getRevision() + 1L);
    }

    private ReportEvent event(Report report, ReportEventType type, UUID actorId,
                              String actorName, ReportStatus previous,
                              ReportStatus current, String message) {
        String author = actorName == null || actorName.trim().isEmpty()
                ? "system" : actorName;
        return new ReportEvent(UUID.randomUUID(), report.getId(), actorId,
                author, type, previous, current, message, proxyId, clock.get());
    }

    private static <T> CompletableFuture<T> completed(T value) {
        return CompletableFuture.completedFuture(value);
    }

    private static ReportOperationResult toRepositoryError(Throwable failure) {
        return ReportOperationResult.failure(
                ReportOperationStatus.REPOSITORY_ERROR);
    }

    private static <T> CompletableFuture<T> guard(CompletableFuture<T> future,
                                                  T fallback) {
        return future.exceptionally(failure -> fallback);
    }

    /** Decisione presa osservando lo stato corrente del report. */
    private interface Mutation {
        Decision decide(Report current);
    }

    /**
     * Esito della valutazione: o si conclude subito, o descrive la modifica.
     */
    private static final class Decision {

        private final ReportOperationResult immediate;
        private final Report updated;
        private final ReportEventType eventType;
        private final UUID actorId;
        private final String actorName;
        private final String message;
        private final String successKey;

        private Decision(ReportOperationResult immediate, Report updated,
                         ReportEventType eventType, UUID actorId,
                         String actorName, String message, String successKey) {
            this.immediate = immediate;
            this.updated = updated;
            this.eventType = eventType;
            this.actorId = actorId;
            this.actorName = actorName;
            this.message = message;
            this.successKey = successKey;
        }

        static Decision failure(ReportOperationStatus status, Report current) {
            return new Decision(
                    ReportOperationResult.failure(status, current), null, null,
                    null, null, null, null);
        }

        static Decision unchanged(Report current, String successKey) {
            return new Decision(
                    ReportOperationResult.unchanged(current, successKey), null,
                    null, null, null, null, null);
        }

        static Decision change(Report updated, ReportEventType eventType,
                               UUID actorId, String actorName, String message,
                               String successKey) {
            return new Decision(null, updated, eventType, actorId, actorName,
                    message, successKey);
        }

        boolean isImmediate() {
            return immediate != null;
        }

        ReportOperationResult result() {
            return immediate;
        }

        Report getUpdated() {
            return updated;
        }

        ReportEventType getEventType() {
            return eventType;
        }

        UUID getActorId() {
            return actorId;
        }

        String getActorName() {
            return actorName;
        }

        String getMessage() {
            return message;
        }

        String getSuccessKey() {
            return successKey;
        }
    }
}
