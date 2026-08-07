package it.legacynetwork.chickenwars.chicken;

import java.util.UUID;

/**
 * Dati necessari a decidere se un colpo alla Gallina Reale e' valido.
 *
 * <p>Contiene solo identificatori e booleani gia' risolti: la decisione resta
 * quindi verificabile senza un server in esecuzione.</p>
 */
public final class RoyalDamageRequest {

    private final UUID attackerId;
    private final String ownerTeamId;
    private final String attackerTeamId;
    private final boolean gameRunning;
    private final boolean attackerPlaying;
    private final double rawDamage;
    private final double damageReduction;

    private RoyalDamageRequest(Builder builder) {
        this.attackerId = builder.attackerId;
        this.ownerTeamId = builder.ownerTeamId;
        this.attackerTeamId = builder.attackerTeamId;
        this.gameRunning = builder.gameRunning;
        this.attackerPlaying = builder.attackerPlaying;
        this.rawDamage = builder.rawDamage;
        this.damageReduction = builder.damageReduction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getAttackerId() {
        return attackerId;
    }

    public String getOwnerTeamId() {
        return ownerTeamId;
    }

    public String getAttackerTeamId() {
        return attackerTeamId;
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    /**
     * Indica se l'aggressore partecipa attivamente: esclude spettatori,
     * eliminati e chi non fa parte della partita.
     */
    public boolean isAttackerPlaying() {
        return attackerPlaying;
    }

    public double getRawDamage() {
        return rawDamage;
    }

    /**
     * Frazione di danno assorbita dall'armatura reale, fra 0 e 1.
     */
    public double getDamageReduction() {
        return damageReduction;
    }

    /**
     * Indica se l'aggressore appartiene alla squadra proprietaria.
     */
    public boolean isFriendly() {
        return ownerTeamId != null && ownerTeamId.equals(attackerTeamId);
    }

    /**
     * Costruttore incrementale del contesto di danno.
     */
    public static final class Builder {

        private UUID attackerId;
        private String ownerTeamId;
        private String attackerTeamId;
        private boolean gameRunning = true;
        private boolean attackerPlaying = true;
        private double rawDamage;
        private double damageReduction;

        public Builder attacker(UUID attackerId, String attackerTeamId) {
            this.attackerId = attackerId;
            this.attackerTeamId = attackerTeamId;
            return this;
        }

        public Builder owner(String ownerTeamId) {
            this.ownerTeamId = ownerTeamId;
            return this;
        }

        public Builder gameRunning(boolean gameRunning) {
            this.gameRunning = gameRunning;
            return this;
        }

        public Builder attackerPlaying(boolean attackerPlaying) {
            this.attackerPlaying = attackerPlaying;
            return this;
        }

        public Builder rawDamage(double rawDamage) {
            this.rawDamage = rawDamage;
            return this;
        }

        public Builder damageReduction(double damageReduction) {
            this.damageReduction = damageReduction;
            return this;
        }

        public RoyalDamageRequest build() {
            return new RoyalDamageRequest(this);
        }
    }
}
