package it.legacynetwork.screenshare.violation;

import it.legacynetwork.screenshare.model.ScreenshareSessionId;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Richiesta strutturata emessa quando un controllo si chiude in violazione.
 *
 * <p>Contiene i fatti, non una decisione: chi la riceve sceglie cosa farne.
 * Oggi la riceve solo il gestore di sola registrazione.</p>
 */
public final class ScreenshareViolation {

    private final UUID targetId;
    private final String targetName;
    private final UUID staffId;
    private final String staffName;
    private final ScreenshareSessionId sessionId;
    private final UUID reportId;
    private final ScreenshareViolationType type;
    private final Instant occurredAt;
    private final Map<String, String> context;
    private final SuggestedPunishmentCategory suggestedCategory;

    public ScreenshareViolation(UUID targetId, String targetName, UUID staffId,
                                String staffName,
                                ScreenshareSessionId sessionId, UUID reportId,
                                ScreenshareViolationType type,
                                Instant occurredAt,
                                Map<String, String> context,
                                SuggestedPunishmentCategory suggestedCategory) {
        if (targetId == null || sessionId == null || type == null
                || occurredAt == null) {
            throw new IllegalArgumentException("Violazione incompleta");
        }
        this.targetId = targetId;
        this.targetName = targetName == null ? "" : targetName;
        this.staffId = staffId;
        this.staffName = staffName == null ? "" : staffName;
        this.sessionId = sessionId;
        this.reportId = reportId;
        this.type = type;
        this.occurredAt = occurredAt;
        this.context = Collections.unmodifiableMap(new LinkedHashMap<>(
                context == null ? Collections.emptyMap() : context));
        this.suggestedCategory = suggestedCategory == null
                ? SuggestedPunishmentCategory.NONE : suggestedCategory;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public Optional<UUID> getStaffId() {
        return Optional.ofNullable(staffId);
    }

    public String getStaffName() {
        return staffName;
    }

    public ScreenshareSessionId getSessionId() {
        return sessionId;
    }

    public Optional<UUID> getReportId() {
        return Optional.ofNullable(reportId);
    }

    public ScreenshareViolationType getType() {
        return type;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Contesto osservato: server, stato della sessione, durata.
     */
    public Map<String, String> getContext() {
        return context;
    }

    public SuggestedPunishmentCategory getSuggestedCategory() {
        return suggestedCategory;
    }
}
