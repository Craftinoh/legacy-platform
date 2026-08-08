package it.legacynetwork.screenshare.repository;

import it.legacynetwork.screenshare.model.ScreenshareEvent;
import it.legacynetwork.screenshare.model.ScreenshareSessionId;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Storico append-only delle sessioni.
 */
public interface ScreenshareEventRepository {

    CompletableFuture<ScreenshareEvent> append(ScreenshareEvent event);

    /**
     * Storico di una sessione, dal piu' recente.
     */
    CompletableFuture<List<ScreenshareEvent>> findBySession(
            ScreenshareSessionId sessionId, int limit);
}
