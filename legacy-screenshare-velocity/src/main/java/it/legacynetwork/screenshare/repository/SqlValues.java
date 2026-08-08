package it.legacynetwork.screenshare.repository;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Conversioni fra il dominio e le colonne portabili dello schema.
 */
final class SqlValues {

    private SqlValues() {
    }

    static String uuid(UUID value) {
        return value == null ? null
                : value.toString().toLowerCase(Locale.ROOT).replace("-", "");
    }

    static UUID uuid(String compact) {
        if (compact == null) {
            return null;
        }
        String hex = compact.trim().toLowerCase(Locale.ROOT).replace("-", "");
        if (hex.length() != 32) {
            return null;
        }
        return UUID.fromString(hex.substring(0, 8) + '-' + hex.substring(8, 12)
                + '-' + hex.substring(12, 16) + '-' + hex.substring(16, 20)
                + '-' + hex.substring(20));
    }

    static long millis(Instant instant) {
        return instant == null ? 0L : instant.toEpochMilli();
    }

    static Instant instant(long millis) {
        return Instant.ofEpochMilli(millis);
    }

    /**
     * Istante opzionale: sullo schema {@code 0} significa "mai".
     */
    static Instant nullableInstant(long millis) {
        return millis == 0L ? null : Instant.ofEpochMilli(millis);
    }
}
