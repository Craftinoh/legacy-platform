package it.legacynetwork.screenshare.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Stato di una sessione di controllo.
 *
 * <p>{@link #CREATED} e {@link #TRANSFERRING} sono momenti di passaggio;
 * {@link #ACTIVE} e' il controllo vero; gli altri quattro sono definitivi e
 * descrivono come e' finito.</p>
 */
public enum ScreenshareStatus {

    CREATED,
    TRANSFERRING,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    VIOLATION,
    FAILED;

    /**
     * Indica se la sessione e' conclusa.
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED || this == VIOLATION
                || this == FAILED;
    }

    /**
     * Indica se la sessione tiene il bersaglio legato al server di controllo.
     */
    public boolean isLocking() {
        return this == CREATED || this == TRANSFERRING || this == ACTIVE;
    }

    public String messageKey() {
        return "screenshare.status." + name().toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }

    public static Optional<ScreenshareStatus> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_');
        for (ScreenshareStatus status : values()) {
            if (status.name().equals(normalized)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
