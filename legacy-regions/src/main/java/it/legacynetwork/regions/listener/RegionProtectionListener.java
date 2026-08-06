package it.legacynetwork.regions.listener;

import it.legacynetwork.regions.LegacyRegionsPlugin;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionFlag;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RegionProtectionListener implements Listener {

    private final LegacyRegionsPlugin plugin;
    private final Map<UUID, Map<RegionFlag, Long>> messageCooldowns = new HashMap<UUID, Map<RegionFlag, Long>>();
    private static final long MESSAGE_COOLDOWN_MS = 3000L;

    public RegionProtectionListener(LegacyRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) {
            return;
        }
        Location loc = event.getBlock().getLocation();
        if (!checkFlag(loc, RegionFlag.BLOCK_BREAK, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) {
            return;
        }
        Location loc = event.getBlock().getLocation();
        if (!checkFlag(loc, RegionFlag.BLOCK_PLACE, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        if (hasBypass(attacker, RegionFlag.PVP)) {
            return;
        }
        Location loc = victim.getLocation();
        if (!plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), RegionFlag.PVP)) {
            event.setCancelled(true);
            sendDeniedMessage(attacker, RegionFlag.PVP);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (hasBypass(player, RegionFlag.FALL_DAMAGE)) {
            return;
        }
        Location loc = player.getLocation();
        if (!checkFlag(loc, RegionFlag.FALL_DAMAGE, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (hasBypass(player, RegionFlag.HUNGER)) {
            return;
        }
        Location loc = player.getLocation();
        if (!plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), RegionFlag.HUNGER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player, RegionFlag.ITEM_DROP)) {
            return;
        }
        Location loc = player.getLocation();
        if (!plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), RegionFlag.ITEM_DROP)) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.ITEM_DROP);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player, RegionFlag.ITEM_PICKUP)) {
            return;
        }
        Location loc = event.getItem().getLocation();
        if (!plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), RegionFlag.ITEM_PICKUP)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location loc = event.getLocation();
        if (!plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), RegionFlag.EXPLOSIONS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), RegionFlag.FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (player != null && hasBypass(player, RegionFlag.FIRE_SPREAD)) {
            return;
        }
        Location loc = event.getBlock().getLocation();
        if (!plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), RegionFlag.FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Location loc = event.getLocation();
        if (!plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), RegionFlag.MOB_SPAWN)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        ProjectileSource source = event.getEntity().getShooter();
        if (!(source instanceof Player)) {
            return;
        }
        Player player = (Player) source;
        if (hasBypass(player, RegionFlag.PROJECTILES)) {
            return;
        }
        Location loc = event.getEntity().getLocation();
        if (!plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), RegionFlag.PROJECTILES)) {
            event.getEntity().remove();
            sendDeniedMessage(player, RegionFlag.PROJECTILES);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntered();
        if (hasBypass(player, RegionFlag.VEHICLE_USE)) {
            return;
        }
        Location loc = event.getVehicle().getLocation();
        if (!plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), RegionFlag.VEHICLE_USE)) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.VEHICLE_USE);
        }
    }

    private boolean checkFlag(Location loc, RegionFlag flag, Player player) {
        if (plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), flag)) {
            return true;
        }
        if (flag.isSpecific()) {
            RegionFlag generalFlag = flag.getGeneralFlag();
            if (generalFlag != null && plugin.getResolver().isAllowed(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), generalFlag)) {
                return true;
            }
        }
        sendDeniedMessage(player, flag);
        return false;
    }

    private void sendDeniedMessage(Player player, RegionFlag flag) {
        long now = System.currentTimeMillis();
        Map<RegionFlag, Long> playerCooldowns = messageCooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            playerCooldowns = new HashMap<RegionFlag, Long>();
            messageCooldowns.put(player.getUniqueId(), playerCooldowns);
        }
        Long lastMessage = playerCooldowns.get(flag);
        if (lastMessage != null && (now - lastMessage) < MESSAGE_COOLDOWN_MS) {
            return;
        }
        playerCooldowns.put(flag, now);

        String deniedMessage = plugin.getConfig().getString("messages.denied", "&cNon puoi farlo in questa zona.");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', deniedMessage));
    }

    private boolean hasBypass(Player player) {
        return player.hasPermission("legacyregions.bypass") || player.hasPermission("legacyregions.admin");
    }

    private boolean hasBypass(Player player, RegionFlag flag) {
        if (hasBypass(player)) {
            return true;
        }
        return player.hasPermission("legacyregions.bypass." + flag.getPermissionKey());
    }
}
