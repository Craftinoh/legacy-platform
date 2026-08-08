package it.legacynetwork.reports.api;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Identificatore di un report.
 *
 * <p>Il valore reale e' un UUID, stabile fra proxy e database. Sul database e
 * nei messaggi viene usata la forma compatta di 32 caratteri esadecimali: e'
 * quella che rende possibile cercare per prefisso, perche' nessuno digita un
 * UUID completo in chat. La forma breve non e' un secondo identificatore, e' un
 * prefisso dello stesso valore.</p>
 */
public final class ReportId {

    /** Lunghezza del prefisso mostrato allo staff. */
    public static final int SHORT_LENGTH = 8;

    private final UUID value;

    private ReportId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("Identificatore report mancante");
        }
        this.value = value;
    }

    public static ReportId of(UUID value) {
        return new ReportId(value);
    }

    public static ReportId random() {
        return new ReportId(UUID.randomUUID());
    }

    /**
     * Interpreta un identificatore completo, con o senza trattini.
     *
     * @param raw testo scritto dall'utente o letto dal database
     * @return l'identificatore, oppure vuoto se non e' un UUID
     */
    public static Optional<ReportId> parse(String raw) {
        Optional<String> normalized = normalizeReference(raw);
        if (!normalized.isPresent() || normalized.get().length() != 32) {
            return Optional.empty();
        }
        String hex = normalized.get();
        String dashed = hex.substring(0, 8) + '-' + hex.substring(8, 12) + '-'
                + hex.substring(12, 16) + '-' + hex.substring(16, 20) + '-'
                + hex.substring(20);
        try {
            return Optional.of(new ReportId(UUID.fromString(dashed)));
        } catch (IllegalArgumentException notAnUuid) {
            return Optional.empty();
        }
    }

    /**
     * Normalizza un riferimento digitato dallo staff.
     *
     * <p>Accetta sia l'UUID completo sia un prefisso: il risultato e' in
     * minuscolo e senza trattini, cosi' com'e' memorizzato.</p>
     *
     * @param raw testo scritto dall'utente
     * @return il riferimento normalizzato, oppure vuoto se inutilizzabile
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
     * Forma memorizzata: 32 caratteri esadecimali minuscoli, senza trattini.
     */
    public String storageValue() {
        return value.toString().toLowerCase(Locale.ROOT).replace("-", "");
    }

    /**
     * Prefisso mostrato allo staff.
     */
    public String shortCode() {
        return storageValue().substring(0, SHORT_LENGTH);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReportId)) {
            return false;
        }
        return value.equals(((ReportId) other).value);
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
