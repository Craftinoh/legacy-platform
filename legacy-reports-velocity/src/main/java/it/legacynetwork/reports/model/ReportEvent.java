package it.legacynetwork.reports.model;

import it.legacynetwork.reports.api.ReportEventType;
import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.api.ReportStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Riga di storico di un report.
 *
 * <p>Immutabile e sola aggiunta: descrive chi ha fatto cosa, da quale stato a
 * quale stato e quando. L'attore puo' mancare quando l'azione arriva dal
 * sistema — un recupero all'avvio, per esempio — ma il nome mostrato c'e'
 * sempre.</p>
 */
public final class ReportEvent {

    private final UUID id;
    private final ReportId reportId;
    private final UUID actorId;
    private final String actorName;
    private final ReportEventType type;
    private final ReportStatus previousStatus;
    private final ReportStatus newStatus;
    private final String message;
    private final String proxyId;
    private final Instant createdAt;

    public ReportEvent(UUID id, ReportId reportId, UUID actorId,
                       String actorName, ReportEventType type,
                       ReportStatus previousStatus, ReportStatus newStatus,
                       String message, String proxyId, Instant createdAt) {
        if (id == null || reportId == null || type == null
                || createdAt == null) {
            throw new IllegalArgumentException("Evento report incompleto");
        }
        if (actorName == null || actorName.trim().isEmpty()) {
            throw new IllegalArgumentException("Attore dell'evento mancante");
        }
        this.id = id;
        this.reportId = reportId;
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

    public ReportId getReportId() {
        return reportId;
    }

    public Optional<UUID> getActorId() {
        return Optional.ofNullable(actorId);
    }

    public String getActorName() {
        return actorName;
    }

    public ReportEventType getType() {
        return type;
    }

    public Optional<ReportStatus> getPreviousStatus() {
        return Optional.ofNullable(previousStatus);
    }

    public Optional<ReportStatus> getNewStatus() {
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
