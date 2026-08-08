package it.legacynetwork.reports.api;

import it.legacynetwork.reports.model.Report;

import java.util.Optional;

/**
 * Risultato di un'operazione sul report.
 *
 * <p>Non contiene testo: porta lo stato, la chiave di traduzione da mostrare e,
 * quando esiste, il report aggiornato.</p>
 */
public final class ReportOperationResult {

    private final ReportOperationStatus status;
    private final Report report;
    private final String messageKey;

    private ReportOperationResult(ReportOperationStatus status, Report report,
                                  String messageKey) {
        if (status == null) {
            throw new IllegalArgumentException("Esito operazione mancante");
        }
        this.status = status;
        this.report = report;
        this.messageKey = messageKey == null || messageKey.trim().isEmpty()
                ? status.messageKey() : messageKey.trim();
    }

    public static ReportOperationResult success(Report report,
                                                String messageKey) {
        return new ReportOperationResult(ReportOperationStatus.SUCCESS, report,
                messageKey);
    }

    public static ReportOperationResult unchanged(Report report,
                                                  String messageKey) {
        return new ReportOperationResult(ReportOperationStatus.UNCHANGED,
                report, messageKey);
    }

    public static ReportOperationResult failure(ReportOperationStatus status) {
        return new ReportOperationResult(status, null, null);
    }

    public static ReportOperationResult failure(ReportOperationStatus status,
                                                Report report) {
        return new ReportOperationResult(status, report, null);
    }

    public ReportOperationStatus getStatus() {
        return status;
    }

    public Optional<Report> getReport() {
        return Optional.ofNullable(report);
    }

    public String getMessageKey() {
        return messageKey;
    }

    public boolean isApplied() {
        return status.isApplied();
    }

    @Override
    public String toString() {
        return "ReportOperationResult[" + status + "]";
    }
}
