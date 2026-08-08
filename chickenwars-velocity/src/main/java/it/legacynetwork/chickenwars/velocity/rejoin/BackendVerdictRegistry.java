package it.legacynetwork.chickenwars.velocity.rejoin;

import it.legacynetwork.chickenwars.routing.RejoinVerdictCodec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Attese dell'esito che il backend deve ancora comunicare.
 *
 * <p>Il proxy sa soltanto di avere collegato il giocatore: se la validazione
 * ChickenWars lo rifiuta, la notizia arriva su questo canale. Senza l'attesa,
 * un rifiuto del backend sarebbe indistinguibile da un successo.</p>
 *
 * <p>Il ritardo per il timeout e' iniettato: i test possono farlo scattare
 * senza attese reali.</p>
 */
public final class BackendVerdictRegistry {

    /** Programma un'azione differita, tipicamente sullo scheduler del proxy. */
    public interface Delayer {
        void schedule(Runnable action, long delayMillis);
    }

    private final Map<UUID, CompletableFuture<BackendVerdict>> waiting =
            new LinkedHashMap<UUID, CompletableFuture<BackendVerdict>>();
    private final Delayer delayer;
    private final long timeoutMillis;

    public BackendVerdictRegistry(Delayer delayer, long timeoutMillis) {
        if (delayer == null) {
            throw new IllegalArgumentException("Delayer mancante");
        }
        this.delayer = delayer;
        this.timeoutMillis = Math.max(1L, timeoutMillis);
    }

    /**
     * Apre l'attesa dell'esito per un giocatore.
     *
     * <p>Un'attesa precedente per lo stesso giocatore viene chiusa in timeout:
     * non possono restare due futuri appesi.</p>
     */
    public CompletableFuture<BackendVerdict> await(final UUID playerId) {
        if (playerId == null) {
            return CompletableFuture.completedFuture(
                    BackendVerdict.timedOut());
        }
        CompletableFuture<BackendVerdict> pending =
                new CompletableFuture<BackendVerdict>();
        CompletableFuture<BackendVerdict> previous;
        synchronized (this) {
            previous = waiting.put(playerId, pending);
        }
        if (previous != null && !previous.isDone()) {
            previous.complete(BackendVerdict.timedOut());
        }
        delayer.schedule(new Runnable() {
            @Override
            public void run() {
                timeout(playerId);
            }
        }, timeoutMillis);
        return pending;
    }

    /**
     * Consegna l'esito ricevuto dal backend.
     *
     * @return {@code true} se qualcuno lo stava davvero attendendo
     */
    public boolean complete(RejoinVerdictCodec.Verdict verdict) {
        if (verdict == null) {
            return false;
        }
        CompletableFuture<BackendVerdict> pending = remove(verdict.getPlayerId());
        if (pending == null) {
            return false;
        }
        return pending.complete(verdict.isAccepted()
                ? BackendVerdict.accepted()
                : BackendVerdict.rejected(verdict.getReason(),
                        verdict.getArenaId()));
    }

    /**
     * Chiude l'attesa scaduta.
     *
     * @return {@code true} se l'attesa era ancora aperta
     */
    public boolean timeout(UUID playerId) {
        CompletableFuture<BackendVerdict> pending = remove(playerId);
        return pending != null && pending.complete(BackendVerdict.timedOut());
    }

    /**
     * Abbandona l'attesa senza produrre un esito applicativo.
     */
    public boolean cancel(UUID playerId) {
        CompletableFuture<BackendVerdict> pending = remove(playerId);
        return pending != null && pending.complete(BackendVerdict.timedOut());
    }

    private synchronized CompletableFuture<BackendVerdict> remove(UUID playerId) {
        return playerId == null ? null : waiting.remove(playerId);
    }

    public synchronized int size() {
        return waiting.size();
    }

    public synchronized void clear() {
        for (CompletableFuture<BackendVerdict> pending : waiting.values()) {
            pending.complete(BackendVerdict.timedOut());
        }
        waiting.clear();
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }
}
