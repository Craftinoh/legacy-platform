package it.legacynetwork.reports.config;

import it.legacynetwork.language.Language;

import java.time.Duration;

/**
 * Configurazione di LegacyReports.
 *
 * <p>Nessun limite e nessun permesso e' scritto nel codice: tutto arriva da
 * {@code config.yml} con un valore predefinito ragionevole.</p>
 */
public final class ReportsConfiguration {

    private final Language fallbackLanguage;
    private final String proxyId;
    private final int pageSize;
    private final int historySize;
    private final Duration cooldown;
    private final Duration duplicateWindow;
    private final int maxOpenPerReporter;
    private final int detailsMaxLength;
    private final boolean protectStaff;
    private final boolean notifyByDefault;
    private final ReportPermissions permissions;
    private final ReportReasonCatalog reasons;

    private ReportsConfiguration(Language fallbackLanguage, String proxyId,
                                 int pageSize, int historySize,
                                 Duration cooldown, Duration duplicateWindow,
                                 int maxOpenPerReporter, int detailsMaxLength,
                                 boolean protectStaff, boolean notifyByDefault,
                                 ReportPermissions permissions,
                                 ReportReasonCatalog reasons) {
        this.fallbackLanguage = fallbackLanguage;
        this.proxyId = proxyId;
        this.pageSize = pageSize;
        this.historySize = historySize;
        this.cooldown = cooldown;
        this.duplicateWindow = duplicateWindow;
        this.maxOpenPerReporter = maxOpenPerReporter;
        this.detailsMaxLength = detailsMaxLength;
        this.protectStaff = protectStaff;
        this.notifyByDefault = notifyByDefault;
        this.permissions = permissions;
        this.reasons = reasons;
    }

    /**
     * Legge l'intero file di configurazione.
     */
    public static ReportsConfiguration fromRoot(ConfigSection root) {
        ConfigSection reports = root.section("reports");
        Language fallback = Language
                .findByInput(root.section("language").text("fallback", "en"))
                .orElse(Language.ENGLISH);
        return new ReportsConfiguration(
                fallback,
                reports.text("proxy-id", "proxy"),
                Math.max(1, reports.number("page-size", 8)),
                Math.max(1, reports.number("history-size", 10)),
                Duration.ofSeconds(
                        Math.max(0L, reports.duration("cooldown-seconds", 60L))),
                Duration.ofMinutes(Math.max(0L,
                        reports.duration("duplicate-window-minutes", 30L))),
                Math.max(0, reports.number("max-open-per-reporter", 3)),
                Math.max(0, reports.section("details")
                        .number("max-length", 200)),
                reports.flag("protect-staff", true),
                reports.flag("notifications-enabled-by-default", true),
                ReportPermissions.fromSection(reports.section("permissions")),
                ReportReasonCatalog.fromSection(reports.section("reasons")));
    }

    public Language getFallbackLanguage() {
        return fallbackLanguage;
    }

    public String getProxyId() {
        return proxyId;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getHistorySize() {
        return historySize;
    }

    public Duration getCooldown() {
        return cooldown;
    }

    public Duration getDuplicateWindow() {
        return duplicateWindow;
    }

    /**
     * Numero massimo di report ancora aperti per lo stesso segnalatore;
     * {@code 0} disattiva il limite.
     */
    public int getMaxOpenPerReporter() {
        return maxOpenPerReporter;
    }

    /**
     * Lunghezza massima dei dettagli; {@code 0} disattiva il controllo.
     */
    public int getDetailsMaxLength() {
        return detailsMaxLength;
    }

    /**
     * Se attivo, chi possiede il permesso protetto non puo' essere segnalato.
     */
    public boolean isProtectStaff() {
        return protectStaff;
    }

    public boolean isNotifyByDefault() {
        return notifyByDefault;
    }

    public ReportPermissions getPermissions() {
        return permissions;
    }

    public ReportReasonCatalog getReasons() {
        return reasons;
    }
}
