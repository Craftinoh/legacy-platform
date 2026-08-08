package it.legacynetwork.screenshare.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Sessione di controllo, immutabile.
 *
 * <p>Come per i report ogni modifica produce una nuova istanza con
 * {@code revision} incrementata: e' quel numero, insieme allo stato atteso, a
 * rendere condizionale l'aggiornamento sul database.</p>
 */
public final class ScreenshareSession {

    private final ScreenshareSessionId id;
    private final UUID targetId;
    private final String targetName;
    private final UUID staffId;
    private final String staffName;
    private final UUID reportId;
    private final String serverId;
    private final Instant createdAt;
    private final Instant startedAt;
    private final Instant expiresAt;
    private final Instant endedAt;
    private final ScreenshareStatus status;
    private final ScreenshareOutcome outcome;
    private final String notes;
    private final String proxyId;
    private final long revision;

    private ScreenshareSession(Builder builder) {
        this.id = require(builder.id, "id");
        this.targetId = require(builder.targetId, "targetId");
        this.targetName = requireText(builder.targetName, "targetName");
        this.staffId = require(builder.staffId, "staffId");
        this.staffName = requireText(builder.staffName, "staffName");
        this.reportId = builder.reportId;
        this.serverId = requireText(builder.serverId, "serverId");
        this.createdAt = require(builder.createdAt, "createdAt");
        this.startedAt = builder.startedAt;
        this.expiresAt = require(builder.expiresAt, "expiresAt");
        this.endedAt = builder.endedAt;
        this.status = require(builder.status, "status");
        this.outcome = builder.outcome;
        this.notes = trimToNull(builder.notes);
        this.proxyId = builder.proxyId == null ? "" : builder.proxyId.trim();
        this.revision = builder.revision;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .target(targetId, targetName)
                .staff(staffId, staffName)
                .reportId(reportId)
                .serverId(serverId)
                .createdAt(createdAt)
                .startedAt(startedAt)
                .expiresAt(expiresAt)
                .endedAt(endedAt)
                .status(status)
                .outcome(outcome)
                .notes(notes)
                .proxyId(proxyId)
                .revision(revision);
    }

    public ScreenshareSessionId getId() {
        return id;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    /**
     * Report collegato, se il controllo nasce da una segnalazione.
     */
    public Optional<UUID> getReportId() {
        return Optional.ofNullable(reportId);
    }

    /**
     * Server su cui si svolge il controllo.
     */
    public String getServerId() {
        return serverId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Istante in cui entrambi erano collegati al server di controllo.
     */
    public Optional<Instant> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    /**
     * Scadenza oltre la quale la sessione va chiusa d'ufficio.
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Optional<Instant> getEndedAt() {
        return Optional.ofNullable(endedAt);
    }

    public ScreenshareStatus getStatus() {
        return status;
    }

    public Optional<ScreenshareOutcome> getOutcome() {
        return Optional.ofNullable(outcome);
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }

    public String getProxyId() {
        return proxyId;
    }

    public long getRevision() {
        return revision;
    }

    /**
     * Indica se la sessione riguarda il giocatore indicato.
     */
    public boolean involves(UUID playerId) {
        return playerId != null
                && (playerId.equals(targetId) || playerId.equals(staffId));
    }

    /**
     * Aggiunge una nota allo storico testuale della sessione.
     */
    public String appendNote(String note) {
        if (note == null || note.trim().isEmpty()) {
            return notes;
        }
        return notes == null ? note.trim() : notes + " | " + note.trim();
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Campo sessione mancante: "
                    + field);
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Campo sessione mancante: "
                    + field);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Costruttore fluente della sessione. */
    public static final class Builder {

        private ScreenshareSessionId id;
        private UUID targetId;
        private String targetName;
        private UUID staffId;
        private String staffName;
        private UUID reportId;
        private String serverId;
        private Instant createdAt;
        private Instant startedAt;
        private Instant expiresAt;
        private Instant endedAt;
        private ScreenshareStatus status = ScreenshareStatus.CREATED;
        private ScreenshareOutcome outcome;
        private String notes;
        private String proxyId;
        private long revision;

        public Builder id(ScreenshareSessionId value) {
            this.id = value;
            return this;
        }

        public Builder target(UUID uuid, String name) {
            this.targetId = uuid;
            this.targetName = name;
            return this;
        }

        public Builder staff(UUID uuid, String name) {
            this.staffId = uuid;
            this.staffName = name;
            return this;
        }

        public Builder reportId(UUID value) {
            this.reportId = value;
            return this;
        }

        public Builder serverId(String value) {
            this.serverId = value;
            return this;
        }

        public Builder createdAt(Instant value) {
            this.createdAt = value;
            return this;
        }

        public Builder startedAt(Instant value) {
            this.startedAt = value;
            return this;
        }

        public Builder expiresAt(Instant value) {
            this.expiresAt = value;
            return this;
        }

        public Builder endedAt(Instant value) {
            this.endedAt = value;
            return this;
        }

        public Builder status(ScreenshareStatus value) {
            this.status = value;
            return this;
        }

        public Builder outcome(ScreenshareOutcome value) {
            this.outcome = value;
            return this;
        }

        public Builder notes(String value) {
            this.notes = value;
            return this;
        }

        public Builder proxyId(String value) {
            this.proxyId = value;
            return this;
        }

        public Builder revision(long value) {
            this.revision = value;
            return this;
        }

        public ScreenshareSession build() {
            return new ScreenshareSession(this);
        }
    }
}
