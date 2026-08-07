package it.legacynetwork.chickenwars.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Preferenza distribuita verso l'istanza precedente, con fallback lobby. */
public final class DistributedReconnectService {
    public static final class Session {
        private final UUID playerId; private final String instanceId;
        private final long expiresAt; private boolean consumed;
        public Session(UUID playerId, String instanceId, long expiresAt) {
            this.playerId = playerId; this.instanceId = instanceId; this.expiresAt = expiresAt;
        }
        public UUID getPlayerId() { return playerId; }
        public String getInstanceId() { return instanceId; }
        public long getExpiresAt() { return expiresAt; }
    }
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    public synchronized void remember(Session session) { sessions.put(session.playerId, session); }
    public synchronized void forget(UUID playerId) { sessions.remove(playerId); }
    public synchronized GameInstanceDescriptor consume(UUID playerId, long now,
            InstanceRegistry registry, long heartbeatTimeout) {
        Session session = sessions.get(playerId);
        if (session == null || session.consumed || session.expiresAt <= now) {
            sessions.remove(playerId); return null;
        }
        GameInstanceDescriptor instance = registry.find(session.instanceId);
        if (instance == null || instance.getHeartbeatAt() < now - heartbeatTimeout
                || instance.getStatus() == InstanceStatus.ENDING
                || instance.getStatus() == InstanceStatus.OFFLINE) return null;
        session.consumed = true; sessions.remove(playerId); return instance;
    }
    public synchronized void clear() { sessions.clear(); }
}
