package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Gestisce posti prenotati, retry idempotenti e cleanup delle scadenze. */
public final class ReservationService {
    private final Map<String, GameReservation> reservations =
            new HashMap<String, GameReservation>();
    private final Map<String, String> byKey = new HashMap<String, String>();

    public synchronized GameReservation create(UUID playerId, MatchMode mode,
            String instanceId, long expiresAt, String idempotencyKey) {
        String existingId = byKey.get(idempotencyKey);
        if (existingId != null) return reservations.get(existingId);
        String id = UUID.randomUUID().toString();
        GameReservation value = new GameReservation(id, playerId, mode,
                instanceId, expiresAt, idempotencyKey);
        reservations.put(id, value); byKey.put(idempotencyKey, id); return value;
    }
    public synchronized GameReservation get(String id) { return reservations.get(id); }
    public synchronized boolean claim(String id, UUID playerId, long now) {
        GameReservation value = reservations.get(id);
        return value != null && value.getPlayerId().equals(playerId) && value.claim(now);
    }
    public synchronized boolean cancel(String id) {
        GameReservation value = reservations.get(id); return value != null && value.cancel();
    }
    public synchronized int reservedSeats(String instanceId, long now) {
        cleanup(now); int count = 0;
        for (GameReservation value : reservations.values()) {
            if (value.getInstanceId().equals(instanceId)
                    && value.getStatus() == ReservationStatus.CREATED) count++;
        }
        return count;
    }
    public synchronized int cleanup(long now) {
        int expired = 0;
        for (GameReservation value : reservations.values()) if (value.expire(now)) expired++;
        return expired;
    }
    public synchronized void clear() { reservations.clear(); byKey.clear(); }
}
