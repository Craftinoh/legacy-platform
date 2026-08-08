package it.legacynetwork.screenshare.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.screenshare.model.ScreenshareEvent;
import it.legacynetwork.screenshare.model.ScreenshareOutcome;
import it.legacynetwork.screenshare.model.ScreenshareSession;
import it.legacynetwork.screenshare.repository.ScreensharePage;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Costruisce le righe mostrate a staff e bersaglio.
 *
 * <p>Ogni testo esce da {@link ScreenshareMessages}: qui si sceglie solo quale
 * chiave comporre e quali segmenti rendere cliccabili.</p>
 */
public final class ScreensharePresenter {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneOffset.UTC);

    private final ScreenshareMessages messages;

    public ScreensharePresenter(ScreenshareMessages messages) {
        if (messages == null) {
            throw new IllegalArgumentException(
                    "Presentazione screenshare incompleta");
        }
        this.messages = messages;
    }

    public ScreenshareMessages getMessages() {
        return messages;
    }

    public ChatLine line(Language language, String key) {
        return ChatLine.text(messages.get(language, key));
    }

    public ChatLine line(Language language, String key,
                         PlaceholderValues placeholders) {
        return ChatLine.text(messages.get(language, key, placeholders));
    }

    /**
     * Riepilogo dei comandi staff.
     */
    public List<ChatLine> usage(Language language) {
        List<ChatLine> lines = new ArrayList<>();
        for (String key : new String[]{"screenshare.command.header",
                "screenshare.command.start", "screenshare.command.stop",
                "screenshare.command.cancel", "screenshare.command.status",
                "screenshare.command.list", "screenshare.command.note"}) {
            lines.add(line(language, key));
        }
        return lines;
    }

    /**
     * Istruzioni mostrate al bersaglio quando il controllo comincia.
     */
    public List<ChatLine> targetInstructions(Language language,
                                             ScreenshareSession session) {
        List<ChatLine> lines = new ArrayList<>();
        lines.add(line(language, "screenshare.target.session-started",
                base(language, session)));
        lines.add(line(language, "screenshare.target.instructions",
                base(language, session)));
        return lines;
    }

    /**
     * Elenco paginato delle sessioni.
     */
    public List<ChatLine> list(Language language, ScreensharePage page) {
        List<ChatLine> lines = new ArrayList<>();
        lines.add(line(language, "screenshare.list.header",
                pagePlaceholders(page)));
        if (page.isEmpty()) {
            lines.add(line(language, "screenshare.list.empty"));
            return lines;
        }
        for (ScreenshareSession session : page.getItems()) {
            lines.add(entry(language, session));
        }
        lines.add(navigation(language, page));
        return lines;
    }

    /**
     * Scheda di una sessione, storico compreso.
     */
    public List<ChatLine> status(Language language, ScreenshareSession session,
                                 List<ScreenshareEvent> history) {
        List<ChatLine> lines = new ArrayList<>();
        PlaceholderValues placeholders = base(language, session);
        lines.add(line(language, "screenshare.info.header", placeholders));
        lines.add(line(language, "screenshare.info.target", placeholders));
        lines.add(line(language, "screenshare.info.staff", placeholders));
        lines.add(line(language, "screenshare.info.server", placeholders));
        lines.add(line(language, "screenshare.info.status", placeholders));
        lines.add(line(language, "screenshare.info.report", placeholders));
        lines.add(line(language, "screenshare.info.created", placeholders));
        lines.add(line(language, "screenshare.info.expires", placeholders));
        if (session.getStartedAt().isPresent()) {
            lines.add(line(language, "screenshare.info.started", placeholders));
        }
        if (session.getEndedAt().isPresent()) {
            lines.add(line(language, "screenshare.info.ended", placeholders));
        }
        if (session.getOutcome().isPresent()) {
            lines.add(line(language, "screenshare.info.outcome", placeholders));
        }
        lines.add(line(language, "screenshare.info.notes", placeholders));
        lines.add(line(language, "screenshare.info.history-header"));
        if (history == null || history.isEmpty()) {
            lines.add(line(language, "screenshare.info.history-empty"));
            return lines;
        }
        for (ScreenshareEvent event : history) {
            lines.add(line(language, "screenshare.info.history-entry",
                    PlaceholderValues.builder()
                            .put("time", TIMESTAMP.format(event.getCreatedAt()))
                            .put("actor", event.getActorName())
                            .put("event", messages.get(language,
                                    event.getType().messageKey()))
                            .put("message", event.getMessage().orElse(""))
                            .build()));
        }
        return lines;
    }

    private ChatLine entry(Language language, ScreenshareSession session) {
        PlaceholderValues placeholders = base(language, session);
        List<ChatSegment> segments = new ArrayList<>();
        segments.add(ChatSegment.text(messages.get(language,
                "screenshare.list.entry", placeholders)));
        segments.add(ChatSegment.text(" "));
        segments.add(ChatSegment.run(
                messages.get(language, "screenshare.button.status",
                        placeholders),
                messages.get(language, "screenshare.button.status.hover",
                        placeholders),
                "/ss status " + session.getTargetName()));
        segments.add(ChatSegment.text(" "));
        segments.add(ChatSegment.suggest(
                messages.get(language, "screenshare.button.stop",
                        placeholders),
                messages.get(language, "screenshare.button.stop.hover",
                        placeholders),
                "/ss stop " + session.getTargetName() + " CLEAN"));
        return ChatLine.of(segments);
    }

    private ChatLine navigation(Language language, ScreensharePage page) {
        List<ChatSegment> segments = new ArrayList<>();
        segments.add(ChatSegment.text(messages.get(language,
                "screenshare.list.footer", pagePlaceholders(page))));
        if (page.hasPreviousPage()) {
            segments.add(ChatSegment.text(" "));
            segments.add(ChatSegment.run(
                    messages.get(language, "screenshare.button.previous"),
                    messages.get(language, "screenshare.button.previous.hover"),
                    "/ss list " + (page.getPage() - 1)));
        }
        if (page.hasNextPage()) {
            segments.add(ChatSegment.text(" "));
            segments.add(ChatSegment.run(
                    messages.get(language, "screenshare.button.next"),
                    messages.get(language, "screenshare.button.next.hover"),
                    "/ss list " + (page.getPage() + 1)));
        }
        return ChatLine.of(segments);
    }

    private static PlaceholderValues pagePlaceholders(ScreensharePage page) {
        return PlaceholderValues.builder()
                .put("page", page.getPage())
                .put("pages", page.getTotalPages())
                .put("total", page.getTotalItems())
                .build();
    }

    /**
     * Segnaposto comuni a tutte le righe che descrivono una sessione.
     */
    public PlaceholderValues base(Language language,
                                  ScreenshareSession session) {
        return PlaceholderValues.builder()
                .put("id", session.getId().shortCode())
                .put("full-id", session.getId().toString())
                .put("target", session.getTargetName())
                .put("staff", session.getStaffName())
                .put("server", session.getServerId())
                .put("status", messages.get(language,
                        session.getStatus().messageKey()))
                .put("outcome", session.getOutcome()
                        .map(ScreenshareOutcome::messageKey)
                        .map(key -> messages.get(language, key))
                        .orElse(""))
                .put("report", session.getReportId()
                        .map(id -> id.toString().replace("-", "")
                                .substring(0, 8))
                        .orElse(messages.get(language,
                                "screenshare.info.report-none")))
                .put("notes", session.getNotes().orElse(messages.get(language,
                        "screenshare.info.notes-none")))
                .put("created", TIMESTAMP.format(session.getCreatedAt()))
                .put("started", session.getStartedAt()
                        .map(TIMESTAMP::format).orElse(""))
                .put("expires", TIMESTAMP.format(session.getExpiresAt()))
                .put("ended", session.getEndedAt()
                        .map(TIMESTAMP::format).orElse(""))
                .build();
    }

    /**
     * Formatta un istante nel fuso usato dai messaggi.
     */
    public static String format(Instant instant) {
        return TIMESTAMP.format(instant);
    }
}
