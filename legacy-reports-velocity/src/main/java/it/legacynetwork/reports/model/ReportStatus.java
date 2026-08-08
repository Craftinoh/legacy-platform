package it.legacynetwork.reports.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Stato di lavorazione di un report.
 *
 * <p>{@link #ACTION_TAKEN} e {@link #DISMISSED} sono definitivi: da li' non si
 * esce senza un'operazione amministrativa che questa prima versione non
 * espone.</p>
 */
public enum ReportStatus {

    OPEN,
    CLAIMED,
    INVESTIGATING,
    SCREENSHARE,
    ACTION_TAKEN,
    DISMISSED;

    /**
     * Indica se lo stato chiude il report.
     */
    public boolean isFinal() {
        return this == ACTION_TAKEN || this == DISMISSED;
    }

    /**
     * Indica se il report e' ancora in carico a qualcuno.
     */
    public boolean isAssigned() {
        return this == CLAIMED || this == INVESTIGATING || this == SCREENSHARE;
    }

    /**
     * Chiave di traduzione del nome mostrato.
     */
    public String messageKey() {
        return "reports.status." + name().toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }

    public static Optional<ReportStatus> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_');
        for (ReportStatus status : values()) {
            if (status.name().equals(normalized)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
