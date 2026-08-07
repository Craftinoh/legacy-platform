package it.legacynetwork.chickenwars.upgrade;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Effetto applicato da una trappola a un bersaglio.
 */
public final class TrapEffect {

    private final PotionEffectType type;
    private final int amplifier;
    private final int durationTicks;

    TrapEffect(PotionEffectType type, int amplifier, int durationTicks) {
        this.type = type;
        this.amplifier = Math.max(0, amplifier);
        this.durationTicks = Math.max(1, durationTicks);
    }

    public PotionEffectType getType() {
        return type;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    /**
     * Costruisce l'effetto Bukkit corrispondente.
     */
    public PotionEffect toPotionEffect() {
        return new PotionEffect(type, durationTicks, amplifier);
    }

    @Override
    public String toString() {
        return type.getName() + ":" + durationTicks + ":" + amplifier;
    }
}
