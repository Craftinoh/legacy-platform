package it.legacynetwork.screenshare.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Riga di storico di una sessione di controllo.
 */
public final class ScreenshareEvent {

    private final UUID id;
    private final ScreenshareSessionId sessionId;
    private final UUID actorId;
    private final String actorName;
    private final ScreenshareEventType type;
    private final ScreenshareStatus previousStatus;
    private final ScreenshareStatus newStatus;
    private final String message;
    private final String proxyId;
    private final Instant createdAt;

    public ScreenshareEvent(UUID id, ScreenshareSessionId sessionId,
                            UUID actorId, String actorName,
                            ScreenshareEventType type,
                            ScreenshareStatus previousStatus,
                            ScreenshareStatus newStatus, String message,
                            String proxyId, Instant createdAt) {
        if (id == null || sessionId == null || type == null
                || createdAt == null) {
            throw new IllegalArgumentException("Evento sessione incompleto");
        }
        if (actorName == null || actorName.trim().isEmpty()) {
            throw new IllegalArgumentException("Attore dell'evento mancante");
        }
        this.id = id;
        this.sessionId = sessionId;
        this.actorId = actorId;
        this.actorName = actorName.trim();
        this.type = type;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.message = message == null || message.trim().isEmpty()
                ? null : message.trim();
        this.proxyId = proxyId == null ? "" : proxyId.trim();
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public ScreenshareSessionId getSessionId() {
        return sessionId;
    }

    public Optional<UUID> getActorId() {
        return Optional.ofNullable(actorId);
    }

    public String getActorName() {
        return actorName;
    }

    public ScreenshareEventType getType() {
        return type;
    }

    public Optional<ScreenshareStatus> getPreviousStatus() {
        return Optional.ofNullable(previousStatus);
    }

    public Optional<ScreenshareStatus> getNewStatus() {
        return Optional.ofNullable(newStatus);
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public String getProxyId() {
        return proxyId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
