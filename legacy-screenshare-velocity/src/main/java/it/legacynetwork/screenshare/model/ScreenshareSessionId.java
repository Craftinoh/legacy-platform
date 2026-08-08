package it.legacynetwork.screenshare.model;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Identificatore di una sessione di controllo.
 *
 * <p>E' un UUID, perche' e' anche il valore che LegacyReports memorizza come
 * sessione collegata al report. Allo staff se ne mostra il prefisso: nessuno
 * digita un UUID in chat.</p>
 */
public final class ScreenshareSessionId {

    /** Lunghezza del prefisso mostrato allo staff. */
    public static final int SHORT_LENGTH = 8;

    private final UUID value;

    private ScreenshareSessionId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("Identificatore sessione mancante");
        }
        this.value = value;
    }

    public static ScreenshareSessionId of(UUID value) {
        return new ScreenshareSessionId(value);
    }

    public static ScreenshareSessionId random() {
        return new ScreenshareSessionId(UUID.randomUUID());
    }

    public static Optional<ScreenshareSessionId> parse(String raw) {
        Optional<String> normalized = normalizeReference(raw);
        if (!normalized.isPresent() || normalized.get().length() != 32) {
            return Optional.empty();
        }
        String hex = normalized.get();
        String dashed = hex.substring(0, 8) + '-' + hex.substring(8, 12) + '-'
                + hex.substring(12, 16) + '-' + hex.substring(16, 20) + '-'
                + hex.substring(20);
        try {
            return Optional.of(new ScreenshareSessionId(
                    UUID.fromString(dashed)));
        } catch (IllegalArgumentException notAnUuid) {
            return Optional.empty();
        }
    }

    /**
     * Normalizza un riferimento digitato: minuscolo, senza trattini.
     */
    public static Optional<String> normalizeReference(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String compact = raw.trim().toLowerCase(Locale.ROOT).replace("-", "");
        if (compact.isEmpty() || compact.length() > 32) {
            return Optional.empty();
        }
        for (int index = 0; index < compact.length(); index++) {
            if (Character.digit(compact.charAt(index), 16) < 0) {
                return Optional.empty();
            }
        }
        return Optional.of(compact);
    }

    public UUID value() {
        return value;
    }

    /**
     * Forma memorizzata: 32 caratteri esadecimali minuscoli.
     */
    public String storageValue() {
        return value.toString().toLowerCase(Locale.ROOT).replace("-", "");
    }

    public String shortCode() {
        return storageValue().substring(0, SHORT_LENGTH);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ScreenshareSessionId)) {
            return false;
        }
        return value.equals(((ScreenshareSessionId) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString().toLowerCase(Locale.ROOT);
    }
}
