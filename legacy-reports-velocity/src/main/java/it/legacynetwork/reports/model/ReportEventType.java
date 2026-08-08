package it.legacynetwork.reports.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Tipo di evento registrato nello storico di un report.
 *
 * <p>Lo storico e' append-only: un evento sbagliato viene corretto da un evento
 * successivo, mai cancellato.</p>
 */
public enum ReportEventType {

    CREATED,
    CLAIMED,
    RELEASED,
    INVESTIGATION_STARTED,
    SCREENSHARE_STARTED,
    SCREENSHARE_ENDED,
    SCREENSHARE_CANCELLED,
    SCREENSHARE_FAILED,
    SCREENSHARE_VIOLATION,
    ACTION_TAKEN,
    DISMISSED,
    NOTE_ADDED;

    /**
     * Chiave di traduzione della riga di storico.
     */
    public String messageKey() {
        return "reports.audit." + name().toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }

    public static Optional<ReportEventType> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_');
        for (ReportEventType type : values()) {
            if (type.name().equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
