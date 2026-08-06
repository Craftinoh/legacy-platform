package it.legacynetwork.regions.listener;

import it.legacynetwork.regions.LegacyRegionsPlugin;
import it.legacynetwork.regions.model.RegionFlag;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RegionProtectionListener implements Listener {

    private static final long MESSAGE_COOLDOWN_MS = 3000L;

    private final LegacyRegionsPlugin plugin;
    private final Map<UUID, Map<RegionFlag, Long>> messageCooldowns =
            new HashMap<UUID, Map<RegionFlag, Long>>();

    public RegionProtectionListener(LegacyRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player, RegionFlag.BLOCK_BREAK)) {
            return;
        }
        if (!isAllowed(event.getBlock().getLocation(), RegionFlag.BLOCK_BREAK)) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.BLOCK_BREAK);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player, RegionFlag.BLOCK_PLACE)) {
            return;
        }
        if (!isAllowed(event.getBlock().getLocation(), RegionFlag.BLOCK_PLACE)) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.BLOCK_PLACE);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player, RegionFlag.INTERACT)) {
            return;
        }
        Location location = event.getClickedBlock() == null
                ? player.getLocation()
                : event.getClickedBlock().getLocation();
        if (!isAllowed(location, RegionFlag.INTERACT)) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.INTERACT);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        Player responsiblePlayer = getResponsiblePlayer(event.getDamager());
        Location location = victim.getLocation();

        if (event.getDamager() instanceof Projectile
                && !hasBypass(responsiblePlayer, RegionFlag.PROJECTILES)
                && !isAllowed(location, RegionFlag.PROJECTILES)) {
            event.setCancelled(true);
            if (responsiblePlayer != null) {
                sendDeniedMessage(responsiblePlayer, RegionFlag.PROJECTILES);
            }
            return;
        }

        if (responsiblePlayer != null && !responsiblePlayer.equals(victim)) {
            if (!hasBypass(responsiblePlayer, RegionFlag.PVP)
                    && !isAllowed(location, RegionFlag.PVP)) {
                event.setCancelled(true);
                sendDeniedMessage(responsiblePlayer, RegionFlag.PVP);
            }
            return;
        }

        if (!hasBypass(victim, RegionFlag.DAMAGE)
                && !isAllowed(location, RegionFlag.DAMAGE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)
                || event instanceof EntityDamageByEntityEvent) {
            return;
        }
        Player player = (Player) event.getEntity();
        RegionFlag flag = event.getCause() == EntityDamageEvent.DamageCause.FALL
                ? RegionFlag.FALL_DAMAGE
                : RegionFlag.DAMAGE;
        if (hasBypass(player, flag)) {
            return;
        }
        if (!isAllowed(player.getLocation(), flag)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (!hasBypass(player, RegionFlag.HUNGER)
                && !isAllowed(player.getLocation(), RegionFlag.HUNGER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!hasBypass(player, RegionFlag.ITEM_DROP)
                && !isAllowed(player.getLocation(), RegionFlag.ITEM_DROP)) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.ITEM_DROP);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!hasBypass(player, RegionFlag.ITEM_PICKUP)
                && !isAllowed(event.getItem().getLocation(), RegionFlag.ITEM_PICKUP)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        protectExplosion(event.getLocation(), event.blockList(),
                new Runnable() {
                    @Override
                    public void run() {
                        event.setCancelled(true);
                    }
                });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        protectExplosion(event.getBlock().getLocation(), event.blockList(),
                new Runnable() {
                    @Override
                    public void run() {
                        event.setCancelled(true);
                    }
                });
    }

    private void protectExplosion(Location origin, List<Block> affectedBlocks,
                                    Runnable cancelAction) {
        if (!isAllowed(origin, RegionFlag.EXPLOSIONS)) {
            cancelAction.run();
            return;
        }
        Iterator<Block> iterator = affectedBlocks.iterator();
        while (iterator.hasNext()) {
            if (!isAllowed(iterator.next().getLocation(), RegionFlag.EXPLOSIONS)) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!isAllowed(event.getBlock().getLocation(), RegionFlag.FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() != Material.FIRE
                && event.getBlock().getType() != Material.FIRE) {
            return;
        }
        if (!isAllowed(event.getBlock().getLocation(), RegionFlag.FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player, RegionFlag.FIRE_SPREAD)) {
            return;
        }
        if (!isAllowed(event.getBlock().getLocation(), RegionFlag.FIRE_SPREAD)) {
            event.setCancelled(true);
            if (player != null) {
                sendDeniedMessage(player, RegionFlag.FIRE_SPREAD);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isAllowed(event.getLocation(), RegionFlag.MOB_SPAWN)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Projectile)) {
            return;
        }
        Projectile projectile = (Projectile) event.getEntity();
        ProjectileSource source = projectile.getShooter();
        Player player = source instanceof Player ? (Player) source : null;
        if (hasBypass(player, RegionFlag.PROJECTILES)) {
            return;
        }
        if (!isAllowed(projectile.getLocation(), RegionFlag.PROJECTILES)) {
            event.setCancelled(true);
            if (player != null) {
                sendDeniedMessage(player, RegionFlag.PROJECTILES);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntered();
        if (!hasBypass(player, RegionFlag.VEHICLE_USE)
                && !isAllowed(event.getVehicle().getLocation(), RegionFlag.VEHICLE_USE)) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.VEHICLE_USE);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        messageCooldowns.remove(event.getPlayer().getUniqueId());
    }

    private Player getResponsiblePlayer(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            ProjectileSource source = ((Projectile) damager).getShooter();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        return null;
    }

    private boolean isAllowed(Location location, RegionFlag flag) {
        if (location == null) {
            return true;
        }
        World world = location.getWorld();
        if (world == null || plugin.getResolver() == null) {
            return true;
        }
        return plugin.getResolver().isAllowed(
                world.getName(), world.getUID().toString(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                flag);
    }

    private void sendDeniedMessage(Player player, RegionFlag flag) {
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<RegionFlag, Long> playerCooldowns =
                messageCooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            playerCooldowns = new HashMap<RegionFlag, Long>();
            messageCooldowns.put(player.getUniqueId(), playerCooldowns);
        }
        Long lastMessage = playerCooldowns.get(flag);
        if (lastMessage != null && now - lastMessage < MESSAGE_COOLDOWN_MS) {
            return;
        }
        playerCooldowns.put(flag, now);

        String message = plugin.getConfig().getString(
                "messages.denied", "&cNon puoi farlo in questa zona.");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private boolean hasBypass(Player player, RegionFlag flag) {
        if (player == null) {
            return false;
        }
        return player.hasPermission("legacyregions.admin")
                || player.hasPermission("legacyregions.bypass")
                || player.hasPermission("legacyregions.bypass."
                + flag.getPermissionKey());
    }
}
