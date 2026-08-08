package it.legacynetwork.screenshare.service;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.model.ReportEventType;
import it.legacynetwork.reports.model.ReportId;
import it.legacynetwork.screenshare.config.ScreenshareConfiguration;
import it.legacynetwork.screenshare.config.StaffDisconnectPolicy;
import it.legacynetwork.screenshare.message.ChatLine;
import it.legacynetwork.screenshare.message.ScreenshareLanguageResolver;
import it.legacynetwork.screenshare.message.ScreensharePresenter;
import it.legacynetwork.screenshare.model.ScreenshareEvent;
import it.legacynetwork.screenshare.model.ScreenshareEventType;
import it.legacynetwork.screenshare.model.ScreenshareOutcome;
import it.legacynetwork.screenshare.model.ScreenshareSession;
import it.legacynetwork.screenshare.model.ScreenshareSessionId;
import it.legacynetwork.screenshare.model.ScreenshareStatus;
import it.legacynetwork.screenshare.model.ScreenshareTransitions;
import it.legacynetwork.screenshare.platform.OnlinePlayer;
import it.legacynetwork.screenshare.platform.PlayerDirectory;
import it.legacynetwork.screenshare.platform.TransferGateway;
import it.legacynetwork.screenshare.reports.ReportLink;
import it.legacynetwork.screenshare.repository.ScreenshareEventRepository;
import it.legacynetwork.screenshare.repository.ScreensharePage;
import it.legacynetwork.screenshare.repository.ScreenshareRepository;
import it.legacynetwork.screenshare.session.ActiveSessionRegistry;
import it.legacynetwork.screenshare.violation.ScreenshareViolation;
import it.legacynetwork.screenshare.violation.ScreenshareViolationHandler;
import it.legacynetwork.screenshare.violation.ScreenshareViolationType;
import it.legacynetwork.screenshare.violation.SuggestedPunishmentCategory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Unico punto in cui una sessione di controllo cambia stato.
 *
 * <p>Regola tutto il ciclo di vita: creazione, trasferimenti, blocco dei cambi
 * server, disconnessioni, scadenze, chiusura e collegamento al report. Ogni
 * passaggio e' condizionato allo stato e alla revisione attesi, quindi ripetere
 * la stessa operazione non produce un secondo effetto.</p>
 *
 * <p>Quello che qui si chiama "controllo" non e' un freeze: il proxy puo'
 * impedire un cambio server e filtrare i propri comandi, nient'altro.
 * Movimento e inventario appartengono al server e non sono raggiungibili da
 * qui.</p>
 */
public final class ScreenshareService {

    /** Stati che tengono la sessione in corso. */
    public static final Set<ScreenshareStatus> OPEN_STATUSES = EnumSet.of(
            ScreenshareStatus.CREATED, ScreenshareStatus.TRANSFERRING,
            ScreenshareStatus.ACTIVE);

    private final ScreenshareConfiguration configuration;
    private final ScreenshareRepository sessions;
    private final ScreenshareEventRepository events;
    private final TransferGateway transfers;
    private final PlayerDirectory directory;
    private final ScreensharePresenter presenter;
    private final ScreenshareLanguageResolver languages;
    private final ReportLink reports;
    private final ScreenshareViolationHandler violations;
    private final ActiveSessionRegistry registry;
    private final Supplier<Instant> clock;

    /** Staff scollegati durante un controllo, in attesa di rientro. */
    private final Map<UUID, Instant> staffAwaySince = new ConcurrentHashMap<>();

    public ScreenshareService(ScreenshareConfiguration configuration,
                              ScreenshareRepository sessions,
                              ScreenshareEventRepository events,
                              TransferGateway transfers,
                              PlayerDirectory directory,
                              ScreensharePresenter presenter,
                              ScreenshareLanguageResolver languages,
                              ReportLink reports,
                              ScreenshareViolationHandler violations,
                              ActiveSessionRegistry registry,
                              Supplier<Instant> clock) {
        if (configuration == null || sessions == null || events == null
                || transfers == null || directory == null || presenter == null
                || languages == null || reports == null || violations == null
                || registry == null || clock == null) {
            throw new IllegalArgumentException("Servizio screenshare incompleto");
        }
        this.configuration = configuration;
        this.sessions = sessions;
        this.events = events;
        this.transfers = transfers;
        this.directory = directory;
        this.presenter = presenter;
        this.languages = languages;
        this.reports = reports;
        this.violations = violations;
        this.registry = registry;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- lettura

    public CompletableFuture<Optional<ScreenshareSession>> find(
            ScreenshareSessionId id) {
        return sessions.find(id).exceptionally(failure -> Optional.empty());
    }

    public CompletableFuture<ScreensharePage> list(int page, int pageSize) {
        return sessions.listByStatuses(null, page, pageSize);
    }

    public CompletableFuture<List<ScreenshareEvent>> history(
            ScreenshareSessionId id, int limit) {
        return events.findBySession(id, limit);
    }

    /**
     * Sessione aperta di un giocatore, cercata per nome.
     *
     * <p>Se il giocatore non e' piu' collegato la ricerca prosegue fra le
     * sessioni aperte: serve a chiudere a mano un controllo rimasto in piedi.</p>
     */
    public CompletableFuture<Optional<ScreenshareSession>> findOpenByName(
            String targetName) {
        Optional<OnlinePlayer> online = directory.findByName(targetName);
        if (online.isPresent()) {
            return sessions.findOpenByTarget(online.get().uniqueId());
        }
        return sessions.findOpen().thenApply(open -> {
            for (ScreenshareSession session : open) {
                if (session.getTargetName().equalsIgnoreCase(
                        targetName == null ? "" : targetName.trim())) {
                    return Optional.of(session);
                }
            }
            return Optional.empty();
        });
    }

    // -------------------------------------------------------------- creazione

    /**
     * Avvia un controllo.
     *
     * @param staff staffer che lo conduce, gia' verificato come giocatore
     * @param targetName nome del giocatore da controllare
     * @param reportReference report da collegare, oppure {@code null}
     */
    public CompletableFuture<ScreenshareOperationResult> start(
            OnlinePlayer staff, String targetName, String reportReference) {
        if (!configuration.hasServer()) {
            return done(ScreenshareOperationResult.failure(
                    ScreenshareOperationStatus.SERVER_NOT_CONFIGURED));
        }
        if (!transfers.isRegistered(configuration.getServer())) {
            return done(ScreenshareOperationResult.failure(
                    ScreenshareOperationStatus.SERVER_NOT_REGISTERED));
        }
        Optional<OnlinePlayer> found = directory.findByName(targetName);
        if (!found.isPresent()) {
            return done(ScreenshareOperationResult.failure(
                    ScreenshareOperationStatus.TARGET_NOT_FOUND));
        }
        OnlinePlayer target = found.get();
        if (target.uniqueId().equals(staff.uniqueId())) {
            return done(ScreenshareOperationResult.failure(
                    ScreenshareOperationStatus.SELF_TARGET));
        }
        boolean linked = reportReference != null
                && !reportReference.trim().isEmpty();
        if (linked && !reports.isAvailable()) {
            return done(ScreenshareOperationResult.failure(
                    ScreenshareOperationStatus.REPORTS_UNAVAILABLE));
        }

        return sessions.findOpenByTarget(target.uniqueId())
                .thenCompose(busy -> {
                    if (busy.isPresent()) {
                        return done(ScreenshareOperationResult.failure(
                                ScreenshareOperationStatus.TARGET_BUSY,
                                busy.get()));
                    }
                    return sessions.findOpenByStaff(staff.uniqueId())
                            .thenCompose(staffSessions -> {
                                if (!configuration
                                        .isAllowMultipleSessionsPerStaff()
                                        && !staffSessions.isEmpty()) {
                                    return done(ScreenshareOperationResult
                                            .failure(ScreenshareOperationStatus
                                                    .STAFF_BUSY,
                                                    staffSessions.get(0)));
                                }
                                return linked
                                        ? startWithReport(staff, target,
                                                reportReference)
                                        : create(staff, target, null);
                            });
                })
                .exceptionally(ScreenshareService::toRepositoryError);
    }

    private CompletableFuture<ScreenshareOperationResult> startWithReport(
            OnlinePlayer staff, OnlinePlayer target, String reference) {
        return reports.findReport(reference).thenCompose(found -> {
            if (!found.isPresent()) {
                return done(ScreenshareOperationResult.failure(
                        ScreenshareOperationStatus.REPORT_NOT_FOUND));
            }
            Report report = found.get();
            if (!report.getTargetId().equals(target.uniqueId())) {
                return done(ScreenshareOperationResult.failure(
                        ScreenshareOperationStatus.REPORT_TARGET_MISMATCH));
            }
            if (report.getStatus().isFinal()) {
                return done(ScreenshareOperationResult.failure(
                        ScreenshareOperationStatus.REPORT_FINAL));
            }
            return create(staff, target, report.getId());
        });
    }

    private CompletableFuture<ScreenshareOperationResult> create(
            OnlinePlayer staff, OnlinePlayer target, ReportId reportId) {
        Instant now = clock.get();
        ScreenshareSession session = ScreenshareSession.builder()
                .id(ScreenshareSessionId.random())
                .target(target.uniqueId(), target.name())
                .staff(staff.uniqueId(), staff.name())
                .reportId(reportId == null ? null : reportId.value())
                .serverId(configuration.getServer())
                .createdAt(now)
                .expiresAt(now.plus(configuration.getMaximumSession()))
                .status(ScreenshareStatus.CREATED)
                .proxyId(configuration.getProxyId())
                .revision(0L)
                .build();

        return sessions.insert(session)
                .thenCompose(stored -> events.append(event(stored,
                                ScreenshareEventType.CREATED,
                                stored.getStaffId(), stored.getStaffName(),
                                null, ScreenshareStatus.CREATED, null))
                        .thenApply(ignored -> stored))
                .thenCompose(stored -> {
                    registry.lock(stored.getTargetId(), stored.getId(),
                            stored.getServerId());
                    registry.assignStaff(stored.getStaffId(), stored.getId());
                    if (reportId == null) {
                        return transferBoth(stored);
                    }
                    return reports.markStarted(reportId, stored.getStaffId(),
                                    stored.getId().value())
                            .thenCompose(result -> {
                                if (!result.isPresent()
                                        || !result.get().isApplied()) {
                                    return failSession(stored,
                                            ScreenshareOperationStatus
                                                    .INVALID_TRANSITION,
                                            "screenshare.audit.report-refused");
                                }
                                return transferBoth(stored);
                            });
                });
    }

    // ------------------------------------------------------------ trasferimenti

    private CompletableFuture<ScreenshareOperationResult> transferBoth(
            ScreenshareSession session) {
        return apply(session, ScreenshareStatus.TRANSFERRING,
                builder -> builder, ScreenshareEventType.TRANSFER_STARTED,
                session.getStaffId(), session.getStaffName(),
                session.getServerId(), "screenshare.success.created")
                .thenCompose(result -> {
                    if (!result.isApplied()) {
                        return done(result);
                    }
                    ScreenshareSession transferring =
                            result.getSession().orElse(session);
                    notify(transferring.getStaffId(),
                            "screenshare.transfer.started",
                            transferring);
                    notify(transferring.getTargetId(),
                            "screenshare.target.session-started",
                            transferring);
                    return transfers.transfer(transferring.getStaffId(),
                                    transferring.getServerId())
                            .thenCompose(staffMoved -> {
                                if (!Boolean.TRUE.equals(staffMoved)) {
                                    return failTransfer(transferring);
                                }
                                return transfers.transfer(
                                                transferring.getTargetId(),
                                                transferring.getServerId())
                                        .thenCompose(targetMoved ->
                                                Boolean.TRUE.equals(targetMoved)
                                                        ? activate(transferring)
                                                        : failTransfer(
                                                                transferring));
                            });
                });
    }

    private CompletableFuture<ScreenshareOperationResult> activate(
            ScreenshareSession session) {
        Instant now = clock.get();
        return apply(session, ScreenshareStatus.ACTIVE,
                builder -> builder.startedAt(now),
                ScreenshareEventType.SESSION_ACTIVE, session.getStaffId(),
                session.getStaffName(), null, "screenshare.success.created")
                .thenApply(result -> {
                    if (result.isApplied()) {
                        ScreenshareSession active =
                                result.getSession().orElse(session);
                        notify(active.getStaffId(),
                                "screenshare.staff.session-active", active);
                        sendAll(active.getTargetId(),
                                presenter.targetInstructions(
                                        languages.resolve(
                                                active.getTargetId()),
                                        active));
                    }
                    return result;
                });
    }

    /**
     * Trasferimento non riuscito: la sessione non resta mai attiva.
     */
    private CompletableFuture<ScreenshareOperationResult> failTransfer(
            ScreenshareSession session) {
        boolean targetGone = !directory.findById(session.getTargetId())
                .isPresent();
        if (targetGone) {
            // Non e' un guasto: il giocatore se n'e' andato durante il
            // trasferimento, ed e' una violazione da registrare.
            return closeWithViolation(session,
                    ScreenshareViolationType.TARGET_LEFT_DURING_TRANSFER,
                    "screenshare.violation.target-left-during-transfer");
        }
        return failSession(session, ScreenshareOperationStatus.TRANSFER_FAILED,
                "screenshare.audit.transfer-failed");
    }

    private CompletableFuture<ScreenshareOperationResult> failSession(
            ScreenshareSession session, ScreenshareOperationStatus status,
            String auditMessage) {
        Instant now = clock.get();
        return apply(session, ScreenshareStatus.FAILED,
                builder -> builder.endedAt(now)
                        .outcome(ScreenshareOutcome.FAILED),
                ScreenshareEventType.TRANSFER_FAILED, session.getStaffId(),
                session.getStaffName(), auditMessage,
                "screenshare.success.failed")
                .thenCompose(result -> {
                    if (!result.isApplied()) {
                        return done(result);
                    }
                    ScreenshareSession failed =
                            result.getSession().orElse(session);
                    return restoreReport(failed, "screenshare.outcome.failed",
                            ReportEventType.SCREENSHARE_FAILED)
                            .thenCompose(ignored -> cleanup(failed))
                            .thenApply(ignored -> {
                                notify(failed.getStaffId(),
                                        "screenshare.transfer.failed", failed);
                                notify(failed.getTargetId(),
                                        "screenshare.target.session-ended",
                                        failed);
                                return ScreenshareOperationResult.failure(
                                        status, failed);
                            });
                });
    }

    // ---------------------------------------------------------------- chiusura

    /**
     * Chiude un controllo con l'esito indicato dallo staff.
     */
    public CompletableFuture<ScreenshareOperationResult> stop(
            UUID staffId, String staffName, String targetName,
            ScreenshareOutcome outcome, boolean force) {
        return withOpenSession(targetName, staffId, force, session -> {
            if (outcome == ScreenshareOutcome.VIOLATION) {
                return closeWithViolation(session,
                        ScreenshareViolationType.STAFF_DECLARED,
                        ScreenshareViolationType.STAFF_DECLARED.messageKey());
            }
            Instant now = clock.get();
            return apply(session, ScreenshareStatus.COMPLETED,
                    builder -> builder.endedAt(now).outcome(outcome),
                    ScreenshareEventType.COMPLETED, staffId, staffName,
                    outcome.messageKey(), "screenshare.success.completed")
                    .thenCompose(result -> after(result, session,
                            outcome.messageKey(),
                            ReportEventType.SCREENSHARE_ENDED,
                            "screenshare.staff.session-completed",
                            "screenshare.target.session-ended"));
        });
    }

    /**
     * Annulla un controllo in corso.
     */
    public CompletableFuture<ScreenshareOperationResult> cancel(
            UUID staffId, String staffName, String targetName, String reason,
            boolean force) {
        return withOpenSession(targetName, staffId, force, session -> {
            Instant now = clock.get();
            return apply(session, ScreenshareStatus.CANCELLED,
                    builder -> builder.endedAt(now)
                            .outcome(ScreenshareOutcome.CANCELLED)
                            .notes(session.appendNote(reason)),
                    ScreenshareEventType.CANCELLED, staffId, staffName, reason,
                    "screenshare.success.cancelled")
                    .thenCompose(result -> after(result, session,
                            "screenshare.outcome.cancelled",
                            ReportEventType.SCREENSHARE_CANCELLED,
                            "screenshare.staff.session-cancelled",
                            "screenshare.target.session-cancelled"));
        });
    }

    /**
     * Aggiunge una nota alla sessione, senza cambiarne lo stato.
     */
    public CompletableFuture<ScreenshareOperationResult> note(
            UUID staffId, String staffName, String targetName, String note) {
        return withOpenSession(targetName, staffId, true, session ->
                apply(session, session.getStatus(),
                        builder -> builder.notes(session.appendNote(note)),
                        ScreenshareEventType.NOTE_ADDED, staffId, staffName,
                        note, "screenshare.success.note-added")
                        .thenCompose(result -> {
                            if (!result.isApplied()) {
                                return done(result);
                            }
                            ScreenshareSession updated =
                                    result.getSession().orElse(session);
                            return reportIdOf(updated)
                                    .map(reportId -> reports.addAudit(reportId,
                                                    staffId, staffName,
                                                    ReportEventType.NOTE_ADDED,
                                                    note)
                                            .thenApply(ignored -> result))
                                    .orElseGet(() -> done(result));
                        }));
    }

    // ------------------------------------------------------- eventi di rete

    /**
     * Il bersaglio si e' scollegato: la sessione si chiude in violazione.
     *
     * <p>Non viene eseguito alcun comando di punizione: la violazione viene
     * consegnata alla porta dedicata, che oggi si limita a registrarla.</p>
     */
    public CompletableFuture<ScreenshareOperationResult> onTargetDisconnect(
            UUID targetId) {
        return sessions.findOpenByTarget(targetId)
                .thenCompose(found -> {
                    if (!found.isPresent()) {
                        return done(ScreenshareOperationResult.failure(
                                ScreenshareOperationStatus.NO_SESSION));
                    }
                    ScreenshareSession session = found.get();
                    ScreenshareViolationType type =
                            session.getStatus() == ScreenshareStatus.ACTIVE
                                    ? ScreenshareViolationType
                                            .TARGET_DISCONNECTED
                                    : ScreenshareViolationType
                                            .TARGET_LEFT_DURING_TRANSFER;
                    return closeWithViolation(session, type,
                            type.messageKey());
                })
                .exceptionally(ScreenshareService::toRepositoryError);
    }

    /**
     * Lo staffer si e' scollegato: si applica la politica configurata.
     */
    public CompletableFuture<ScreenshareOperationResult> onStaffDisconnect(
            UUID staffId) {
        return sessions.findOpenByStaff(staffId)
                .thenCompose(open -> {
                    if (open.isEmpty()) {
                        return done(ScreenshareOperationResult.failure(
                                ScreenshareOperationStatus.NO_SESSION));
                    }
                    ScreenshareSession session = open.get(0);
                    if (configuration.getStaffDisconnectPolicy()
                            == StaffDisconnectPolicy.CANCEL) {
                        return cancelSession(session,
                                "screenshare.staff.staff-disconnected");
                    }
                    staffAwaySince.put(staffId, clock.get());
                    return events.append(event(session,
                                    ScreenshareEventType.STAFF_DISCONNECTED,
                                    staffId, session.getStaffName(), null, null,
                                    null))
                            .thenApply(ignored -> {
                                notify(session.getTargetId(),
                                        "screenshare.target.staff-away",
                                        session);
                                return ScreenshareOperationResult.unchanged(
                                        session,
                                        "screenshare.success.unchanged");
                            });
                })
                .exceptionally(ScreenshareService::toRepositoryError);
    }

    /**
     * Lo staffer e' rientrato entro la finestra concessa.
     */
    public CompletableFuture<ScreenshareOperationResult> onStaffReconnect(
            UUID staffId) {
        if (staffAwaySince.remove(staffId) == null) {
            return done(ScreenshareOperationResult.failure(
                    ScreenshareOperationStatus.NO_SESSION));
        }
        return sessions.findOpenByStaff(staffId)
                .thenCompose(open -> {
                    if (open.isEmpty()) {
                        return done(ScreenshareOperationResult.failure(
                                ScreenshareOperationStatus.NO_SESSION));
                    }
                    ScreenshareSession session = open.get(0);
                    return events.append(event(session,
                                    ScreenshareEventType.STAFF_RECONNECTED,
                                    staffId, session.getStaffName(), null, null,
                                    null))
                            .thenApply(ignored -> {
                                notify(session.getStaffId(),
                                        "screenshare.staff.staff-reconnected",
                                        session);
                                return ScreenshareOperationResult.unchanged(
                                        session,
                                        "screenshare.success.unchanged");
                            });
                })
                .exceptionally(ScreenshareService::toRepositoryError);
    }

    /**
     * Registra il blocco di un cambio server nello storico.
     */
    public CompletableFuture<Void> recordBlockedSwitch(
            ScreenshareSessionId sessionId, UUID playerId, String playerName,
            String requestedServer) {
        return sessions.find(sessionId).thenCompose(found -> {
            if (!found.isPresent()) {
                return CompletableFuture.<Void>completedFuture(null);
            }
            return events.append(event(found.get(),
                            ScreenshareEventType.SERVER_SWITCH_BLOCKED,
                            playerId, playerName, null, null, requestedServer))
                    .<Void>thenApply(ignored -> null);
        }).exceptionally(failure -> null);
    }

    /**
     * Registra il blocco di un comando nello storico.
     */
    public CompletableFuture<Void> recordBlockedCommand(
            ScreenshareSessionId sessionId, UUID playerId, String playerName,
            String label) {
        return sessions.find(sessionId).thenCompose(found -> {
            if (!found.isPresent()) {
                return CompletableFuture.<Void>completedFuture(null);
            }
            return events.append(event(found.get(),
                            ScreenshareEventType.COMMAND_BLOCKED, playerId,
                            playerName, null, null, label))
                    .<Void>thenApply(ignored -> null);
        }).exceptionally(failure -> null);
    }

    // ------------------------------------------------------- scadenze e avvio

    /**
     * Passata periodica: trasferimenti scaduti, sessioni troppo lunghe e
     * finestre di rientro esaurite.
     *
     * <p>Un solo compito centrale, non un task per sessione.</p>
     */
    public CompletableFuture<Integer> tick() {
        Instant now = clock.get();
        return sessions.findOpen().thenCompose(open -> {
            CompletableFuture<Integer> chain =
                    CompletableFuture.completedFuture(0);
            for (ScreenshareSession session : open) {
                chain = chain.thenCompose(count ->
                        tickSession(session, now)
                                .thenApply(closed -> count
                                        + (closed ? 1 : 0)));
            }
            return chain;
        }).exceptionally(failure -> 0);
    }

    private CompletableFuture<Boolean> tickSession(ScreenshareSession session,
                                                   Instant now) {
        if (session.getStatus() != ScreenshareStatus.ACTIVE) {
            Instant deadline = session.getCreatedAt()
                    .plus(configuration.getTransferTimeout());
            if (now.isAfter(deadline)) {
                return timeout(session).thenApply(result -> true);
            }
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        if (now.isAfter(session.getExpiresAt())) {
            return timeout(session).thenApply(result -> true);
        }
        Instant away = staffAwaySince.get(session.getStaffId());
        if (away != null && now.isAfter(
                away.plus(configuration.getStaffReconnectGrace()))) {
            staffAwaySince.remove(session.getStaffId());
            return cancelSession(session, "screenshare.staff.staff-disconnected")
                    .thenApply(result -> true);
        }
        return CompletableFuture.completedFuture(Boolean.FALSE);
    }

    private CompletableFuture<ScreenshareOperationResult> timeout(
            ScreenshareSession session) {
        Instant now = clock.get();
        boolean active = session.getStatus() == ScreenshareStatus.ACTIVE;
        ScreenshareStatus target = active ? ScreenshareStatus.CANCELLED
                : ScreenshareStatus.FAILED;
        ScreenshareOutcome outcome = active ? ScreenshareOutcome.CANCELLED
                : ScreenshareOutcome.FAILED;
        return apply(session, target,
                builder -> builder.endedAt(now).outcome(outcome),
                ScreenshareEventType.TIMED_OUT, null, "system", null,
                active ? "screenshare.success.cancelled"
                        : "screenshare.success.failed")
                .thenCompose(result -> after(result, session,
                        outcome.messageKey(),
                        active ? ReportEventType.SCREENSHARE_CANCELLED
                                : ReportEventType.SCREENSHARE_FAILED,
                        "screenshare.staff.session-timeout",
                        "screenshare.target.session-ended"));
    }

    /**
     * Ripristino all'avvio: nessuna sessione resta appesa e nessun vincolo
     * sopravvive a un riavvio.
     *
     * @return il numero di sessioni chiuse d'ufficio
     */
    public CompletableFuture<Integer> recover() {
        registry.clear();
        staffAwaySince.clear();
        return sessions.findOpen().thenCompose(open -> {
            CompletableFuture<Integer> chain =
                    CompletableFuture.completedFuture(0);
            for (ScreenshareSession session : open) {
                chain = chain.thenCompose(count -> recoverSession(session)
                        .thenApply(closed -> count + (closed ? 1 : 0)));
            }
            return chain;
        }).exceptionally(failure -> 0);
    }

    private CompletableFuture<Boolean> recoverSession(
            ScreenshareSession session) {
        Instant now = clock.get();
        boolean targetOnline = directory.findById(session.getTargetId())
                .isPresent();
        boolean staffOnline = directory.findById(session.getStaffId())
                .isPresent();

        if (session.getStatus() != ScreenshareStatus.ACTIVE) {
            // Creata o in trasferimento prima del riavvio: non e' recuperabile.
            return apply(session, ScreenshareStatus.FAILED,
                    builder -> builder.endedAt(now)
                            .outcome(ScreenshareOutcome.FAILED),
                    ScreenshareEventType.RECOVERED, null, "system",
                    "screenshare.audit.recovered", "screenshare.success.failed")
                    .thenCompose(result -> after(result, session,
                            "screenshare.outcome.failed",
                            ReportEventType.SCREENSHARE_FAILED,
                            "screenshare.staff.session-failed",
                            "screenshare.target.session-ended"))
                    .thenApply(result -> true);
        }
        if (targetOnline && staffOnline) {
            // Entrambi ci sono: il vincolo va solo ricostruito.
            registry.lock(session.getTargetId(), session.getId(),
                    session.getServerId());
            registry.assignStaff(session.getStaffId(), session.getId());
            return events.append(event(session,
                            ScreenshareEventType.RECOVERED, null, "system",
                            null, null, "screenshare.audit.recovered"))
                    .thenApply(ignored -> Boolean.FALSE);
        }
        if (!targetOnline) {
            return closeWithViolation(session,
                    ScreenshareViolationType.TARGET_DISCONNECTED,
                    "screenshare.violation.target-disconnected")
                    .thenApply(result -> true);
        }
        return cancelSession(session, "screenshare.staff.staff-disconnected")
                .thenApply(result -> true);
    }

    // -------------------------------------------------------------- chiusure

    private CompletableFuture<ScreenshareOperationResult> cancelSession(
            ScreenshareSession session, String auditMessage) {
        Instant now = clock.get();
        return apply(session, ScreenshareStatus.CANCELLED,
                builder -> builder.endedAt(now)
                        .outcome(ScreenshareOutcome.CANCELLED),
                ScreenshareEventType.CANCELLED, null, "system", auditMessage,
                "screenshare.success.cancelled")
                .thenCompose(result -> after(result, session,
                        "screenshare.outcome.cancelled",
                        ReportEventType.SCREENSHARE_CANCELLED,
                        "screenshare.staff.session-cancelled",
                        "screenshare.target.session-cancelled"));
    }

    private CompletableFuture<ScreenshareOperationResult> closeWithViolation(
            ScreenshareSession session, ScreenshareViolationType type,
            String auditMessage) {
        Instant now = clock.get();
        return apply(session, ScreenshareStatus.VIOLATION,
                builder -> builder.endedAt(now)
                        .outcome(ScreenshareOutcome.VIOLATION),
                ScreenshareEventType.VIOLATION, null, "system", auditMessage,
                "screenshare.success.violation")
                .thenCompose(result -> {
                    if (!result.isApplied()) {
                        // Un secondo disconnect non emette una seconda
                        // violazione: la transizione condizionale l'ha gia'
                        // consumata.
                        return done(result);
                    }
                    ScreenshareSession closed =
                            result.getSession().orElse(session);
                    violations.handle(violation(closed, type, now));
                    return after(result, session,
                            "screenshare.outcome.violation",
                            ReportEventType.SCREENSHARE_VIOLATION,
                            "screenshare.staff.target-disconnected",
                            "screenshare.target.session-ended");
                });
    }

    private ScreenshareViolation violation(ScreenshareSession session,
                                           ScreenshareViolationType type,
                                           Instant now) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("server", session.getServerId());
        context.put("status", session.getStatus().name());
        context.put("createdAt", session.getCreatedAt().toString());
        session.getStartedAt().ifPresent(
                started -> context.put("startedAt", started.toString()));
        context.put("proxy", session.getProxyId());
        return new ScreenshareViolation(session.getTargetId(),
                session.getTargetName(), session.getStaffId(),
                session.getStaffName(), session.getId(),
                session.getReportId().orElse(null), type, now, context,
                SuggestedPunishmentCategory.SCREENSHARE_EVASION);
    }

    /**
     * Passi comuni a ogni chiusura: report, vincoli, messaggi.
     */
    private CompletableFuture<ScreenshareOperationResult> after(
            ScreenshareOperationResult result, ScreenshareSession previous,
            String outcomeKey, ReportEventType auditType, String staffKey,
            String targetKey) {
        if (!result.isApplied()) {
            return done(result);
        }
        ScreenshareSession closed = result.getSession().orElse(previous);
        return restoreReport(closed, outcomeKey, auditType)
                .thenCompose(ignored -> cleanup(closed))
                .thenApply(ignored -> {
                    notify(closed.getStaffId(), staffKey, closed);
                    notify(closed.getTargetId(), targetKey, closed);
                    return result;
                });
    }

    /**
     * Riporta il report all'indagine, senza mai chiuderlo.
     */
    private CompletableFuture<Void> restoreReport(ScreenshareSession session,
                                                  String outcomeKey,
                                                  ReportEventType auditType) {
        Optional<ReportId> reportId = reportIdOf(session);
        if (!reportId.isPresent()) {
            return CompletableFuture.completedFuture(null);
        }
        return reports.markEnded(reportId.get(), session.getStaffId(),
                        session.getId().value(), outcomeKey, auditType)
                .thenAccept(result -> {
                    if (result.isPresent() && result.get().isApplied()) {
                        notify(session.getStaffId(),
                                "screenshare.staff.report-updated", session);
                    } else {
                        notify(session.getStaffId(),
                                "screenshare.staff.report-unavailable",
                                session);
                    }
                });
    }

    /**
     * Rimuove i vincoli e riporta il bersaglio su un server di rientro.
     */
    private CompletableFuture<Void> cleanup(ScreenshareSession session) {
        registry.allowCleanup(session.getTargetId());
        registry.releaseStaff(session.getStaffId());
        staffAwaySince.remove(session.getStaffId());
        CompletableFuture<Void> chain =
                CompletableFuture.completedFuture(null);
        for (String fallback : configuration.getFallbackServers()) {
            if (!transfers.isRegistered(fallback)) {
                continue;
            }
            if (!directory.findById(session.getTargetId()).isPresent()) {
                break;
            }
            chain = chain.thenCompose(ignored -> transfers.transfer(
                    session.getTargetId(), fallback).thenApply(moved -> null));
            break;
        }
        return chain.handle((ignored, failure) -> null)
                .thenApply(ignored -> {
                    registry.unlock(session.getTargetId());
                    return null;
                });
    }

    // -------------------------------------------------------------- infrastr.

    private CompletableFuture<ScreenshareOperationResult> withOpenSession(
            String targetName, UUID staffId, boolean force,
            java.util.function.Function<ScreenshareSession,
                    CompletableFuture<ScreenshareOperationResult>> action) {
        return findOpenByName(targetName)
                .thenCompose(found -> {
                    if (!found.isPresent()) {
                        return done(ScreenshareOperationResult.failure(
                                ScreenshareOperationStatus.NO_SESSION));
                    }
                    ScreenshareSession session = found.get();
                    if (!force && !session.getStaffId().equals(staffId)) {
                        return done(ScreenshareOperationResult.failure(
                                ScreenshareOperationStatus.NOT_OWNER, session));
                    }
                    return action.apply(session);
                })
                .exceptionally(ScreenshareService::toRepositoryError);
    }

    private CompletableFuture<ScreenshareOperationResult> apply(
            ScreenshareSession current, ScreenshareStatus target,
            UnaryOperator<ScreenshareSession.Builder> mutator,
            ScreenshareEventType eventType, UUID actorId, String actorName,
            String message, String successKey) {
        boolean sameStatus = current.getStatus() == target;
        if (!sameStatus
                && !ScreenshareTransitions.isAllowed(current.getStatus(),
                        target)) {
            return done(ScreenshareOperationResult.failure(
                    ScreenshareOperationStatus.INVALID_TRANSITION, current));
        }
        ScreenshareSession updated = mutator.apply(current.toBuilder()
                        .status(target)
                        .revision(current.getRevision() + 1L))
                .build();
        return sessions.update(updated, current.getStatus(),
                        current.getRevision())
                .thenCompose(changed -> {
                    if (!Boolean.TRUE.equals(changed)) {
                        return done(ScreenshareOperationResult.failure(
                                ScreenshareOperationStatus
                                        .CONCURRENT_MODIFICATION, current));
                    }
                    return events.append(event(updated, eventType, actorId,
                                    actorName, current.getStatus(),
                                    updated.getStatus(), message))
                            .thenApply(ignored ->
                                    ScreenshareOperationResult.success(updated,
                                            successKey));
                });
    }

    private ScreenshareEvent event(ScreenshareSession session,
                                   ScreenshareEventType type, UUID actorId,
                                   String actorName,
                                   ScreenshareStatus previous,
                                   ScreenshareStatus current, String message) {
        String author = actorName == null || actorName.trim().isEmpty()
                ? "system" : actorName;
        return new ScreenshareEvent(UUID.randomUUID(), session.getId(), actorId,
                author, type, previous, current, message,
                configuration.getProxyId(), clock.get());
    }

    private Optional<ReportId> reportIdOf(ScreenshareSession session) {
        return session.getReportId().map(ReportId::of);
    }

    /**
     * Invia una riga localizzata a un giocatore, se e' collegato.
     */
    private void notify(UUID playerId, String key,
                        ScreenshareSession session) {
        Optional<OnlinePlayer> player = directory.findById(playerId);
        if (!player.isPresent()) {
            return;
        }
        Language language = languages.resolve(playerId);
        PlaceholderValues placeholders = presenter.base(language, session);
        player.get().send(presenter.line(language, key, placeholders));
    }

    private void sendAll(UUID playerId, List<ChatLine> lines) {
        Optional<OnlinePlayer> player = directory.findById(playerId);
        if (!player.isPresent()) {
            return;
        }
        for (ChatLine line : new ArrayList<>(lines)) {
            player.get().send(line);
        }
    }

    private static <T> CompletableFuture<T> done(T value) {
        return CompletableFuture.completedFuture(value);
    }

    private static ScreenshareOperationResult toRepositoryError(
            Throwable failure) {
        return ScreenshareOperationResult.failure(
                ScreenshareOperationStatus.REPOSITORY_ERROR);
    }
}
