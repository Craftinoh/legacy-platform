package it.legacynetwork.combat.fireball;

import it.legacynetwork.combat.LegacyCombatPlugin;
import it.legacynetwork.combat.config.CombatConfig;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.UUID;

public final class FireballListener implements Listener {

    private final LegacyCombatPlugin plugin;

    public FireballListener(LegacyCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Explosive)) {
            return;
        }

        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        Player shooter = (Player) projectile.getShooter();

        CombatConfig config = plugin.getFireballConfig();
        if (!config.isFireballEnabled()) {
            return;
        }

        FireballTracker.FireballData data = new FireballTracker.FireballData(
                shooter.getUniqueId(), System.currentTimeMillis(), config);
        plugin.getFireballTracker().register(projectile.getUniqueId(), data);

        if (plugin.isDebug()) {
            plugin.getLogger().info("Fireball registrato: "
                    + projectile.getUniqueId()
                    + " shooter=" + shooter.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        FireballTracker.FireballData data =
                plugin.getFireballTracker().find(damager.getUniqueId());
        if (data == null) {
            return;
        }

        Entity victim = event.getEntity();

        if (data.getShooterImmunityTicks() > 0
                && victim instanceof LivingEntity) {
            UUID victimUuid = victim.getUniqueId();
            if (victimUuid.equals(data.getShooterUuid())) {
                long elapsed =
                        System.currentTimeMillis() - data.getLaunchTime();
                if (elapsed < data.getShooterImmunityTicks() * 50L) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        event.setDamage(data.getDamage());

        if (plugin.isDebug()) {
            plugin.getLogger().info("Fireball ha colpito "
                    + victim.getType() + " danno=" + data.getDamage());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        FireballTracker.FireballData data =
                plugin.getFireballTracker().find(entity.getUniqueId());
        if (data == null) {
            return;
        }

        if (data.hasExploded()) {
            return;
        }

        data.setExploded(true);

        if (!data.isBlockDamage()) {
            event.blockList().clear();
        }

        event.setYield((float) data.getExplosionPower());

        if (plugin.isDebug()) {
            plugin.getLogger().info("Fireball esploso: "
                    + entity.getUniqueId()
                    + " blockDamage=" + data.isBlockDamage());
        }

        plugin.getFireballTracker().remove(entity.getUniqueId());
    }
}
