package it.legacynetwork.reports.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Report immutabile.
 *
 * <p>Ogni modifica produce una nuova istanza con {@code revision} incrementata:
 * e' quel numero, insieme allo stato atteso, a rendere condizionale l'update sul
 * database e a far fallire in modo pulito due staffer che agiscono insieme.</p>
 */
public final class Report {

    private final ReportId id;
    private final UUID reporterId;
    private final String reporterName;
    private final UUID targetId;
    private final String targetName;
    private final String reasonId;
    private final String details;
    private final ReportSnapshot snapshot;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final ReportStatus status;
    private final UUID assignedStaffId;
    private final String assignedStaffName;
    private final String resolution;
    private final String punishmentId;
    private final UUID screenshareId;
    private final long revision;

    private Report(Builder builder) {
        this.id = require(builder.id, "id");
        this.reporterId = require(builder.reporterId, "reporterId");
        this.reporterName = requireText(builder.reporterName, "reporterName");
        this.targetId = require(builder.targetId, "targetId");
        this.targetName = requireText(builder.targetName, "targetName");
        this.reasonId = requireText(builder.reasonId, "reasonId");
        this.details = trimToNull(builder.details);
        this.snapshot = require(builder.snapshot, "snapshot");
        this.createdAt = require(builder.createdAt, "createdAt");
        this.updatedAt = require(builder.updatedAt, "updatedAt");
        this.status = require(builder.status, "status");
        this.assignedStaffId = builder.assignedStaffId;
        this.assignedStaffName = trimToNull(builder.assignedStaffName);
        this.resolution = trimToNull(builder.resolution);
        this.punishmentId = trimToNull(builder.punishmentId);
        this.screenshareId = builder.screenshareId;
        this.revision = builder.revision;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .reporter(reporterId, reporterName)
                .target(targetId, targetName)
                .reasonId(reasonId)
                .details(details)
                .snapshot(snapshot)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .status(status)
                .assignedStaff(assignedStaffId, assignedStaffName)
                .resolution(resolution)
                .punishmentId(punishmentId)
                .screenshareId(screenshareId)
                .revision(revision);
    }

    public ReportId getId() {
        return id;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getReasonId() {
        return reasonId;
    }

    public Optional<String> getDetails() {
        return Optional.ofNullable(details);
    }

    public ReportSnapshot getSnapshot() {
        return snapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public Optional<UUID> getAssignedStaffId() {
        return Optional.ofNullable(assignedStaffId);
    }

    public Optional<String> getAssignedStaffName() {
        return Optional.ofNullable(assignedStaffName);
    }

    public Optional<String> getResolution() {
        return Optional.ofNullable(resolution);
    }

    public Optional<String> getPunishmentId() {
        return Optional.ofNullable(punishmentId);
    }

    public Optional<UUID> getScreenshareId() {
        return Optional.ofNullable(screenshareId);
    }

    public long getRevision() {
        return revision;
    }

    /**
     * Indica se lo staffer indicato ha gia' in carico il report.
     */
    public boolean isAssignedTo(UUID staffId) {
        return staffId != null && staffId.equals(assignedStaffId);
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Campo report mancante: " + field);
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Campo report mancante: " + field);
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

    /** Costruttore fluente del report. */
    public static final class Builder {

        private ReportId id;
        private UUID reporterId;
        private String reporterName;
        private UUID targetId;
        private String targetName;
        private String reasonId;
        private String details;
        private ReportSnapshot snapshot;
        private Instant createdAt;
        private Instant updatedAt;
        private ReportStatus status = ReportStatus.OPEN;
        private UUID assignedStaffId;
        private String assignedStaffName;
        private String resolution;
        private String punishmentId;
        private UUID screenshareId;
        private long revision;

        public Builder id(ReportId value) {
            this.id = value;
            return this;
        }

        public Builder reporter(UUID uuid, String name) {
            this.reporterId = uuid;
            this.reporterName = name;
            return this;
        }

        public Builder target(UUID uuid, String name) {
            this.targetId = uuid;
            this.targetName = name;
            return this;
        }

        public Builder reasonId(String value) {
            this.reasonId = value;
            return this;
        }

        public Builder details(String value) {
            this.details = value;
            return this;
        }

        public Builder snapshot(ReportSnapshot value) {
            this.snapshot = value;
            return this;
        }

        public Builder createdAt(Instant value) {
            this.createdAt = value;
            return this;
        }

        public Builder updatedAt(Instant value) {
            this.updatedAt = value;
            return this;
        }

        public Builder status(ReportStatus value) {
            this.status = value;
            return this;
        }

        public Builder assignedStaff(UUID uuid, String name) {
            this.assignedStaffId = uuid;
            this.assignedStaffName = name;
            return this;
        }

        public Builder resolution(String value) {
            this.resolution = value;
            return this;
        }

        public Builder punishmentId(String value) {
            this.punishmentId = value;
            return this;
        }

        public Builder screenshareId(UUID value) {
            this.screenshareId = value;
            return this;
        }

        public Builder revision(long value) {
            this.revision = value;
            return this;
        }

        public Report build() {
            return new Report(this);
        }
    }
}
