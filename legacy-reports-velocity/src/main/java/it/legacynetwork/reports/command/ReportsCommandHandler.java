package it.legacynetwork.reports.command;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.reports.api.ReportOperationResult;
import it.legacynetwork.reports.config.ReportsConfiguration;
import it.legacynetwork.reports.message.ChatLine;
import it.legacynetwork.reports.message.ReportLanguageResolver;
import it.legacynetwork.reports.message.ReportPresenter;
import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.notification.StaffNotificationPreferences;
import it.legacynetwork.reports.platform.CommandActor;
import it.legacynetwork.reports.platform.OnlinePlayer;
import it.legacynetwork.reports.platform.PlayerDirectory;
import it.legacynetwork.reports.repository.ReportPage;
import it.legacynetwork.reports.service.ReportService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Logica dei comandi staff {@code /reports ...}.
 *
 * <p>L'adapter Velocity non decide nulla: qui si verificano permessi, argomenti
 * e disponibilita' del report, mentre ogni cambio di stato passa da
 * {@link ReportService}. L'interfaccia e' testuale — righe, pulsanti cliccabili
 * e paginazione — perche' sul proxy non esiste alcun inventario.</p>
 */
public final class ReportsCommandHandler {

    private static final List<String> SUBCOMMANDS = Collections.unmodifiableList(
            Arrays.asList("list", "info", "claim", "investigate", "release",
                    "dismiss", "action", "player", "notifications"));

    private final ReportsConfiguration configuration;
    private final ReportService service;
    private final PlayerDirectory directory;
    private final ReportPresenter presenter;
    private final ReportLanguageResolver languages;
    private final StaffNotificationPreferences preferences;

    public ReportsCommandHandler(ReportsConfiguration configuration,
                                 ReportService service,
                                 PlayerDirectory directory,
                                 ReportPresenter presenter,
                                 ReportLanguageResolver languages,
                                 StaffNotificationPreferences preferences) {
        if (configuration == null || service == null || directory == null
                || presenter == null || languages == null
                || preferences == null) {
            throw new IllegalArgumentException("Comandi staff incompleti");
        }
        this.configuration = configuration;
        this.service = service;
        this.directory = directory;
        this.presenter = presenter;
        this.languages = languages;
        this.preferences = preferences;
    }

    public CompletableFuture<Void> execute(CommandActor actor,
                                           String[] arguments) {
        Language language = languages.resolve(actor.uniqueId());
        if (!permitted(actor, configuration.getPermissions().getStaffView())) {
            return fail(actor, language, "reports.error.no-permission");
        }
        if (arguments.length == 0) {
            return usage(actor, language);
        }

        String subcommand = arguments[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "list":
                return list(actor, language, arguments);
            case "info":
                return info(actor, language, arguments);
            case "claim":
                return transition(actor, language, arguments,
                        configuration.getPermissions().getStaffClaim(),
                        (report, staff) -> service.claim(report.getId(),
                                staff.uniqueId(), staff.name()));
            case "investigate":
                return transition(actor, language, arguments,
                        configuration.getPermissions().getStaffClaim(),
                        (report, staff) -> service.investigate(report.getId(),
                                staff.uniqueId(), staff.name()));
            case "release":
                return transition(actor, language, arguments,
                        configuration.getPermissions().getStaffClaim(),
                        (report, staff) -> service.release(report.getId(),
                                staff.uniqueId(), staff.name(),
                                staff.hasPermission(configuration
                                        .getPermissions().getAdmin())));
            case "dismiss":
                return resolve(actor, language, arguments, false);
            case "action":
                return resolve(actor, language, arguments, true);
            case "player":
                return player(actor, language, arguments);
            case "notifications":
                return notifications(actor, language);
            default:
                return usage(actor, language);
        }
    }

    public List<String> suggest(CommandActor actor, String[] arguments) {
        if (!permitted(actor, configuration.getPermissions().getStaffView())) {
            return Collections.emptyList();
        }
        if (arguments.length <= 1) {
            return filter(SUBCOMMANDS, arguments.length == 0 ? ""
                    : arguments[0]);
        }
        if (arguments.length == 2
                && "player".equalsIgnoreCase(arguments[0])) {
            return filter(directory.names(), arguments[1]);
        }
        return Collections.emptyList();
    }

    // ------------------------------------------------------------ sottocomandi

    private CompletableFuture<Void> list(CommandActor actor, Language language,
                                         String[] arguments) {
        Optional<Integer> page = page(arguments, 1);
        if (!page.isPresent()) {
            return fail(actor, language, "reports.error.invalid-page");
        }
        return service.listActive(page.get(), configuration.getPageSize())
                .thenAccept(result -> send(actor,
                        presenter.list(language, result)))
                .exceptionally(failure -> {
                    actor.send(presenter.line(language,
                            "reports.error.repository-error"));
                    return null;
                });
    }

    private CompletableFuture<Void> player(CommandActor actor,
                                           Language language,
                                           String[] arguments) {
        if (!permitted(actor,
                configuration.getPermissions().getStaffHistory())) {
            return fail(actor, language, "reports.error.no-permission");
        }
        if (arguments.length < 2) {
            return usage(actor, language);
        }
        Optional<Integer> page = page(arguments, 2);
        if (!page.isPresent()) {
            return fail(actor, language, "reports.error.invalid-page");
        }
        Optional<OnlinePlayer> target = directory.findByName(arguments[1]);
        if (!target.isPresent()) {
            return fail(actor, language, "reports.error.target-not-found",
                    PlaceholderValues.builder()
                            .put("target", arguments[1])
                            .build());
        }
        OnlinePlayer found = target.get();
        return service.listByTarget(found.uniqueId(), page.get(),
                        configuration.getPageSize())
                .thenAccept(result -> send(actor, presenter.playerHistory(
                        language, found.name(), result)))
                .exceptionally(failure -> {
                    actor.send(presenter.line(language,
                            "reports.error.repository-error"));
                    return null;
                });
    }

    private CompletableFuture<Void> info(CommandActor actor, Language language,
                                         String[] arguments) {
        if (arguments.length < 2) {
            return usage(actor, language);
        }
        return withReport(actor, language, arguments[1], report ->
                service.history(report.getId(),
                                configuration.getHistorySize())
                        .thenAccept(history -> send(actor,
                                presenter.info(language, report, history))));
    }

    private CompletableFuture<Void> transition(
            CommandActor actor, Language language, String[] arguments,
            String permission,
            BiFunction<Report, CommandActor,
                    CompletableFuture<ReportOperationResult>> operation) {
        if (!permitted(actor, permission)) {
            return fail(actor, language, "reports.error.no-permission");
        }
        if (!actor.isPlayer()) {
            return fail(actor, language, "reports.error.player-only");
        }
        if (arguments.length < 2) {
            return usage(actor, language);
        }
        return withReport(actor, language, arguments[1], report ->
                operation.apply(report, actor)
                        .thenAccept(result -> show(actor, language, result,
                                report)));
    }

    private CompletableFuture<Void> resolve(CommandActor actor,
                                            Language language,
                                            String[] arguments,
                                            boolean actionTaken) {
        if (!permitted(actor,
                configuration.getPermissions().getStaffResolve())) {
            return fail(actor, language, "reports.error.no-permission");
        }
        if (!actor.isPlayer()) {
            return fail(actor, language, "reports.error.player-only");
        }
        if (arguments.length < 3) {
            return usage(actor, language);
        }
        String reason = join(arguments, 2);
        if (reason.isEmpty()) {
            return fail(actor, language, "reports.error.reason-required");
        }
        int maxLength = configuration.getDetailsMaxLength();
        if (maxLength > 0 && reason.length() > maxLength) {
            return fail(actor, language, "reports.error.details-too-long",
                    PlaceholderValues.builder()
                            .put("limit", maxLength)
                            .build());
        }
        UUID staffId = actor.uniqueId();
        return withReport(actor, language, arguments[1], report -> {
            CompletableFuture<ReportOperationResult> operation = actionTaken
                    ? service.actionTaken(report.getId(), staffId,
                            actor.name(), reason)
                    : service.dismiss(report.getId(), staffId, actor.name(),
                            reason);
            return operation.thenAccept(
                    result -> show(actor, language, result, report));
        });
    }

    private CompletableFuture<Void> notifications(CommandActor actor,
                                                  Language language) {
        if (!actor.isPlayer()) {
            return fail(actor, language, "reports.error.player-only");
        }
        boolean enabled = preferences.toggle(actor.uniqueId());
        return fail(actor, language, enabled
                ? "reports.success.notifications-on"
                : "reports.success.notifications-off");
    }

    // ------------------------------------------------------------- infrastrutt.

    private CompletableFuture<Void> withReport(
            CommandActor actor, Language language, String reference,
            java.util.function.Function<Report,
                    CompletableFuture<Void>> action) {
        return service.findByReference(reference)
                .thenCompose(found -> {
                    if (!found.isPresent()) {
                        return fail(actor, language, "reports.error.not-found",
                                PlaceholderValues.builder()
                                        .put("id", reference)
                                        .build());
                    }
                    return action.apply(found.get());
                })
                .exceptionally(failure -> {
                    actor.send(presenter.line(language,
                            "reports.error.repository-error"));
                    return null;
                });
    }

    private void show(CommandActor actor, Language language,
                      ReportOperationResult result, Report previous) {
        Report report = result.getReport().orElse(previous);
        actor.send(presenter.line(language, result.getMessageKey(),
                presenter.base(language, report)));
    }

    private void send(CommandActor actor, List<ChatLine> lines) {
        for (ChatLine line : lines) {
            actor.send(line);
        }
    }

    private CompletableFuture<Void> usage(CommandActor actor,
                                          Language language) {
        send(actor, presenter.staffUsage(language));
        return CompletableFuture.completedFuture(null);
    }

    private boolean permitted(CommandActor actor, String node) {
        return actor.hasPermission(node)
                || actor.hasPermission(configuration.getPermissions()
                        .getAdmin());
    }

    private static Optional<Integer> page(String[] arguments, int index) {
        if (arguments.length <= index) {
            return Optional.of(1);
        }
        try {
            int page = Integer.parseInt(arguments[index].trim());
            return page < 1 ? Optional.empty() : Optional.of(page);
        } catch (NumberFormatException invalid) {
            return Optional.empty();
        }
    }

    private static String join(String[] arguments, int from) {
        StringBuilder builder = new StringBuilder();
        for (int index = from; index < arguments.length; index++) {
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
        return CompletableFuture.completedFuture(null);
    }
}
