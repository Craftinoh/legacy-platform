package it.legacynetwork.combat.hit;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class LegacyHitEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player attacker;
    private final Player victim;
    private final String damageCause;
    private final double originalDamage;
    private double finalDamage;
    private final Projectile projectile;
    private final boolean isDirectHit;
    private final long timestamp;
    private boolean cancelled;

    public LegacyHitEvent(Player attacker, Player victim, String damageCause,
                          double originalDamage, double finalDamage,
                          Projectile projectile, boolean isDirectHit,
                          long timestamp) {
        this.attacker = attacker;
        this.victim = victim;
        this.damageCause = damageCause;
        this.originalDamage = originalDamage;
        this.finalDamage = finalDamage;
        this.projectile = projectile;
        this.isDirectHit = isDirectHit;
        this.timestamp = timestamp;
    }

    public Player getAttacker() {
        return attacker;
    }

    public Player getVictim() {
        return victim;
    }

    public String getDamageCause() {
        return damageCause;
    }

    public double getOriginalDamage() {
        return originalDamage;
    }

    public double getFinalDamage() {
        return finalDamage;
    }

    public void setFinalDamage(double finalDamage) {
        this.finalDamage = finalDamage;
    }

    public Projectile getProjectile() {
        return projectile;
    }

    public boolean isDirectHit() {
        return isDirectHit;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
