package it.legacynetwork.screenshare.command;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.screenshare.config.ScreenshareConfiguration;
import it.legacynetwork.screenshare.message.ChatLine;
import it.legacynetwork.screenshare.message.ScreenshareLanguageResolver;
import it.legacynetwork.screenshare.message.ScreensharePresenter;
import it.legacynetwork.screenshare.model.ScreenshareOutcome;
import it.legacynetwork.screenshare.model.ScreenshareSession;
import it.legacynetwork.screenshare.platform.CommandActor;
import it.legacynetwork.screenshare.platform.OnlinePlayer;
import it.legacynetwork.screenshare.platform.PlayerDirectory;
import it.legacynetwork.screenshare.service.ScreenshareOperationResult;
import it.legacynetwork.screenshare.service.ScreenshareService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Logica di {@code /ss}.
 *
 * <p>Nessuna dipendenza da Velocity: l'adapter passa un {@link CommandActor} e
 * un {@link PlayerDirectory}, quindi ogni ramo e' verificabile in un test. Le
 * transizioni restano tutte in {@link ScreenshareService}.</p>
 */
public final class ScreenshareCommandHandler {

    private static final List<String> SUBCOMMANDS =
            Collections.unmodifiableList(Arrays.asList("start", "stop",
                    "cancel", "status", "list", "note"));

    private final ScreenshareConfiguration configuration;
    private final ScreenshareService service;
    private final PlayerDirectory directory;
    private final ScreensharePresenter presenter;
    private final ScreenshareLanguageResolver languages;

    public ScreenshareCommandHandler(ScreenshareConfiguration configuration,
                                     ScreenshareService service,
                                     PlayerDirectory directory,
                                     ScreensharePresenter presenter,
                                     ScreenshareLanguageResolver languages) {
        if (configuration == null || service == null || directory == null
                || presenter == null || languages == null) {
            throw new IllegalArgumentException(
                    "Comando screenshare incompleto");
        }
        this.configuration = configuration;
        this.service = service;
        this.directory = directory;
        this.presenter = presenter;
        this.languages = languages;
    }

    public CompletableFuture<Void> execute(CommandActor actor,
                                           String[] arguments) {
        Language language = languages.resolve(actor.uniqueId());
        if (!permitted(actor, configuration.getPermissions().getView())) {
            return fail(actor, language, "screenshare.error.no-permission");
        }
        if (arguments.length == 0) {
            return usage(actor, language);
        }

        switch (arguments[0].toLowerCase(Locale.ROOT)) {
            case "start":
                return start(actor, language, arguments);
            case "stop":
                return stop(actor, language, arguments);
            case "cancel":
                return cancel(actor, language, arguments);
            case "status":
                return status(actor, language, arguments);
            case "list":
                return list(actor, language, arguments);
            case "note":
                return note(actor, language, arguments);
            default:
                return usage(actor, language);
        }
    }

    public List<String> suggest(CommandActor actor, String[] arguments) {
        if (!permitted(actor, configuration.getPermissions().getView())) {
            return Collections.emptyList();
        }
        if (arguments.length <= 1) {
            return filter(SUBCOMMANDS,
                    arguments.length == 0 ? "" : arguments[0]);
        }
        if (arguments.length == 2 && !"list".equalsIgnoreCase(arguments[0])) {
            return filter(directory.names(), arguments[1]);
        }
        if (arguments.length == 3 && "stop".equalsIgnoreCase(arguments[0])) {
            List<String> outcomes = new ArrayList<>();
            for (ScreenshareOutcome outcome : ScreenshareOutcome.selectable()) {
                outcomes.add(outcome.name());
            }
            return filter(outcomes, arguments[2]);
        }
        return Collections.emptyList();
    }

    // ------------------------------------------------------------ sottocomandi

    private CompletableFuture<Void> start(CommandActor actor, Language language,
                                          String[] arguments) {
        if (!permitted(actor, configuration.getPermissions().getStart())) {
            return fail(actor, language, "screenshare.error.no-permission");
        }
        if (!actor.isPlayer()) {
            return fail(actor, language, "screenshare.error.player-only");
        }
        if (arguments.length < 2) {
            return usage(actor, language);
        }
        Optional<OnlinePlayer> staff = directory.findById(actor.uniqueId());
        if (!staff.isPresent()) {
            return fail(actor, language, "screenshare.error.player-only");
        }
        String reportReference = arguments.length >= 3 ? arguments[2] : null;
        return service.start(staff.get(), arguments[1], reportReference)
                .thenAccept(result -> show(actor, language, result,
                        arguments[1]));
    }

    private CompletableFuture<Void> stop(CommandActor actor, Language language,
                                         String[] arguments) {
        if (!permitted(actor, configuration.getPermissions().getStop())) {
            return fail(actor, language, "screenshare.error.no-permission");
        }
        if (!actor.isPlayer()) {
            return fail(actor, language, "screenshare.error.player-only");
        }
        if (arguments.length < 3) {
            return usage(actor, language);
        }
        Optional<ScreenshareOutcome> outcome =
                ScreenshareOutcome.parse(arguments[2]);
        if (!outcome.isPresent()
                || !ScreenshareOutcome.selectable().contains(outcome.get())) {
            return fail(actor, language, "screenshare.error.invalid-outcome",
                    PlaceholderValues.builder()
                            .put("outcomes", selectableOutcomes())
                            .build());
        }
        return service.stop(actor.uniqueId(), actor.name(), arguments[1],
                        outcome.get(), isAdmin(actor))
                .thenAccept(result -> show(actor, language, result,
                        arguments[1]));
    }

    private CompletableFuture<Void> cancel(CommandActor actor,
                                           Language language,
                                           String[] arguments) {
        if (!permitted(actor, configuration.getPermissions().getStop())) {
            return fail(actor, language, "screenshare.error.no-permission");
        }
        if (!actor.isPlayer()) {
            return fail(actor, language, "screenshare.error.player-only");
        }
        if (arguments.length < 3) {
            return usage(actor, language);
        }
        String reason = join(arguments, 2);
        if (reason.isEmpty()) {
            return fail(actor, language, "screenshare.error.reason-required");
        }
        if (tooLong(reason)) {
            return fail(actor, language, "screenshare.error.note-too-long",
                    PlaceholderValues.builder()
                            .put("limit", configuration.getNoteMaxLength())
                            .build());
        }
        return service.cancel(actor.uniqueId(), actor.name(), arguments[1],
                        reason, isAdmin(actor))
                .thenAccept(result -> show(actor, language, result,
                        arguments[1]));
    }

    private CompletableFuture<Void> note(CommandActor actor, Language language,
                                         String[] arguments) {
        if (!permitted(actor, configuration.getPermissions().getNote())) {
            return fail(actor, language, "screenshare.error.no-permission");
        }
        if (!actor.isPlayer()) {
            return fail(actor, language, "screenshare.error.player-only");
        }
        if (arguments.length < 3) {
            return usage(actor, language);
        }
        String note = join(arguments, 2);
        if (note.isEmpty()) {
            return fail(actor, language, "screenshare.error.note-required");
        }
        if (tooLong(note)) {
            return fail(actor, language, "screenshare.error.note-too-long",
                    PlaceholderValues.builder()
                            .put("limit", configuration.getNoteMaxLength())
                            .build());
        }
        return service.note(actor.uniqueId(), actor.name(), arguments[1], note)
                .thenAccept(result -> show(actor, language, result,
                        arguments[1]));
    }

    private CompletableFuture<Void> status(CommandActor actor,
                                           Language language,
                                           String[] arguments) {
        String targetName = arguments.length >= 2 ? arguments[1] : actor.name();
        return service.findOpenByName(targetName)
                .thenCompose(found -> {
                    if (!found.isPresent()) {
                        return fail(actor, language,
                                "screenshare.error.no-session",
                                PlaceholderValues.builder()
                                        .put("target", targetName)
                                        .build());
                    }
                    ScreenshareSession session = found.get();
                    return service.history(session.getId(),
                                    configuration.getHistorySize())
                            .thenAccept(history -> send(actor,
                                    presenter.status(language, session,
                                            history)));
                })
                .exceptionally(failure -> {
                    actor.send(presenter.line(language,
                            "screenshare.error.repository-error"));
                    return null;
                });
    }

    private CompletableFuture<Void> list(CommandActor actor, Language language,
                                         String[] arguments) {
        int page = 1;
        if (arguments.length >= 2) {
            try {
                page = Integer.parseInt(arguments[1].trim());
            } catch (NumberFormatException invalid) {
                return fail(actor, language, "screenshare.error.invalid-page");
            }
            if (page < 1) {
                return fail(actor, language, "screenshare.error.invalid-page");
            }
        }
        return service.list(page, configuration.getPageSize())
                .thenAccept(result -> send(actor,
                        presenter.list(language, result)))
                .exceptionally(failure -> {
                    actor.send(presenter.line(language,
                            "screenshare.error.repository-error"));
                    return null;
                });
    }

    // ------------------------------------------------------------- infrastr.

    private void show(CommandActor actor, Language language,
                      ScreenshareOperationResult result, String targetName) {
        Optional<ScreenshareSession> session = result.getSession();
        PlaceholderValues placeholders = session
                .map(value -> presenter.base(language, value))
                .orElseGet(() -> PlaceholderValues.builder()
                        .put("target", targetName)
                        .build());
        actor.send(presenter.line(language, result.getMessageKey(),
                placeholders));
    }

    private String selectableOutcomes() {
        StringBuilder builder = new StringBuilder();
        for (ScreenshareOutcome outcome : ScreenshareOutcome.selectable()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(outcome.name());
        }
        return builder.toString();
    }

    private boolean tooLong(String text) {
        int limit = configuration.getNoteMaxLength();
        return limit > 0 && text.length() > limit;
    }

    private boolean isAdmin(CommandActor actor) {
        return actor.hasPermission(configuration.getPermissions().getAdmin());
    }

    private boolean permitted(CommandActor actor, String node) {
        return actor.hasPermission(node) || isAdmin(actor);
    }

    private CompletableFuture<Void> usage(CommandActor actor,
                                          Language language) {
        send(actor, presenter.usage(language));
        return CompletableFuture.completedFuture(null);
    }

    private void send(CommandActor actor, List<ChatLine> lines) {
        for (ChatLine line : lines) {
            actor.send(line);
        }
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
}
