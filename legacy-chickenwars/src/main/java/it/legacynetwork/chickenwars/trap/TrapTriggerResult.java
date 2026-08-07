package it.legacynetwork.chickenwars.trap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Esito osservabile di un tentativo di attivazione di una trappola.
 */
public final class TrapTriggerResult {

    /** Tipologie di esito. */
    public enum Type {
        /** L'ingresso non riguarda una trappola: nulla e' cambiato. */
        NOT_ELIGIBLE,
        /** Nessuna trappola in coda. */
        NO_TRAP,
        /** Trappola consumata e applicata. */
        TRIGGERED,
        /** Trappola consumata ma nessun effetto applicabile. */
        FAILED
    }

    private static final TrapTriggerResult NOT_ELIGIBLE =
            new TrapTriggerResult(Type.NOT_ELIGIBLE, null, 0, 0, false, 0, 0,
                    Collections.<UUID>emptyList());
    private static final TrapTriggerResult NO_TRAP =
            new TrapTriggerResult(Type.NO_TRAP, null, 0, 0, false, 0, 0,
                    Collections.<UUID>emptyList());

    private final Type type;
    private final String trapId;
    private final int intruderEffects;
    private final int defenderEffects;
    private final boolean invisibilityRevealed;
    private final int defenderDurationTicks;
    private final int remainingTraps;
    private final List<UUID> affectedDefenders;

    private TrapTriggerResult(Type type, String trapId, int intruderEffects,
                              int defenderEffects, boolean invisibilityRevealed,
                              int defenderDurationTicks, int remainingTraps,
                              List<UUID> affectedDefenders) {
        this.type = type;
        this.trapId = trapId;
        this.intruderEffects = intruderEffects;
        this.defenderEffects = defenderEffects;
        this.invisibilityRevealed = invisibilityRevealed;
        this.defenderDurationTicks = defenderDurationTicks;
        this.remainingTraps = remainingTraps;
        this.affectedDefenders = Collections.unmodifiableList(
                new ArrayList<UUID>(affectedDefenders));
    }

    static TrapTriggerResult notEligible() {
        return NOT_ELIGIBLE;
    }

    static TrapTriggerResult noTrap() {
        return NO_TRAP;
    }

    static TrapTriggerResult of(Type type, String trapId, int intruderEffects,
                                int defenderEffects,
                                boolean invisibilityRevealed,
                                int defenderDurationTicks, int remainingTraps,
                                List<UUID> affectedDefenders) {
        return new TrapTriggerResult(type, trapId, intruderEffects,
                defenderEffects, invisibilityRevealed, defenderDurationTicks,
                remainingTraps, affectedDefenders);
    }

    public Type getType() {
        return type;
    }

    /**
     * @return la trappola coinvolta, oppure {@code null} se nessuna
     */
    public String getTrapId() {
        return trapId;
    }

    /** Effetti realmente applicati all'intruso. */
    public int getIntruderEffects() {
        return intruderEffects;
    }

    /** Effetti realmente applicati ai difensori. */
    public int getDefenderEffects() {
        return defenderEffects;
    }

    /** Indica se l'intruso e' stato reso visibile. */
    public boolean isInvisibilityRevealed() {
        return invisibilityRevealed;
    }

    /**
     * Durata usata per i difensori, comprensiva del bonus Royal Guard.
     */
    public int getDefenderDurationTicks() {
        return defenderDurationTicks;
    }

    /** Trappole ancora in coda dopo l'attivazione. */
    public int getRemainingTraps() {
        return remainingTraps;
    }

    /** Difensori che hanno ricevuto almeno un effetto. */
    public List<UUID> getAffectedDefenders() {
        return affectedDefenders;
    }

    public boolean isTriggered() {
        return type == Type.TRIGGERED;
    }

    @Override
    public String toString() {
        return "TrapTriggerResult{" + type + ", trap=" + trapId
                + ", intruso=" + intruderEffects
                + ", difensori=" + defenderEffects
                + ", visibile=" + invisibilityRevealed + '}';
    }
}
