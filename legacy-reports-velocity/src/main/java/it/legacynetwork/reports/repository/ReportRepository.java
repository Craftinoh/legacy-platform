package it.legacynetwork.reports.repository;

import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.model.ReportId;
import it.legacynetwork.reports.model.ReportStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage dei report.
 *
 * <p>Nessun metodo cancella: un report sbagliato viene chiuso, non rimosso.
 * Tutte le operazioni restituiscono un future perche' nessuna query deve girare
 * sul thread eventi di Velocity.</p>
 */
public interface ReportRepository {

    /**
     * Registra un nuovo report.
     */
    CompletableFuture<Report> insert(Report report);

    /**
     * Cerca per identificatore esatto.
     */
    CompletableFuture<Optional<Report>> find(ReportId id);

    /**
     * Cerca da un riferimento digitato: UUID completo oppure prefisso breve.
     *
     * <p>Un prefisso ambiguo non seleziona nessuno: meglio nessun risultato che
     * il report sbagliato.</p>
     */
    CompletableFuture<Optional<Report>> findByReference(String reference);

    /**
     * Elenca i report negli stati indicati, dal piu' recente.
     */
    CompletableFuture<ReportPage> listByStatuses(Set<ReportStatus> statuses,
                                                 int page, int pageSize);

    /**
     * Elenca lo storico di un giocatore segnalato.
     */
    CompletableFuture<ReportPage> listByTarget(UUID targetId, int page,
                                               int pageSize);

    /**
     * Conta i report ancora aperti creati da un segnalatore.
     */
    CompletableFuture<Integer> countByReporter(UUID reporterId,
                                               Set<ReportStatus> statuses);

    /**
     * Cerca un report recente dello stesso segnalatore sullo stesso bersaglio.
     */
    CompletableFuture<Optional<Report>> findRecentDuplicate(UUID reporterId,
                                                            UUID targetId,
                                                            Instant notBefore);

    /**
     * Applica un aggiornamento condizionato allo stato e alla revisione attesi.
     *
     * @param updated report con la nuova revisione gia' impostata
     * @param expectedStatus stato che deve trovarsi sul database
     * @param expectedRevision revisione che deve trovarsi sul database
     * @return {@code true} se la riga e' stata aggiornata
     */
    CompletableFuture<Boolean> update(Report updated,
                                      ReportStatus expectedStatus,
                                      long expectedRevision);
}
