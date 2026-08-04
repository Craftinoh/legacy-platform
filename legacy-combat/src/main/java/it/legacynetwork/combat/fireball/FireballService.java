package it.legacynetwork.combat.fireball;

import it.legacynetwork.combat.LegacyCombatPlugin;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class FireballService {

    private final LegacyCombatPlugin plugin;

    public FireballService(LegacyCombatPlugin plugin) {
        this.plugin = plugin;
    }

    public Fireball launchFireball(Player player, double speed, double damage,
                                    double explosionPower,
                                    boolean blockDamage, boolean fire,
                                    int immunityTicks) {
        Vector direction = player.getLocation().getDirection().normalize();
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setVelocity(direction.multiply(speed));
        fireball.setIsIncendiary(fire);
        fireball.setYield((float) explosionPower);

        FireballTracker.FireballData data = new FireballTracker.FireballData(
                player.getUniqueId(), System.currentTimeMillis(),
                damage, explosionPower, blockDamage, fire, immunityTicks);
        plugin.getFireballTracker().register(fireball.getUniqueId(), data);

        if (plugin.isDebug()) {
            plugin.getLogger().info("Fireball lanciata da "
                    + player.getName() + " speed=" + speed
                    + " damage=" + damage);
        }

        return fireball;
    }
}
