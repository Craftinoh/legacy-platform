package it.legacynetwork.screenshare.session;

import it.legacynetwork.screenshare.model.ScreenshareSessionId;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vincoli attivi, tenuti in memoria.
 *
 * <p>Gli eventi di Velocity — cambio server, comando — vanno decisi
 * immediatamente e non possono aspettare il database: questo registro e' la
 * copia locale di cio' che serve a quelle decisioni. Il database resta la
 * verita' storica, questo e' solo lo stato del momento.</p>
 */
public final class ActiveSessionRegistry {

    private final Map<UUID, TargetLock> targets = new ConcurrentHashMap<>();
    private final Map<UUID, ScreenshareSessionId> staff =
            new ConcurrentHashMap<>();

    /**
     * Lega un bersaglio al server di controllo.
     */
    public void lock(UUID targetId, ScreenshareSessionId sessionId,
                     String screenshareServer) {
        if (targetId == null || sessionId == null) {
            return;
        }
        targets.put(targetId, new TargetLock(sessionId,
                screenshareServer == null ? "" : screenshareServer, false));
    }

    /**
     * Consente al bersaglio di raggiungere i server di rientro.
     *
     * <p>Serve nell'ultimo tratto: la sessione e' finita e il giocatore va
     * riportato in lobby, ma il vincolo non e' ancora stato rimosso.</p>
     */
    public void allowCleanup(UUID targetId) {
        if (targetId == null) {
            return;
        }
        TargetLock current = targets.get(targetId);
        if (current != null) {
            targets.put(targetId, new TargetLock(current.sessionId,
                    current.screenshareServer, true));
        }
    }

    public void unlock(UUID targetId) {
        if (targetId != null) {
            targets.remove(targetId);
        }
    }

    public Optional<TargetLock> lockOf(UUID targetId) {
        return targetId == null ? Optional.empty()
                : Optional.ofNullable(targets.get(targetId));
    }

    public boolean isLocked(UUID targetId) {
        return targetId != null && targets.containsKey(targetId);
    }

    /**
     * Registra lo staffer impegnato in una sessione.
     */
    public void assignStaff(UUID staffId, ScreenshareSessionId sessionId) {
        if (staffId != null && sessionId != null) {
            staff.put(staffId, sessionId);
        }
    }

    public void releaseStaff(UUID staffId) {
        if (staffId != null) {
            staff.remove(staffId);
        }
    }

    public Optional<ScreenshareSessionId> sessionOfStaff(UUID staffId) {
        return staffId == null ? Optional.empty()
                : Optional.ofNullable(staff.get(staffId));
    }

    /**
     * Dimentica tutto: usato allo spegnimento del proxy.
     */
    public void clear() {
        targets.clear();
        staff.clear();
    }

    /** Vincolo attivo su un bersaglio. */
    public static final class TargetLock {

        private final ScreenshareSessionId sessionId;
        private final String screenshareServer;
        private final boolean cleanup;

        TargetLock(ScreenshareSessionId sessionId, String screenshareServer,
                   boolean cleanup) {
            this.sessionId = sessionId;
            this.screenshareServer = screenshareServer;
            this.cleanup = cleanup;
        }

        public ScreenshareSessionId getSessionId() {
            return sessionId;
        }

        public String getScreenshareServer() {
            return screenshareServer;
        }

        /**
         * Indica se il bersaglio puo' raggiungere i server di rientro.
         */
        public boolean isCleanup() {
            return cleanup;
        }
    }
}
