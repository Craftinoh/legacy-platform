package it.legacynetwork.combat.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class CombatConfig {

    private final boolean hitEnabled;
    private final String knockbackMode;
    private final double horizontalKnockback;
    private final double verticalKnockback;
    private final double verticalLimit;
    private final double sprintMultiplier;
    private final boolean fireballEnabled;
    private final double fireballSpeed;
    private final double fireballDamage;
    private final double fireballExplosionPower;
    private final boolean fireballBlockDamage;
    private final boolean fireballFire;
    private final int fireballShooterImmunityTicks;

    public CombatConfig(boolean hitEnabled, String knockbackMode,
                        double horizontalKnockback, double verticalKnockback,
                        double verticalLimit, double sprintMultiplier,
                        boolean fireballEnabled, double fireballSpeed,
                        double fireballDamage, double fireballExplosionPower,
                        boolean fireballBlockDamage, boolean fireballFire,
                        int fireballShooterImmunityTicks) {
        this.hitEnabled = hitEnabled;
        this.knockbackMode = knockbackMode;
        this.horizontalKnockback = horizontalKnockback;
        this.verticalKnockback = verticalKnockback;
        this.verticalLimit = verticalLimit;
        this.sprintMultiplier = sprintMultiplier;
        this.fireballEnabled = fireballEnabled;
        this.fireballSpeed = fireballSpeed;
        this.fireballDamage = fireballDamage;
        this.fireballExplosionPower = fireballExplosionPower;
        this.fireballBlockDamage = fireballBlockDamage;
        this.fireballFire = fireballFire;
        this.fireballShooterImmunityTicks = fireballShooterImmunityTicks;
    }

    public static CombatConfig from(FileConfiguration config) {
        return new CombatConfig(
                config.getBoolean("hit.enabled", false),
                config.getString("hit.knockback.mode", "VANILLA"),
                config.getDouble("hit.knockback.horizontal", 0.40),
                config.getDouble("hit.knockback.vertical", 0.36),
                config.getDouble("hit.knockback.vertical-limit", 0.40),
                config.getDouble("hit.knockback.sprint-multiplier", 1.0),
                config.getBoolean("fireball.enabled", false),
                config.getDouble("fireball.speed", 1.2),
                config.getDouble("fireball.damage", 4.0),
                config.getDouble("fireball.explosion-power", 2.0),
                config.getBoolean("fireball.block-damage", false),
                config.getBoolean("fireball.fire", false),
                config.getInt("fireball.shooter-immunity-ticks", 10));
    }

    public boolean isHitEnabled() {
        return hitEnabled;
    }

    public String getKnockbackMode() {
        return knockbackMode;
    }

    public double getHorizontalKnockback() {
        return horizontalKnockback;
    }

    public double getVerticalKnockback() {
        return verticalKnockback;
    }

    public double getVerticalLimit() {
        return verticalLimit;
    }

    public double getSprintMultiplier() {
        return sprintMultiplier;
    }

    public boolean isFireballEnabled() {
        return fireballEnabled;
    }

    public double getFireballSpeed() {
        return fireballSpeed;
    }

    public double getFireballDamage() {
        return fireballDamage;
    }

    public double getFireballExplosionPower() {
        return fireballExplosionPower;
    }

    public boolean isFireballBlockDamage() {
        return fireballBlockDamage;
    }

    public boolean isFireballFire() {
        return fireballFire;
    }

    public int getFireballShooterImmunityTicks() {
        return fireballShooterImmunityTicks;
    }
}
