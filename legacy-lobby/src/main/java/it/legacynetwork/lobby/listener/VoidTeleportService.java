package it.legacynetwork.lobby.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.lang.reflect.Method;

public final class VoidTeleportService {
    private final JavaPlugin plugin;

    private boolean authMeIntegrationEnabled;
    private int belowY;
    private String target;
    private String fallback;
    private int checkTicks;
    private BukkitTask task;

    private Plugin authMePlugin;
    private Object authMeApi;
    private Method isAuthenticatedMethod;
    private Location authMeSpawn;
    private boolean authMeReady;

    public VoidTeleportService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void configure(boolean enabled,
                          boolean authMeIntegrationEnabled,
                          int belowY,
                          String target,
                          String fallback,
                          int checkTicks) {
        stop();
        this.authMeIntegrationEnabled = authMeIntegrationEnabled;
        this.belowY = belowY;
        this.target = normalizeTarget(target, "AUTHME");
        this.fallback = normalizeTarget(fallback, "WORLD_SPAWN");
        this.checkTicks = Math.max(5, checkTicks);
        initializeAuthMe();
        if (enabled) {
            start();
        }
    }

    private String normalizeTarget(String value, String fallbackValue) {
        if (value == null || value.trim().isEmpty()) {
            return fallbackValue;
        }
        return value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private void start() {
        task = Bukkit.getScheduler().runTaskTimer(
                plugin, new Runnable() {
                    @Override
                    public void run() {
                        check();
                    }
                }, checkTicks, checkTicks);
    }

    private void check() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.getLocation().getY() >= belowY) {
                continue;
            }
            if (!canTeleportAuthenticatedPlayer(player)) {
                continue;
            }
            Location destination = resolveDestination(player);
            if (destination == null || destination.getWorld() == null) {
                continue;
            }
            if (destination.getY() < belowY) {
                plugin.getLogger().warning(
                        "Void teleport ignorato: la destinazione e' sotto below-y.");
                stop();
                return;
            }
            player.teleport(destination);
        }
    }

    private boolean canTeleportAuthenticatedPlayer(Player player) {
        if (!authMeIntegrationEnabled || authMePlugin == null) {
            return true;
        }
        if (!authMeReady) {
            return false;
        }
        try {
            Object result = isAuthenticatedMethod.invoke(authMeApi, player);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning(
                    "Controllo autenticazione AuthMe fallito; void teleport sospeso.");
            authMeReady = false;
            return false;
        } catch (LinkageError error) {
            plugin.getLogger().warning(
                    "API AuthMe incompatibile; void teleport sospeso.");
            authMeReady = false;
            return false;
        }
    }

    private Location resolveDestination(Player player) {
        Location primary = resolveTarget(player, target);
        if (primary != null) {
            return primary;
        }
        return resolveTarget(player, fallback);
    }

    private Location resolveTarget(Player player, String destinationType) {
        if ("DISABLED".equals(destinationType)) {
            return null;
        }
        if ("AUTHME".equals(destinationType)) {
            if (!authMeIntegrationEnabled || authMeSpawn == null) {
                return null;
            }
            return authMeSpawn.clone();
        }
        if ("WORLD_SPAWN".equals(destinationType)) {
            World world = player.getWorld();
            return world == null ? null : world.getSpawnLocation();
        }
        return null;
    }

    private void initializeAuthMe() {
        authMePlugin = null;
        authMeApi = null;
        isAuthenticatedMethod = null;
        authMeSpawn = null;
        authMeReady = false;

        if (!authMeIntegrationEnabled) {
            return;
        }

        Plugin candidate = Bukkit.getPluginManager().getPlugin("AuthMe");
        if (candidate == null || !candidate.isEnabled()) {
            plugin.getLogger().warning(
                    "Integrazione AuthMe attiva, ma AuthMe non e' disponibile.");
            return;
        }
        authMePlugin = candidate;

        try {
            Class<?> apiClass = Class.forName(
                    "fr.xephi.authme.api.v3.AuthMeApi",
                    true,
                    candidate.getClass().getClassLoader());
            authMeApi = apiClass.getMethod("getInstance").invoke(null);
            isAuthenticatedMethod = apiClass.getMethod(
                    "isAuthenticated", Player.class);
            authMeReady = true;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning(
                    "API AuthMe v3 non disponibile: i giocatori non verranno spostati dal void.");
            authMeReady = false;
        } catch (LinkageError error) {
            plugin.getLogger().warning(
                    "API AuthMe incompatibile: i giocatori non verranno spostati dal void.");
            authMeReady = false;
        }

        authMeSpawn = loadAuthMeSpawn(candidate);
        if (authMeSpawn == null) {
            plugin.getLogger().warning(
                    "Spawn AuthMe non leggibile; verra' usato il fallback configurato.");
        }
    }

    private Location loadAuthMeSpawn(Plugin authMe) {
        File spawnFile = new File(authMe.getDataFolder(), "spawn.yml");
        if (!spawnFile.isFile()) {
            return null;
        }
        YamlConfiguration spawnConfig =
                YamlConfiguration.loadConfiguration(spawnFile);
        ConfigurationSection section =
                spawnConfig.getConfigurationSection("spawn");
        if (section == null) {
            return null;
        }
        String worldName = section.getString("world");
        if (worldName == null || worldName.trim().isEmpty()) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch"));
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
