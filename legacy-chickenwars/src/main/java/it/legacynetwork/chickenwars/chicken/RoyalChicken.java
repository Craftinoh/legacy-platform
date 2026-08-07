package it.legacynetwork.chickenwars.chicken;

import it.legacynetwork.chickenwars.hologram.Hologram;
import it.legacynetwork.chickenwars.model.ChickenState;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Gallina Reale di una squadra: entita' viva che sostituisce il letto delle
 * BedWars e determina la possibilita' di respawn dei compagni.
 *
 * <p>La logica numerica risiede in {@link ChickenVitals}; questa classe collega
 * quei valori all'entita' Bukkit, al nido e all'ologramma.</p>
 */
public final class RoyalChicken {

    private final String teamId;
    private final SimpleLocation nest;
    private final ChickenVitals vitals;

    private ChickenState state = ChickenState.PROTECTED;
    private LivingEntity entity;
    private Hologram hologram;

    private long lastDamageMillis;
    private long lastAlertMillis;
    private long lastFeedMillis;
    private UUID lastAttacker;
    private UUID killer;
    private int appliedVitalityLevel;
    private boolean defeatDispatched;

    public RoyalChicken(String teamId, SimpleLocation nest,
                        ChickenSettings settings) {
        if (teamId == null) {
            throw new IllegalArgumentException("ID squadra mancante");
        }
        if (settings == null) {
            throw new IllegalArgumentException("Impostazioni gallina mancanti");
        }
        this.teamId = teamId;
        this.nest = nest;
        this.vitals = new ChickenVitals(settings.getHealth(), settings.getShield());
    }

    /**
     * Applica danno e aggiorna lo stato conseguente.
     *
     * <p>Nessun effetto viene prodotto se lo stato corrente non consente danno,
     * per esempio durante la protezione iniziale.</p>
     *
     * @param amount   danno richiesto
     * @param attacker autore del colpo, eventualmente nullo
     * @return il dettaglio del danno applicato
     */
    public DamageOutcome damage(double amount, UUID attacker) {
        if (!state.isDamageable()) {
            return new DamageOutcome(0.0D, 0.0D, false);
        }

        DamageOutcome outcome = vitals.applyDamage(amount);
        if (outcome.isNoop()) {
            return outcome;
        }

        this.lastDamageMillis = System.currentTimeMillis();
        if (attacker != null) {
            this.lastAttacker = attacker;
        }

        if (outcome.isFatal()) {
            this.killer = attacker;
            this.state = ChickenState.DEAD;
        } else if (vitals.hasShield()) {
            this.state = ChickenState.SHIELDED;
        } else {
            this.state = ChickenState.DAMAGED;
        }
        return outcome;
    }

    /**
     * Cura la gallina, riportandola a uno stato non danneggiato quando piena.
     *
     * @return la cura realmente applicata
     */
    public double heal(double amount) {
        double healed = vitals.heal(amount);
        if (healed > 0.0D && state != ChickenState.DEAD) {
            state = vitals.isFullHealth() ? ChickenState.NORMAL : ChickenState.HEALING;
        }
        return healed;
    }

    /**
     * Ripristina scudo.
     *
     * @return lo scudo realmente ripristinato
     */
    public double restoreShield(double amount) {
        return vitals.restoreShield(amount);
    }

    /**
     * Esegue la rigenerazione periodica prevista dalla configurazione.
     *
     * <p>La rigenerazione parte solo dopo il ritardo previsto dall'ultimo danno
     * subito, in modo distinto per vita e scudo.</p>
     *
     * @param settings impostazioni correnti
     * @return {@code true} se qualche valore e' cambiato
     */
    public boolean regenerate(ChickenSettings settings) {
        if (state == ChickenState.DEAD || settings == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - lastDamageMillis;
        boolean changed = false;

        if (settings.isShieldRegenerationEnabled()
                && elapsed >= settings.getShieldRegenerationDelayMillis()
                && !vitals.isFullShield()) {
            changed = vitals.restoreShield(
                    settings.getShieldRegenerationAmount()) > 0.0D || changed;
        }

        if (settings.isHealthRegenerationEnabled()
                && elapsed >= settings.getHealthRegenerationDelayMillis()
                && !vitals.isFullHealth()) {
            changed = vitals.heal(
                    settings.getHealthRegenerationAmount()) > 0.0D || changed;
        }

        if (changed && state != ChickenState.PROTECTED
                && state != ChickenState.INVULNERABLE) {
            state = vitals.isFullHealth() ? ChickenState.NORMAL : ChickenState.HEALING;
        }
        return changed;
    }

    /**
     * Verifica il cooldown degli avvisi di attacco.
     *
     * <p>Registra l'invio quando consentito, evitando lo spam di notifiche.</p>
     *
     * @param cooldownMillis intervallo minimo tra due avvisi
     * @return {@code true} se l'avviso puo' essere inviato ora
     */
    public boolean tryAlert(long cooldownMillis) {
        long now = System.currentTimeMillis();
        if (now - lastAlertMillis < cooldownMillis) {
            return false;
        }
        lastAlertMillis = now;
        return true;
    }

    /**
     * Verifica il cooldown dell'alimentazione.
     *
     * <p>Registra il pasto quando consentito, impedendo di curare la gallina a
     * ritmo illimitato.</p>
     *
     * @param cooldownMillis intervallo minimo tra due pasti
     * @return {@code true} se la gallina puo' essere nutrita ora
     */
    public boolean tryFeed(long cooldownMillis) {
        long now = System.currentTimeMillis();
        if (now - lastFeedMillis < cooldownMillis) {
            return false;
        }
        lastFeedMillis = now;
        return true;
    }

    /**
     * Livello di Royal Vitality gia' tradotto in salute massima.
     *
     * <p>Distinguere il livello acquistato da quello applicato e' cio' che
     * rende innocuo riaprire il menu: un livello viene convertito in salute una
     * sola volta.</p>
     */
    public int getAppliedVitalityLevel() {
        return appliedVitalityLevel;
    }

    public void setAppliedVitalityLevel(int appliedVitalityLevel) {
        this.appliedVitalityLevel = Math.max(0, appliedVitalityLevel);
    }

    /**
     * Segna la sconfitta come gia' notificata.
     *
     * <p>Eventi Bukkit duplicati riferiti allo stesso colpo fatale ottengono
     * {@code false}: il callback di dominio parte una volta sola.</p>
     *
     * @return {@code true} soltanto alla prima chiamata
     */
    public synchronized boolean markDefeated() {
        if (defeatDispatched) {
            return false;
        }
        defeatDispatched = true;
        return true;
    }

    /**
     * Indica se la sconfitta e' gia' stata notificata.
     */
    public synchronized boolean isDefeatDispatched() {
        return defeatDispatched;
    }

    /**
     * Termina la fase di protezione iniziale.
     */
    public void releaseProtection() {
        if (state == ChickenState.PROTECTED) {
            state = ChickenState.NORMAL;
        }
    }

    public boolean isAlive() {
        return state != ChickenState.DEAD && !vitals.isDead();
    }

    public String getTeamId() {
        return teamId;
    }

    public SimpleLocation getNest() {
        return nest;
    }

    public ChickenVitals getVitals() {
        return vitals;
    }

    public ChickenState getState() {
        return state;
    }

    public void setState(ChickenState state) {
        if (state != null) {
            this.state = state;
        }
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public void setEntity(LivingEntity entity) {
        this.entity = entity;
    }

    public Hologram getHologram() {
        return hologram;
    }

    public void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }

    /**
     * @return l'ultimo aggressore, oppure {@code null}
     */
    public UUID getLastAttacker() {
        return lastAttacker;
    }

    /**
     * @return chi ha inferto il colpo fatale, oppure {@code null}
     */
    public UUID getKiller() {
        return killer;
    }

    public long getLastDamageMillis() {
        return lastDamageMillis;
    }
}
