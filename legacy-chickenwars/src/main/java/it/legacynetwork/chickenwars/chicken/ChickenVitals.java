package it.legacynetwork.chickenwars.chicken;

/**
 * Vita e scudo della Gallina Reale.
 *
 * <p>Classe volutamente priva di dipendenze Bukkit: contiene solo le regole
 * numeriche di danno, cura e rigenerazione, ed e' quindi verificabile con test
 * unitari puri.</p>
 *
 * <p>Non e' thread safe: viene manipolata unicamente dal thread principale.</p>
 */
public final class ChickenVitals {

    private final double maxHealth;
    private final double maxShield;

    private double health;
    private double shield;

    public ChickenVitals(double maxHealth, double maxShield) {
        if (maxHealth <= 0.0D) {
            throw new IllegalArgumentException(
                    "La vita massima deve essere maggiore di zero");
        }
        if (maxShield < 0.0D) {
            throw new IllegalArgumentException(
                    "Lo scudo massimo non puo' essere negativo");
        }
        this.maxHealth = maxHealth;
        this.maxShield = maxShield;
        this.health = maxHealth;
        this.shield = maxShield;
    }

    /**
     * Applica danno consumando prima lo scudo e poi la vita.
     *
     * <p>Valori nulli o negativi non producono alcun effetto, cosi' come i colpi
     * inferti a una gallina gia' morta.</p>
     *
     * @param amount danno richiesto
     * @return il dettaglio di quanto e' stato realmente applicato
     */
    public DamageOutcome applyDamage(double amount) {
        if (amount <= 0.0D || isDead()) {
            return new DamageOutcome(0.0D, 0.0D, false);
        }

        double absorbed = Math.min(shield, amount);
        shield -= absorbed;

        double remaining = amount - absorbed;
        double lost = Math.min(health, remaining);
        health -= lost;

        return new DamageOutcome(absorbed, lost, health <= 0.0D);
    }

    /**
     * Ripristina vita fino al massimo consentito.
     *
     * @param amount cura richiesta
     * @return la cura realmente applicata
     */
    public double heal(double amount) {
        if (amount <= 0.0D || isDead()) {
            return 0.0D;
        }
        double healed = Math.min(maxHealth - health, amount);
        health += healed;
        return healed;
    }

    /**
     * Ripristina scudo fino al massimo consentito.
     *
     * @param amount scudo richiesto
     * @return lo scudo realmente ripristinato
     */
    public double restoreShield(double amount) {
        if (amount <= 0.0D || isDead()) {
            return 0.0D;
        }
        double restored = Math.min(maxShield - shield, amount);
        shield += restored;
        return restored;
    }

    /**
     * Porta la vita a zero, azzerando anche lo scudo residuo.
     */
    public void kill() {
        health = 0.0D;
        shield = 0.0D;
    }

    /**
     * Lascia la gallina in vita con una quantita' minima di salute.
     *
     * <p>Usato dagli effetti che impediscono una singola morte.</p>
     *
     * @param amount vita da lasciare, limitata al massimo consentito
     */
    public void surviveWith(double amount) {
        health = Math.min(maxHealth, Math.max(0.1D, amount));
    }

    public boolean isDead() {
        return health <= 0.0D;
    }

    public boolean hasShield() {
        return shield > 0.0D;
    }

    public boolean isFullHealth() {
        return health >= maxHealth;
    }

    public boolean isFullShield() {
        return shield >= maxShield;
    }

    public double getHealth() {
        return health;
    }

    public double getShield() {
        return shield;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getMaxShield() {
        return maxShield;
    }

    /**
     * Rapporto vita corrente su vita massima, compreso tra 0 e 1.
     */
    public double getHealthRatio() {
        return health / maxHealth;
    }

    /**
     * Vita arrotondata per la visualizzazione in ologrammi e scoreboard.
     */
    public int getDisplayHealth() {
        return (int) Math.ceil(health);
    }

    /**
     * Scudo arrotondato per la visualizzazione in ologrammi e scoreboard.
     */
    public int getDisplayShield() {
        return (int) Math.ceil(shield);
    }

    @Override
    public String toString() {
        return "ChickenVitals{" + getDisplayHealth() + "/" + (int) maxHealth
                + ", scudo " + getDisplayShield() + "/" + (int) maxShield + '}';
    }
}
