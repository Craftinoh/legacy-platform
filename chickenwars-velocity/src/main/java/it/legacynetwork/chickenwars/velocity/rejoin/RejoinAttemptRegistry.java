package it.legacynetwork.chickenwars.velocity.rejoin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tentativi di rientro attualmente in corso, uno per giocatore.
 *
 * <p>E' la sola protezione contro il doppio comando: finche' un tentativo e'
 * aperto non ne parte un secondo, quindi non possono nascere due prenotazioni
 * ne' due richieste di connessione.</p>
 *
 * <p>Un tentativo piu' vecchio del timeout viene considerato abbandonato e
 * sostituito: un errore che impedisse la chiusura non bloccherebbe per sempre
 * il giocatore.</p>
 */
public final class RejoinAttemptRegistry {

    private final Map<UUID, Long> started = new HashMap<UUID, Long>();
    private final long timeoutMillis;

    public RejoinAttemptRegistry(long timeoutMillis) {
        this.timeoutMillis = Math.max(1L, timeoutMillis);
    }

    /**
     * Apre un tentativo per il giocatore.
     *
     * @return {@code true} se il tentativo e' stato aperto ora
     */
    public synchronized boolean begin(UUID playerId, long now) {
        if (playerId == null) {
            return false;
        }
        Long since = started.get(playerId);
        if (since != null && now - since.longValue() < timeoutMillis) {
            return false;
        }
        started.put(playerId, Long.valueOf(now));
        return true;
    }

    /**
     * Chiude il tentativo, qualunque sia stato l'esito.
     *
     * @return {@code true} se un tentativo era davvero aperto
     */
    public synchronized boolean finish(UUID playerId) {
        return playerId != null && started.remove(playerId) != null;
    }

    public synchronized boolean isInProgress(UUID playerId, long now) {
        Long since = started.get(playerId);
        return since != null && now - since.longValue() < timeoutMillis;
    }

    public synchronized int size() {
        return started.size();
    }

    /**
     * Dimentica ogni tentativo, allo spegnimento del proxy.
     */
    public synchronized void clear() {
        started.clear();
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }
}
