package it.legacynetwork.combat.hit;

import it.legacynetwork.combat.LegacyCombatPlugin;
import it.legacynetwork.combat.config.CombatConfig;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public final class HitListener implements Listener {

    private final LegacyCombatPlugin plugin;

    public HitListener(LegacyCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Entity damager = event.getDamager();
        Player attacker = resolveAttacker(damager);

        if (attacker == null) {
            return;
        }

        CombatConfig config = plugin.getHitConfig();
        if (!config.isHitEnabled()) {
            return;
        }

        Projectile projectile = damager instanceof Projectile
                ? (Projectile) damager : null;
        boolean isDirectHit = projectile == null;

        String damageCause = event.getCause().name();
        double originalDamage = event.getDamage();
        double finalDamage = event.getFinalDamage();

        LegacyHitEvent hitEvent = new LegacyHitEvent(
                attacker, victim, damageCause, originalDamage,
                finalDamage, projectile, isDirectHit,
                System.currentTimeMillis());
        Bukkit.getPluginManager().callEvent(hitEvent);

        if (hitEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        event.setDamage(hitEvent.getFinalDamage());

        if ("CUSTOM".equalsIgnoreCase(config.getKnockbackMode())) {
            event.setCancelled(true);
            applyCustomKnockback(attacker, victim, config,
                    hitEvent.getFinalDamage());
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Arrow || damager instanceof Snowball
                || damager instanceof Egg || damager instanceof Fireball) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }

    private void applyCustomKnockback(Player attacker, Player victim,
                                       CombatConfig config, double damage) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            victim.damage(damage, attacker);

            Vector direction = victim.getLocation().toVector()
                    .subtract(attacker.getLocation().toVector())
                    .normalize();

            double horizontal = config.getHorizontalKnockback();
            double vertical = config.getVerticalKnockback();

            if (attacker.isSprinting()) {
                horizontal *= config.getSprintMultiplier();
            }

            double vy = vertical;
            if (vy > config.getVerticalLimit()) {
                vy = config.getVerticalLimit();
            }

            Vector knockback = new Vector(
                    direction.getX() * horizontal,
                    vy,
                    direction.getZ() * horizontal);

            victim.setVelocity(knockback);
        });
    }
}
