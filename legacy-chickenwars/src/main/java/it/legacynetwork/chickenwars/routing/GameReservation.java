package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.UUID;

public final class GameReservation {
    private final String reservationId;
    private final UUID playerId;
    private final MatchMode mode;
    private final String instanceId;
    private final long expiresAt;
    private final String idempotencyKey;
    private ReservationStatus status;

    public GameReservation(String reservationId, UUID playerId, MatchMode mode,
            String instanceId, long expiresAt, String idempotencyKey) {
        if (reservationId == null || playerId == null || mode == null
                || instanceId == null || idempotencyKey == null) {
            throw new IllegalArgumentException("Prenotazione non valida");
        }
        this.reservationId = reservationId; this.playerId = playerId;
        this.mode = mode; this.instanceId = instanceId;
        this.expiresAt = expiresAt; this.idempotencyKey = idempotencyKey;
        this.status = ReservationStatus.CREATED;
    }
    public synchronized boolean claim(long now) {
        expire(now);
        if (status != ReservationStatus.CREATED) return false;
        status = ReservationStatus.CLAIMED; return true;
    }
    public synchronized boolean cancel() {
        if (status != ReservationStatus.CREATED) return false;
        status = ReservationStatus.CANCELLED; return true;
    }
    public synchronized boolean expire(long now) {
        if (status == ReservationStatus.CREATED && now >= expiresAt) {
            status = ReservationStatus.EXPIRED; return true;
        }
        return false;
    }
    public String getReservationId() { return reservationId; }
    public UUID getPlayerId() { return playerId; }
    public MatchMode getMode() { return mode; }
    public String getInstanceId() { return instanceId; }
    public long getExpiresAt() { return expiresAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public synchronized ReservationStatus getStatus() { return status; }
}
