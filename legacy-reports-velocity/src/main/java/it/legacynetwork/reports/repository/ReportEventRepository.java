package it.legacynetwork.reports.repository;

import it.legacynetwork.reports.model.ReportEvent;
import it.legacynetwork.reports.api.ReportId;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Storico append-only dei report.
 *
 * <p>Non esiste alcun metodo di cancellazione o modifica: e' la garanzia che
 * rende l'audit utile.</p>
 */
public interface ReportEventRepository {

    /**
     * Aggiunge un evento.
     */
    CompletableFuture<ReportEvent> append(ReportEvent event);

    /**
     * Storico di un report, dal piu' recente.
     *
     * @param limit numero massimo di righe restituite
     */
    CompletableFuture<List<ReportEvent>> findByReport(ReportId reportId,
                                                      int limit);
}
