package it.legacynetwork.reports.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.reports.config.ReportReasonCatalog;
import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.model.ReportEvent;
import it.legacynetwork.reports.repository.ReportPage;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Costruisce le righe mostrate allo staff.
 *
 * <p>Ogni testo esce da {@link ReportMessages}: qui si decide soltanto quali
 * chiavi comporre, con quali segnaposto e quali segmenti rendere cliccabili.
 * L'interfaccia di questo plugin e' fatta di chat, non di inventari.</p>
 */
public final class ReportPresenter {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneOffset.UTC);

    private final ReportMessages messages;
    private final ReportReasonCatalog reasons;

    public ReportPresenter(ReportMessages messages,
                           ReportReasonCatalog reasons) {
        if (messages == null || reasons == null) {
            throw new IllegalArgumentException("Presentazione report incompleta");
        }
        this.messages = messages;
        this.reasons = reasons;
    }

    public ReportMessages getMessages() {
        return messages;
    }

    /**
     * Riga semplice, senza segnaposto.
     */
    public ChatLine line(Language language, String key) {
        return ChatLine.text(messages.get(language, key));
    }

    /**
     * Riga semplice con segnaposto.
     */
    public ChatLine line(Language language, String key,
                         PlaceholderValues placeholders) {
        return ChatLine.text(messages.get(language, key, placeholders));
    }

    /**
     * Riepilogo del comando giocatore, motivi disponibili compresi.
     */
    public List<ChatLine> reportUsage(Language language) {
        List<ChatLine> lines = new ArrayList<>();
        lines.add(line(language, "reports.command.report.usage"));
        StringBuilder available = new StringBuilder();
        for (it.legacynetwork.reports.model.ReportReason reason
                : reasons.enabled()) {
            if (available.length() > 0) {
                available.append(", ");
            }
            available.append(reason.getId());
        }
        lines.add(line(language, "reports.command.report.reasons",
                PlaceholderValues.builder()
                        .put("reasons", available.toString())
                        .build()));
        return lines;
    }

    /**
     * Riepilogo dei comandi staff.
     */
    public List<ChatLine> staffUsage(Language language) {
        List<ChatLine> lines = new ArrayList<>();
        for (String key : new String[]{"reports.command.reports.header",
                "reports.command.reports.list",
                "reports.command.reports.info",
                "reports.command.reports.claim",
                "reports.command.reports.investigate",
                "reports.command.reports.release",
                "reports.command.reports.dismiss",
                "reports.command.reports.action",
                "reports.command.reports.player",
                "reports.command.reports.notifications"}) {
            lines.add(line(language, key));
        }
        return lines;
    }

    /**
     * Elenco paginato dei report attivi.
     */
    public List<ChatLine> list(Language language, ReportPage page) {
        List<ChatLine> lines = new ArrayList<>();
        lines.add(line(language, "reports.list.header", pagePlaceholders(page)));
        if (page.isEmpty()) {
            lines.add(line(language, "reports.list.empty"));
            return lines;
        }
        for (Report report : page.getItems()) {
            lines.add(entry(language, report, "reports.list.entry"));
        }
        lines.add(navigation(language, page, "/reports list "));
        return lines;
    }

    /**
     * Storico dei report a carico di un giocatore.
     */
    public List<ChatLine> playerHistory(Language language, String targetName,
                                        ReportPage page) {
        List<ChatLine> lines = new ArrayList<>();
        lines.add(line(language, "reports.player.header",
                PlaceholderValues.builder()
                        .put("target", targetName)
                        .put("page", page.getPage())
                        .put("pages", page.getTotalPages())
                        .put("total", page.getTotalItems())
                        .build()));
        if (page.isEmpty()) {
            lines.add(line(language, "reports.player.empty"));
            return lines;
        }
        for (Report report : page.getItems()) {
            lines.add(entry(language, report, "reports.player.entry"));
        }
        lines.add(navigation(language, page, "/reports player " + targetName
                + " "));
        return lines;
    }

    /**
     * Scheda completa di un report, storico compreso.
     */
    public List<ChatLine> info(Language language, Report report,
                               List<ReportEvent> history) {
        List<ChatLine> lines = new ArrayList<>();
        lines.add(line(language, "reports.info.header", base(language, report)));
        lines.add(line(language, "reports.info.reporter",
                base(language, report)));
        lines.add(line(language, "reports.info.target", base(language, report)));
        lines.add(line(language, "reports.info.reason", base(language, report)));
        if (report.getDetails().isPresent()) {
            lines.add(line(language, "reports.info.details",
                    base(language, report)));
        }
        lines.add(line(language, "reports.info.server", base(language, report)));
        lines.add(line(language, "reports.info.ping", base(language, report)));
        lines.add(line(language, "reports.info.status", base(language, report)));
        lines.add(line(language, "reports.info.staff", base(language, report)));
        lines.add(line(language, "reports.info.created",
                base(language, report)));
        lines.add(line(language, "reports.info.updated",
                base(language, report)));
        if (report.getResolution().isPresent()) {
            lines.add(line(language, "reports.info.resolution",
                    base(language, report)));
        }
        if (report.getScreenshareId().isPresent()) {
            lines.add(line(language, "reports.info.screenshare",
                    base(language, report)));
        }
        lines.add(line(language, "reports.info.history-header"));
        if (history == null || history.isEmpty()) {
            lines.add(line(language, "reports.info.history-empty"));
            return lines;
        }
        for (ReportEvent event : history) {
            lines.add(line(language, "reports.info.history-entry",
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

    /**
     * Notifica inviata allo staff quando nasce un report.
     */
    public List<ChatLine> notification(Language language, Report report) {
        List<ChatLine> lines = new ArrayList<>();
        lines.add(line(language, "reports.notification.created",
                base(language, report)));
        lines.add(buttons(language, report));
        return lines;
    }

    private ChatLine entry(Language language, Report report, String key) {
        List<ChatSegment> segments = new ArrayList<>();
        segments.add(ChatSegment.text(
                messages.get(language, key, base(language, report))));
        segments.add(ChatSegment.text(" "));
        segments.addAll(buttons(language, report).getSegments());
        return ChatLine.of(segments);
    }

    private ChatLine buttons(Language language, Report report) {
        PlaceholderValues placeholders = base(language, report);
        List<ChatSegment> segments = new ArrayList<>();
        segments.add(ChatSegment.run(
                messages.get(language, "reports.button.info", placeholders),
                messages.get(language, "reports.button.info.hover",
                        placeholders),
                "/reports info " + report.getId().shortCode()));
        segments.add(ChatSegment.text(" "));
        segments.add(ChatSegment.suggest(
                messages.get(language, "reports.button.claim", placeholders),
                messages.get(language, "reports.button.claim.hover",
                        placeholders),
                "/reports claim " + report.getId().shortCode()));
        return ChatLine.of(segments);
    }

    private ChatLine navigation(Language language, ReportPage page,
                                String commandPrefix) {
        List<ChatSegment> segments = new ArrayList<>();
        segments.add(ChatSegment.text(messages.get(language,
                "reports.list.footer", pagePlaceholders(page))));
        if (page.hasPreviousPage()) {
            segments.add(ChatSegment.text(" "));
            segments.add(ChatSegment.run(
                    messages.get(language, "reports.button.previous"),
                    messages.get(language, "reports.button.previous.hover"),
                    commandPrefix + (page.getPage() - 1)));
        }
        if (page.hasNextPage()) {
            segments.add(ChatSegment.text(" "));
            segments.add(ChatSegment.run(
                    messages.get(language, "reports.button.next"),
                    messages.get(language, "reports.button.next.hover"),
                    commandPrefix + (page.getPage() + 1)));
        }
        return ChatLine.of(segments);
    }

    private static PlaceholderValues pagePlaceholders(ReportPage page) {
        return PlaceholderValues.builder()
                .put("page", page.getPage())
                .put("pages", page.getTotalPages())
                .put("total", page.getTotalItems())
                .build();
    }

    /**
     * Segnaposto comuni a tutte le righe che descrivono un report.
     */
    public PlaceholderValues base(Language language, Report report) {
        return PlaceholderValues.builder()
                .put("id", report.getId().shortCode())
                .put("full-id", report.getId().toString())
                .put("reporter", report.getReporterName())
                .put("target", report.getTargetName())
                .put("reason", messages.get(language,
                        reasons.displayKey(report.getReasonId())))
                .put("details", report.getDetails().orElse(""))
                .put("server", report.getSnapshot().getServerId())
                .put("ping", report.getSnapshot().getTargetPingMillis())
                .put("status", messages.get(language,
                        report.getStatus().messageKey()))
                .put("staff", report.getAssignedStaffName().orElse(
                        messages.get(language, "reports.info.staff-none")))
                .put("resolution", report.getResolution().orElse(""))
                .put("screenshare", report.getScreenshareId()
                        .map(java.util.UUID::toString).orElse(""))
                .put("created", format(report.getCreatedAt()))
                .put("updated", format(report.getUpdatedAt()))
                .build();
    }

    private static String format(Instant instant) {
        return TIMESTAMP.format(instant);
    }
}
