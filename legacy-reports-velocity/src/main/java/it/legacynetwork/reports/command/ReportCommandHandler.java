package it.legacynetwork.reports.command;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.reports.config.ReportsConfiguration;
import it.legacynetwork.reports.message.ChatLine;
import it.legacynetwork.reports.message.ReportLanguageResolver;
import it.legacynetwork.reports.message.ReportPresenter;
import it.legacynetwork.reports.api.Report;
import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.model.ReportReason;
import it.legacynetwork.reports.api.ReportSnapshot;
import it.legacynetwork.reports.api.ReportStatus;
import it.legacynetwork.reports.notification.StaffNotificationService;
import it.legacynetwork.reports.platform.CommandActor;
import it.legacynetwork.reports.platform.OnlinePlayer;
import it.legacynetwork.reports.platform.PlayerDirectory;
import it.legacynetwork.reports.service.ReportService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Logica di {@code /report <player> <reason> [details]}.
 *
 * <p>Nessuna dipendenza da Velocity: l'adapter passa un {@link CommandActor} e
 * un {@link PlayerDirectory}, quindi ogni ramo — console, bersaglio assente,
 * auto-segnalazione, motivo sconosciuto, attesa, duplicato, limite, errore dello
 * storage — e' verificabile in un test.</p>
 */
public final class ReportCommandHandler {

    private final ReportsConfiguration configuration;
    private final ReportService service;
    private final PlayerDirectory directory;
    private final ReportPresenter presenter;
    private final ReportLanguageResolver languages;
    private final StaffNotificationService notifications;
    private final CooldownRegistry cooldowns;
    private final Supplier<Instant> clock;

    public ReportCommandHandler(ReportsConfiguration configuration,
                                ReportService service,
                                PlayerDirectory directory,
                                ReportPresenter presenter,
                                ReportLanguageResolver languages,
                                StaffNotificationService notifications,
                                CooldownRegistry cooldowns,
                                Supplier<Instant> clock) {
        if (configuration == null || service == null || directory == null
                || presenter == null || languages == null
                || notifications == null || cooldowns == null
                || clock == null) {
            throw new IllegalArgumentException("Comando report incompleto");
        }
        this.configuration = configuration;
        this.service = service;
        this.directory = directory;
        this.presenter = presenter;
        this.languages = languages;
        this.notifications = notifications;
        this.cooldowns = cooldowns;
        this.clock = clock;
    }

    /**
     * Esegue il comando.
     *
     * @return un future completato quando l'esito e' stato mostrato
     */
    public CompletableFuture<Void> execute(CommandActor actor,
                                           String[] arguments) {
        Language language = languages.resolve(actor.uniqueId());

        if (!actor.isPlayer()) {
            return fail(actor, language, "reports.error.player-only");
        }
        if (!actor.hasPermission(
                configuration.getPermissions().getReport())) {
            return fail(actor, language, "reports.error.no-permission");
        }
        if (configuration.getReasons().isEmpty()) {
            return fail(actor, language, "reports.error.reasons-unavailable");
        }
        if (arguments.length < 2) {
            for (ChatLine line : presenter.reportUsage(language)) {
                actor.send(line);
            }
            return done();
        }

        Optional<ReportReason> reason =
                configuration.getReasons().find(arguments[1]);
        if (!reason.isPresent()) {
            actor.send(presenter.line(language, "reports.error.invalid-reason",
                    PlaceholderValues.builder()
                            .put("reasons", availableReasons())
                            .build()));
            return done();
        }

        Optional<OnlinePlayer> target = directory.findByName(arguments[0]);
        if (!target.isPresent()) {
            return fail(actor, language, "reports.error.target-not-found",
                    PlaceholderValues.builder()
                            .put("target", arguments[0])
                            .build());
        }
        OnlinePlayer victim = target.get();
        if (victim.uniqueId().equals(actor.uniqueId())) {
            return fail(actor, language, "reports.error.self-report");
        }
        if (configuration.isProtectStaff() && victim.hasPermission(
                configuration.getPermissions().getProtectedTarget())) {
            return fail(actor, language, "reports.error.protected-target",
                    PlaceholderValues.builder()
                            .put("target", victim.name())
                            .build());
        }

        String details = joinDetails(arguments);
        if (reason.get().isRequireDetails() && details.isEmpty()) {
            return fail(actor, language, "reports.error.details-required");
        }
        if (!reason.get().isAllowDetails()) {
            details = "";
        }
        int maxLength = configuration.getDetailsMaxLength();
        if (maxLength > 0 && details.length() > maxLength) {
            return fail(actor, language, "reports.error.details-too-long",
                    PlaceholderValues.builder()
                            .put("limit", maxLength)
                            .build());
        }

        Instant now = clock.get();
        long waiting = cooldowns.remainingSeconds(actor.uniqueId(), now,
                configuration.getCooldown());
        if (waiting > 0L) {
            return fail(actor, language, "reports.error.cooldown",
                    PlaceholderValues.builder()
                            .put("seconds", waiting)
                            .build());
        }

        String storedDetails = details;
        return service.countActiveByReporter(actor.uniqueId())
                .thenCompose(open -> {
                    int limit = configuration.getMaxOpenPerReporter();
                    if (limit > 0 && open >= limit) {
                        return fail(actor, language,
                                "reports.error.too-many-open",
                                PlaceholderValues.builder()
                                        .put("limit", limit)
                                        .build());
                    }
                    return service.findRecentDuplicate(actor.uniqueId(),
                                    victim.uniqueId(),
                                    now.minus(configuration
                                            .getDuplicateWindow()))
                            .thenCompose(duplicate -> {
                                if (duplicate.isPresent()) {
                                    return fail(actor, language,
                                            "reports.error.duplicate",
                                            PlaceholderValues.builder()
                                                    .put("id", duplicate.get()
                                                            .getId()
                                                            .shortCode())
                                                    .build());
                                }
                                return create(actor, language, victim,
                                        reason.get(), storedDetails, now);
                            });
                })
                .exceptionally(failure -> {
                    actor.send(presenter.line(language,
                            "reports.error.repository-error"));
                    return null;
                });
    }

    /**
     * Suggerimenti del comando: nomi collegati, poi motivi attivi.
     */
    public List<String> suggest(CommandActor actor, String[] arguments) {
        if (!actor.hasPermission(configuration.getPermissions().getReport())) {
            return Collections.emptyList();
        }
        if (arguments.length <= 1) {
            return filter(directory.names(),
                    arguments.length == 0 ? "" : arguments[0]);
        }
        if (arguments.length == 2) {
            List<String> ids = new ArrayList<>();
            for (ReportReason reason : configuration.getReasons().enabled()) {
                ids.add(reason.getId());
            }
            return filter(ids, arguments[1]);
        }
        return Collections.emptyList();
    }

    private CompletableFuture<Void> create(CommandActor actor,
                                           Language language,
                                           OnlinePlayer victim,
                                           ReportReason reason,
                                           String details, Instant now) {
        // Fra il controllo e la scrittura il bersaglio puo' essersi scollegato:
        // meglio dirlo che registrare una segnalazione su un giocatore assente.
        Optional<OnlinePlayer> current = directory.findById(victim.uniqueId());
        if (!current.isPresent()) {
            return fail(actor, language, "reports.error.target-offline",
                    PlaceholderValues.builder()
                            .put("target", victim.name())
                            .build());
        }
        OnlinePlayer online = current.get();
        Report report = Report.builder()
                .id(ReportId.random())
                .reporter(actor.uniqueId(), actor.name())
                .target(online.uniqueId(), online.name())
                .reasonId(reason.getId())
                .details(details)
                .snapshot(new ReportSnapshot(online.serverId(),
                        online.pingMillis(), configuration.getProxyId(), now))
                .createdAt(now)
                .updatedAt(now)
                .status(ReportStatus.OPEN)
                .revision(0L)
                .build();

        return service.create(report).thenAccept(result -> {
            if (!result.isApplied()) {
                actor.send(presenter.line(language, result.getMessageKey()));
                return;
            }
            Report stored = result.getReport().orElse(report);
            cooldowns.record(actor.uniqueId(), now);
            actor.send(presenter.line(language, result.getMessageKey(),
                    presenter.base(language, stored)));
            notifications.notifyCreated(stored);
        });
    }

    private String availableReasons() {
        StringBuilder builder = new StringBuilder();
        for (ReportReason reason : configuration.getReasons().enabled()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(reason.getId());
        }
        return builder.toString();
    }

    private static String joinDetails(String[] arguments) {
        StringBuilder builder = new StringBuilder();
        for (int index = 2; index < arguments.length; index++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(arguments[index]);
        }
        return builder.toString().trim();
    }

    private static List<String> filter(List<String> candidates, String prefix) {
        String normalized = prefix == null ? ""
                : prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    private CompletableFuture<Void> fail(CommandActor actor, Language language,
                                         String key) {
        return fail(actor, language, key, PlaceholderValues.empty());
    }

    private CompletableFuture<Void> fail(CommandActor actor, Language language,
                                         String key,
                                         PlaceholderValues placeholders) {
        actor.send(presenter.line(language, key, placeholders));
        return done();
    }

    private static CompletableFuture<Void> done() {
        return CompletableFuture.completedFuture(null);
    }
}
