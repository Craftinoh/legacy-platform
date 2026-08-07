package it.legacynetwork.chickenwars.chicken;

/**
 * Esito di un colpo inferto alla Gallina Reale.
 *
 * <p>Lo scudo viene sempre consumato prima della vita: {@link #getShieldAbsorbed()}
 * e {@link #getHealthLost()} sommati danno il danno realmente applicato, che puo'
 * essere inferiore al danno richiesto quando la gallina muore.</p>
 */
public final class DamageOutcome {

    private final double shieldAbsorbed;
    private final double healthLost;
    private final boolean fatal;

    public DamageOutcome(double shieldAbsorbed, double healthLost, boolean fatal) {
        this.shieldAbsorbed = shieldAbsorbed;
        this.healthLost = healthLost;
        this.fatal = fatal;
    }

    /** Quantita' di danno assorbita dallo scudo. */
    public double getShieldAbsorbed() {
        return shieldAbsorbed;
    }

    /** Quantita' di danno sottratta alla vita. */
    public double getHealthLost() {
        return healthLost;
    }

    /** Danno totale effettivamente applicato. */
    public double getTotalApplied() {
        return shieldAbsorbed + healthLost;
    }

    /** Indica se il colpo ha portato la vita a zero. */
    public boolean isFatal() {
        return fatal;
    }

    /** Indica se il colpo non ha prodotto alcun effetto. */
    public boolean isNoop() {
        return getTotalApplied() <= 0.0D;
    }

    @Override
    public String toString() {
        return "DamageOutcome{shield=" + shieldAbsorbed
                + ", health=" + healthLost
                + ", fatal=" + fatal + '}';
    }
}
