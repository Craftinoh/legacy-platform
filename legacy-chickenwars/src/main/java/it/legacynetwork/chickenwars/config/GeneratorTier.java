package it.legacynetwork.chickenwars.config;

/**
 * Parametri di un generatore a un determinato livello.
 */
public final class GeneratorTier {

    private final int intervalTicks;
    private final int amount;

    public GeneratorTier(int intervalTicks, int amount) {
        this.intervalTicks = Math.max(1, intervalTicks);
        this.amount = Math.max(1, amount);
    }

    /** Intervallo tra due generazioni, espresso in tick di server. */
    public int getIntervalTicks() {
        return intervalTicks;
    }

    /** Quantita' di risorsa prodotta a ogni generazione. */
    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "GeneratorTier{" + intervalTicks + " tick, x" + amount + '}';
    }
}
