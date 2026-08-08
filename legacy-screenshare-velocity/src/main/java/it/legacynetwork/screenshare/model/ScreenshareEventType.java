package it.legacynetwork.screenshare.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Tipo di evento nello storico di una sessione.
 *
 * <p>Come per i report lo storico e' di sola aggiunta.</p>
 */
public enum ScreenshareEventType {

    CREATED,
    TRANSFER_STARTED,
    TRANSFER_FAILED,
    SESSION_ACTIVE,
    SERVER_SWITCH_BLOCKED,
    COMMAND_BLOCKED,
    TARGET_DISCONNECTED,
    STAFF_DISCONNECTED,
    STAFF_RECONNECTED,
    NOTE_ADDED,
    COMPLETED,
    CANCELLED,
    VIOLATION,
    FAILED,
    TIMED_OUT,
    RECOVERED;

    public String messageKey() {
        return "screenshare.audit." + name().toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }

    public static Optional<ScreenshareEventType> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_');
        for (ScreenshareEventType type : values()) {
            if (type.name().equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
