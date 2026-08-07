package it.legacynetwork.chickenwars.chicken;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Impostazioni immutabili della Gallina Reale, caricate da {@code chickens.yml}.
 */
public final class ChickenSettings {

    private final String displayName;
    private final double health;
    private final double shield;
    private final double movementRadius;
    private final boolean canMove;
    private final boolean returnToNest;

    private final boolean healthRegenerationEnabled;
    private final long healthRegenerationDelayMillis;
    private final double healthRegenerationAmount;

    private final boolean shieldRegenerationEnabled;
    private final long shieldRegenerationDelayMillis;
    private final double shieldRegenerationAmount;

    private final boolean damageFromMelee;
    private final boolean damageFromProjectiles;
    private final boolean damageFromExplosions;
    private final boolean damageFromEnvironment;

    private final boolean hologramEnabled;
    private final double hologramHeight;
    private final List<String> hologramLines;

    private final boolean alertsEnabled;
    private final long alertCooldownMillis;

    private final boolean feedingEnabled;
    private final Material feedMaterial;
    private final double feedHealAmount;
    private final double feedShieldAmount;
    private final long feedCooldownMillis;

    private final boolean lightningOnDeath;
    private final int deathFeatherParticles;
    private final boolean lastFeatherEnabled;
    private final int lastFeatherDurationSeconds;
    private final List<String> lastFeatherEffects;

    private ChickenSettings(Builder builder) {
        this.displayName = builder.displayName;
        this.health = builder.health;
        this.shield = builder.shield;
        this.movementRadius = builder.movementRadius;
        this.canMove = builder.canMove;
        this.returnToNest = builder.returnToNest;
        this.healthRegenerationEnabled = builder.healthRegenerationEnabled;
        this.healthRegenerationDelayMillis = builder.healthRegenerationDelayMillis;
        this.healthRegenerationAmount = builder.healthRegenerationAmount;
        this.shieldRegenerationEnabled = builder.shieldRegenerationEnabled;
        this.shieldRegenerationDelayMillis = builder.shieldRegenerationDelayMillis;
        this.shieldRegenerationAmount = builder.shieldRegenerationAmount;
        this.damageFromMelee = builder.damageFromMelee;
        this.damageFromProjectiles = builder.damageFromProjectiles;
        this.damageFromExplosions = builder.damageFromExplosions;
        this.damageFromEnvironment = builder.damageFromEnvironment;
        this.hologramEnabled = builder.hologramEnabled;
        this.hologramHeight = builder.hologramHeight;
        this.hologramLines = Collections.unmodifiableList(
                new ArrayList<String>(builder.hologramLines));
        this.alertsEnabled = builder.alertsEnabled;
        this.alertCooldownMillis = builder.alertCooldownMillis;
        this.feedingEnabled = builder.feedingEnabled;
        this.feedMaterial = builder.feedMaterial;
        this.feedHealAmount = builder.feedHealAmount;
        this.feedShieldAmount = builder.feedShieldAmount;
        this.feedCooldownMillis = builder.feedCooldownMillis;
        this.lightningOnDeath = builder.lightningOnDeath;
        this.deathFeatherParticles = builder.deathFeatherParticles;
        this.lastFeatherEnabled = builder.lastFeatherEnabled;
        this.lastFeatherDurationSeconds = builder.lastFeatherDurationSeconds;
        this.lastFeatherEffects = Collections.unmodifiableList(
                new ArrayList<String>(builder.lastFeatherEffects));
    }

    /**
     * Costruisce le impostazioni leggendo una sezione di {@code chickens.yml}.
     *
     * <p>Ogni valore mancante ricade sul rispettivo default, cosi' che un file
     * parziale resti utilizzabile.</p>
     *
     * @param section sezione da leggere, eventualmente nulla
     * @return le impostazioni risultanti, mai nulle
     */
    public static ChickenSettings fromSection(ConfigurationSection section) {
        Builder builder = new Builder();
        if (section == null) {
            return builder.build();
        }

        builder.displayName = section.getString("display-name", builder.displayName);
        builder.health = Math.max(1.0D, section.getDouble("health", builder.health));
        builder.shield = Math.max(0.0D, section.getDouble("shield", builder.shield));
        builder.movementRadius = Math.max(0.0D,
                section.getDouble("movement-radius", builder.movementRadius));
        builder.canMove = section.getBoolean("can-move", builder.canMove);
        builder.returnToNest = section.getBoolean("return-to-nest", builder.returnToNest);

        ConfigurationSection healthRegen =
                section.getConfigurationSection("health-regeneration");
        if (healthRegen != null) {
            builder.healthRegenerationEnabled =
                    healthRegen.getBoolean("enabled", builder.healthRegenerationEnabled);
            builder.healthRegenerationDelayMillis = Math.max(0L,
                    healthRegen.getLong("delay-after-damage", 15L)) * 1000L;
            builder.healthRegenerationAmount = Math.max(0.0D,
                    healthRegen.getDouble("amount", builder.healthRegenerationAmount));
        }

        ConfigurationSection shieldRegen =
                section.getConfigurationSection("shield-regeneration");
        if (shieldRegen != null) {
            builder.shieldRegenerationEnabled =
                    shieldRegen.getBoolean("enabled", builder.shieldRegenerationEnabled);
            builder.shieldRegenerationDelayMillis = Math.max(0L,
                    shieldRegen.getLong("delay-after-damage", 10L)) * 1000L;
            builder.shieldRegenerationAmount = Math.max(0.0D,
                    shieldRegen.getDouble("amount", builder.shieldRegenerationAmount));
        }

        ConfigurationSection damage = section.getConfigurationSection("damage");
        if (damage != null) {
            builder.damageFromMelee =
                    damage.getBoolean("player-melee", builder.damageFromMelee);
            builder.damageFromProjectiles =
                    damage.getBoolean("projectiles", builder.damageFromProjectiles);
            builder.damageFromExplosions =
                    damage.getBoolean("explosions", builder.damageFromExplosions);
            builder.damageFromEnvironment =
                    damage.getBoolean("environment", builder.damageFromEnvironment);
        }

        ConfigurationSection hologram = section.getConfigurationSection("hologram");
        if (hologram != null) {
            builder.hologramEnabled =
                    hologram.getBoolean("enabled", builder.hologramEnabled);
            builder.hologramHeight =
                    hologram.getDouble("height", builder.hologramHeight);
            List<String> lines = hologram.getStringList("lines");
            if (lines != null && !lines.isEmpty()) {
                builder.hologramLines = lines;
            }
        }

        ConfigurationSection alerts = section.getConfigurationSection("alerts");
        if (alerts != null) {
            builder.alertsEnabled = alerts.getBoolean("enabled", builder.alertsEnabled);
            builder.alertCooldownMillis = Math.max(0L,
                    alerts.getLong("cooldown-seconds", 3L)) * 1000L;
        }

        ConfigurationSection feeding = section.getConfigurationSection("feeding");
        if (feeding != null) {
            builder.feedingEnabled =
                    feeding.getBoolean("enabled", builder.feedingEnabled);
            Material material = Material.matchMaterial(
                    String.valueOf(feeding.getString("material", "SEEDS"))
                            .toUpperCase(Locale.ROOT));
            if (material != null) {
                builder.feedMaterial = material;
            }
            builder.feedHealAmount = Math.max(0.0D,
                    feeding.getDouble("heal-amount", builder.feedHealAmount));
            builder.feedShieldAmount = Math.max(0.0D,
                    feeding.getDouble("shield-amount", builder.feedShieldAmount));
            builder.feedCooldownMillis = Math.max(0L,
                    feeding.getLong("cooldown-seconds", 2L)) * 1000L;
        }

        ConfigurationSection death = section.getConfigurationSection("death");
        if (death != null) {
            builder.lightningOnDeath =
                    death.getBoolean("lightning-effect", builder.lightningOnDeath);
            builder.deathFeatherParticles = Math.max(0,
                    death.getInt("feather-particles", builder.deathFeatherParticles));
        }

        ConfigurationSection lastFeather = section.getConfigurationSection("last-feather");
        if (lastFeather != null) {
            builder.lastFeatherEnabled =
                    lastFeather.getBoolean("enabled", builder.lastFeatherEnabled);
            builder.lastFeatherDurationSeconds = Math.max(1,
                    lastFeather.getInt("duration-seconds",
                            builder.lastFeatherDurationSeconds));
            List<String> effects = lastFeather.getStringList("effects");
            if (effects != null && !effects.isEmpty()) {
                builder.lastFeatherEffects = effects;
            }
        }

        return builder.build();
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getHealth() {
        return health;
    }

    public double getShield() {
        return shield;
    }

    public double getMovementRadius() {
        return movementRadius;
    }

    public boolean canMove() {
        return canMove;
    }

    public boolean shouldReturnToNest() {
        return returnToNest;
    }

    public boolean isHealthRegenerationEnabled() {
        return healthRegenerationEnabled;
    }

    public long getHealthRegenerationDelayMillis() {
        return healthRegenerationDelayMillis;
    }

    public double getHealthRegenerationAmount() {
        return healthRegenerationAmount;
    }

    public boolean isShieldRegenerationEnabled() {
        return shieldRegenerationEnabled;
    }

    public long getShieldRegenerationDelayMillis() {
        return shieldRegenerationDelayMillis;
    }

    public double getShieldRegenerationAmount() {
        return shieldRegenerationAmount;
    }

    public boolean isDamageFromMelee() {
        return damageFromMelee;
    }

    public boolean isDamageFromProjectiles() {
        return damageFromProjectiles;
    }

    public boolean isDamageFromExplosions() {
        return damageFromExplosions;
    }

    public boolean isDamageFromEnvironment() {
        return damageFromEnvironment;
    }

    public boolean isHologramEnabled() {
        return hologramEnabled;
    }

    public double getHologramHeight() {
        return hologramHeight;
    }

    public List<String> getHologramLines() {
        return hologramLines;
    }

    public boolean isAlertsEnabled() {
        return alertsEnabled;
    }

    public long getAlertCooldownMillis() {
        return alertCooldownMillis;
    }

    public boolean isFeedingEnabled() {
        return feedingEnabled;
    }

    /** Materiale accettato come mangime dalla Gallina Reale. */
    public Material getFeedMaterial() {
        return feedMaterial;
    }

    public double getFeedHealAmount() {
        return feedHealAmount;
    }

    public double getFeedShieldAmount() {
        return feedShieldAmount;
    }

    public long getFeedCooldownMillis() {
        return feedCooldownMillis;
    }

    public boolean isLightningOnDeath() {
        return lightningOnDeath;
    }

    public int getDeathFeatherParticles() {
        return deathFeatherParticles;
    }

    public boolean isLastFeatherEnabled() {
        return lastFeatherEnabled;
    }

    public int getLastFeatherDurationSeconds() {
        return lastFeatherDurationSeconds;
    }

    public List<String> getLastFeatherEffects() {
        return lastFeatherEffects;
    }

    /**
     * Valori predefiniti, usati anche quando {@code chickens.yml} e' incompleto.
     */
    private static final class Builder {

        private String displayName = "&e&lGallina Reale";
        private double health = 100.0D;
        private double shield = 25.0D;
        private double movementRadius = 4.0D;
        private boolean canMove = true;
        private boolean returnToNest = true;

        private boolean healthRegenerationEnabled = true;
        private long healthRegenerationDelayMillis = 15000L;
        private double healthRegenerationAmount = 1.0D;

        private boolean shieldRegenerationEnabled = true;
        private long shieldRegenerationDelayMillis = 10000L;
        private double shieldRegenerationAmount = 1.0D;

        private boolean damageFromMelee = true;
        private boolean damageFromProjectiles = true;
        private boolean damageFromExplosions = true;
        private boolean damageFromEnvironment = false;

        private boolean hologramEnabled = true;
        private double hologramHeight = 1.8D;
        private List<String> hologramLines = Arrays.asList(
                "{team_color}&lGallina di {team_name}",
                "&c{health}/{max_health} &b{shield}/{max_shield}");

        private boolean alertsEnabled = true;
        private long alertCooldownMillis = 3000L;

        private boolean feedingEnabled = true;
        private Material feedMaterial = Material.SEEDS;
        private double feedHealAmount = 5.0D;
        private double feedShieldAmount = 2.0D;
        private long feedCooldownMillis = 2000L;

        private boolean lightningOnDeath = true;
        private int deathFeatherParticles = 80;
        private boolean lastFeatherEnabled = true;
        private int lastFeatherDurationSeconds = 10;
        private List<String> lastFeatherEffects = Arrays.asList(
                "SPEED:1", "INCREASE_DAMAGE:0");

        private ChickenSettings build() {
            return new ChickenSettings(this);
        }
    }
}
