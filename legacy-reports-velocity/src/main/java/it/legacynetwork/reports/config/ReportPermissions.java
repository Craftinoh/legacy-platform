package it.legacynetwork.reports.config;

/**
 * Nodi di permesso, tutti configurabili.
 *
 * <p>I valori predefiniti seguono la convenzione gia' usata dagli altri plugin
 * della rete: {@code <plugin>.<area>.<azione>}.</p>
 */
public final class ReportPermissions {

    private final String report;
    private final String staffView;
    private final String staffClaim;
    private final String staffResolve;
    private final String staffHistory;
    private final String admin;
    private final String protectedTarget;

    private ReportPermissions(String report, String staffView,
                              String staffClaim, String staffResolve,
                              String staffHistory, String admin,
                              String protectedTarget) {
        this.report = report;
        this.staffView = staffView;
        this.staffClaim = staffClaim;
        this.staffResolve = staffResolve;
        this.staffHistory = staffHistory;
        this.admin = admin;
        this.protectedTarget = protectedTarget;
    }

    public static ReportPermissions defaults() {
        return fromSection(ConfigSection.empty());
    }

    public static ReportPermissions fromSection(ConfigSection section) {
        return new ReportPermissions(
                section.text("report", "legacyreports.command.report"),
                section.text("staff-view", "legacyreports.staff.view"),
                section.text("staff-claim", "legacyreports.staff.claim"),
                section.text("staff-resolve", "legacyreports.staff.resolve"),
                section.text("staff-history", "legacyreports.staff.history"),
                section.text("admin", "legacyreports.admin"),
                section.text("protected", "legacyreports.protected"));
    }

    public String getReport() {
        return report;
    }

    public String getStaffView() {
        return staffView;
    }

    public String getStaffClaim() {
        return staffClaim;
    }

    public String getStaffResolve() {
        return staffResolve;
    }

    public String getStaffHistory() {
        return staffHistory;
    }

    public String getAdmin() {
        return admin;
    }

    /**
     * Permesso che rende un giocatore non segnalabile, se la protezione e'
     * attiva.
     */
    public String getProtectedTarget() {
        return protectedTarget;
    }
}
