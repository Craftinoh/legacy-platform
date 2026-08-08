package it.legacynetwork.reports.api;

import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.model.ReportEventType;
import it.legacynetwork.reports.model.ReportId;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Superficie pubblica di LegacyReports per gli altri plugin del proxy.
 *
 * <p>Piccola per scelta: serve a LegacyScreenshare per collegare un controllo a
 * una segnalazione, non a esporre l'intero dominio. Le operazioni sono
 * idempotenti — ripeterle con gli stessi argomenti non produce un secondo
 * cambio di stato — e non bloccano mai il thread di chi chiama.</p>
 */
public interface LegacyReportsApi {

    /**
     * Cerca un report per identificatore.
     */
    CompletableFuture<Optional<Report>> findReport(ReportId id);

    /**
     * Cerca un report da un riferimento digitato dallo staff.
     *
     * @param reference UUID completo oppure prefisso breve
     */
    CompletableFuture<Optional<Report>> findReportByReference(String reference);

    /**
     * Porta il report in {@code SCREENSHARE} e vi collega la sessione.
     */
    CompletableFuture<ReportOperationResult> markScreenshareStarted(
            ReportId id, UUID staffId, UUID screenshareSessionId);

    /**
     * Riporta il report in {@code INVESTIGATING} al termine del controllo.
     *
     * <p>Il report non viene mai chiuso da qui: l'esito del controllo e' una
     * nota di storico, la decisione resta allo staff.</p>
     *
     * @param outcomeKey chiave di traduzione dell'esito, mai testo gia' scritto
     */
    CompletableFuture<ReportOperationResult> markScreenshareEnded(
            ReportId id, UUID staffId, UUID screenshareSessionId,
            String outcomeKey);

    /**
     * Variante che consente di scegliere il tipo di evento registrato.
     *
     * <p>Serve a distinguere nello storico una chiusura regolare da un
     * annullamento, da un fallimento di trasferimento o da una violazione.</p>
     */
    CompletableFuture<ReportOperationResult> markScreenshareEnded(
            ReportId id, UUID staffId, UUID screenshareSessionId,
            String outcomeKey, ReportEventType auditType);

    /**
     * Aggiunge una riga di storico senza cambiare stato.
     */
    CompletableFuture<ReportOperationResult> addAuditEvent(
            ReportId id, UUID actorId, String actorName, ReportEventType type,
            String message);
}
