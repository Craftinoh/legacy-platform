package it.legacynetwork.screenshare.repository;

import it.legacynetwork.screenshare.model.ScreenshareSession;
import it.legacynetwork.screenshare.model.ScreenshareSessionId;
import it.legacynetwork.screenshare.model.ScreenshareStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage delle sessioni di controllo.
 *
 * <p>Nessun metodo cancella e nessuna query gira sul thread di chi chiama.</p>
 */
public interface ScreenshareRepository {

    CompletableFuture<ScreenshareSession> insert(ScreenshareSession session);

    CompletableFuture<Optional<ScreenshareSession>> find(
            ScreenshareSessionId id);

    /**
     * Sessione non conclusa del giocatore indicato, se esiste.
     */
    CompletableFuture<Optional<ScreenshareSession>> findOpenByTarget(
            UUID targetId);

    /**
     * Sessioni non concluse condotte dallo staffer indicato.
     */
    CompletableFuture<List<ScreenshareSession>> findOpenByStaff(UUID staffId);

    /**
     * Tutte le sessioni non concluse: serve ai timeout e al recupero.
     */
    CompletableFuture<List<ScreenshareSession>> findOpen();

    CompletableFuture<ScreensharePage> listByStatuses(
            Set<ScreenshareStatus> statuses, int page, int pageSize);

    /**
     * Aggiornamento condizionato allo stato e alla revisione attesi.
     *
     * @return {@code true} se la riga e' stata aggiornata
     */
    CompletableFuture<Boolean> update(ScreenshareSession updated,
                                      ScreenshareStatus expectedStatus,
                                      long expectedRevision);
}
