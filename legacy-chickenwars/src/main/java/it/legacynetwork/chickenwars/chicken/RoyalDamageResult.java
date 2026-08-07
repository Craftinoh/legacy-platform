package it.legacynetwork.chickenwars.chicken;

/**
 * Esito osservabile di un tentativo di danneggiare la Gallina Reale.
 */
public final class RoyalDamageResult {

    /** Tipologie di esito. */
    public enum Type {
        /** Il colpo non e' valido: nulla e' cambiato. */
        IGNORED,
        /** Danno applicato, la gallina resta viva. */
        DAMAGED,
        /** Il colpo ha portato la salute a zero. */
        DEFEATED
    }

    private static final RoyalDamageResult NOTHING =
            new RoyalDamageResult(Type.IGNORED, 0.0D, 0.0D, null);

    private final Type type;
    private final double rawDamage;
    private final double appliedDamage;
    private final DamageOutcome outcome;

    private RoyalDamageResult(Type type, double rawDamage,
                              double appliedDamage, DamageOutcome outcome) {
        this.type = type;
        this.rawDamage = rawDamage;
        this.appliedDamage = appliedDamage;
        this.outcome = outcome;
    }

    static RoyalDamageResult ignored() {
        return NOTHING;
    }

    static RoyalDamageResult of(Type type, double rawDamage,
                                double appliedDamage, DamageOutcome outcome) {
        return new RoyalDamageResult(type, rawDamage, appliedDamage, outcome);
    }

    public Type getType() {
        return type;
    }

    /** Danno richiesto prima della riduzione. */
    public double getRawDamage() {
        return rawDamage;
    }

    /** Danno realmente applicato dopo la riduzione dell'armatura. */
    public double getAppliedDamage() {
        return appliedDamage;
    }

    /**
     * @return il dettaglio scudo/vita, oppure {@code null} se ignorato
     */
    public DamageOutcome getOutcome() {
        return outcome;
    }

    public boolean isIgnored() {
        return type == Type.IGNORED;
    }

    public boolean isDefeated() {
        return type == Type.DEFEATED;
    }

    @Override
    public String toString() {
        return "RoyalDamageResult{" + type + ", raw=" + rawDamage
                + ", applied=" + appliedDamage + '}';
    }
}
