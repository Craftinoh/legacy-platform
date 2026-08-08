package it.legacynetwork.screenshare.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Esito di un controllo.
 *
 * <p>Solo una descrizione: nessun esito applica una punizione, perche' questo
 * plugin non ne conosce nessuna. La decisione resta al report e, quando
 * esistera', a LegacyPunishments.</p>
 */
public enum ScreenshareOutcome {

    /** Nessun problema riscontrato. */
    CLEAN(ScreenshareStatus.COMPLETED),
    /** Controllo concluso senza una risposta chiara. */
    INCONCLUSIVE(ScreenshareStatus.COMPLETED),
    /** Il giocatore si e' sottratto al controllo o e' stato trovato in fallo. */
    VIOLATION(ScreenshareStatus.VIOLATION),
    /** Controllo interrotto dallo staff. */
    CANCELLED(ScreenshareStatus.CANCELLED),
    /** Controllo non partito per un problema tecnico. */
    FAILED(ScreenshareStatus.FAILED);

    private final ScreenshareStatus status;

    ScreenshareOutcome(ScreenshareStatus status) {
        this.status = status;
    }

    /**
     * Stato finale che questo esito comporta.
     */
    public ScreenshareStatus terminalStatus() {
        return status;
    }

    public String messageKey() {
        return "screenshare.outcome." + name().toLowerCase(Locale.ROOT);
    }

    /**
     * Esiti che lo staff puo' indicare a mano con {@code /ss stop}.
     */
    public static List<ScreenshareOutcome> selectable() {
        List<ScreenshareOutcome> selectable = new ArrayList<>();
        selectable.add(CLEAN);
        selectable.add(INCONCLUSIVE);
        selectable.add(VIOLATION);
        return selectable;
    }

    public static Optional<ScreenshareOutcome> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (ScreenshareOutcome outcome : values()) {
            if (outcome.name().equals(normalized)) {
                return Optional.of(outcome);
            }
        }
        return Optional.empty();
    }
}
