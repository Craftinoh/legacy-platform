package it.legacynetwork.reports.repository;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Conversioni fra il dominio e le colonne portabili dello schema.
 *
 * <p>UUID come esadecimale compatto e istanti come epoch millis UTC: due scelte
 * che tengono lo stesso SQL valido su motori diversi ed evitano qualunque
 * dipendenza dal fuso orario del processo.</p>
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

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
