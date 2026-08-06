package it.legacynetwork.lobby.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class VoidTeleportService {
    private final JavaPlugin plugin;
    private boolean enabled;
    private int belowY;
    private String target;
    private String fallback;
    private int checkTicks;
    private BukkitTask task;

    public VoidTeleportService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void configure(boolean enabled, int belowY, String target,
                           String fallback, int checkTicks) {
        stop();
        this.enabled = enabled;
        this.belowY = belowY;
        this.target = target;
        this.fallback = fallback;
        this.checkTicks = Math.max(1, checkTicks);
        if (enabled) {
            start();
        }
    }

    private void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::check, 0L, checkTicks);
    }

    private void check() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            if (player.getLocation().getY() >= belowY) {
                continue;
            }
            Location destination = resolveDestination(player);
            if (destination != null) {
                player.teleport(destination);
            }
        }
    }

    private Location resolveDestination(Player player) {
        if ("AUTHME".equalsIgnoreCase(target)) {
            Location authmeSpawn = getAuthMeSpawn(player);
            if (authmeSpawn != null) {
                return authmeSpawn;
            }
        }
        if ("WORLD_SPAWN".equalsIgnoreCase(target)
                || "WORLD_SPAWN".equalsIgnoreCase(fallback)) {
            World world = player.getWorld();
            if (world != null) {
                return world.getSpawnLocation();
            }
        }
        if ("AUTHME".equalsIgnoreCase(fallback)) {
            return getAuthMeSpawn(player);
        }
        return null;
    }

    private Location getAuthMeSpawn(Player player) {
        try {
            org.bukkit.plugin.Plugin authMe =
                    Bukkit.getPluginManager().getPlugin("AuthMe");
            if (authMe == null || !authMe.isEnabled()) {
                return null;
            }
            Class<?> apiClass = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
            Object apiInstance = apiClass.getMethod("getInstance").invoke(null);
            return (Location) apiClass.getMethod("getSpawnLocation", Player.class)
                    .invoke(apiInstance, player);
        } catch (Exception e) {
            return null;
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
