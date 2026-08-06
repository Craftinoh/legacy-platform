package it.legacynetwork.regions.listener;

import it.legacynetwork.regions.LegacyRegionsPlugin;
import it.legacynetwork.regions.message.RegionMessageService;
import it.legacynetwork.regions.model.RegionDecision;
import it.legacynetwork.regions.model.RegionFlag;
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

import java.util.Iterator;
import java.util.List;

public final class RegionProtectionListener implements Listener {

    private final LegacyRegionsPlugin plugin;
    private final RegionMessageService messageService;

    public RegionProtectionListener(LegacyRegionsPlugin plugin,
                                    RegionMessageService messageService) {
        this.plugin = plugin;
        this.messageService = messageService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player, RegionFlag.BLOCK_BREAK)) {
            return;
        }
        Location location = event.getBlock().getLocation();
        RegionDecision decision = resolve(location, RegionFlag.BLOCK_BREAK);
        if (!decision.isAllowed()) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.BLOCK_BREAK, decision);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player, RegionFlag.BLOCK_PLACE)) {
            return;
        }
        Location location = event.getBlock().getLocation();
        RegionDecision decision = resolve(location, RegionFlag.BLOCK_PLACE);
        if (!decision.isAllowed()) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.BLOCK_PLACE, decision);
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
        RegionDecision decision = resolve(location, RegionFlag.INTERACT);
        if (!decision.isAllowed()) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.INTERACT, decision);
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
                && !hasBypass(responsiblePlayer, RegionFlag.PROJECTILES)) {
            RegionDecision projectileDecision =
                    resolve(location, RegionFlag.PROJECTILES);
            if (!projectileDecision.isAllowed()) {
                event.setCancelled(true);
                sendDeniedMessage(responsiblePlayer,
                        RegionFlag.PROJECTILES, projectileDecision);
                return;
            }
        }

        if (responsiblePlayer != null && !responsiblePlayer.equals(victim)) {
            if (!hasBypass(responsiblePlayer, RegionFlag.PVP)) {
                RegionDecision pvpDecision = resolve(location, RegionFlag.PVP);
                if (!pvpDecision.isAllowed()) {
                    event.setCancelled(true);
                    sendDeniedMessage(responsiblePlayer,
                            RegionFlag.PVP, pvpDecision);
                }
            }
            return;
        }

        if (!hasBypass(victim, RegionFlag.DAMAGE)) {
            RegionDecision damageDecision =
                    resolve(location, RegionFlag.DAMAGE);
            if (!damageDecision.isAllowed()) {
                event.setCancelled(true);
                sendDeniedMessage(victim,
                        RegionFlag.DAMAGE, damageDecision);
            }
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
        RegionDecision decision = resolve(player.getLocation(), flag);
        if (!decision.isAllowed()) {
            event.setCancelled(true);
            sendDeniedMessage(player, flag, decision);
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
        RegionDecision decision =
                resolve(player.getLocation(), RegionFlag.HUNGER);
        if (!decision.isAllowed()) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.HUNGER, decision);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player, RegionFlag.ITEM_DROP)) {
            return;
        }
        RegionDecision decision =
                resolve(player.getLocation(), RegionFlag.ITEM_DROP);
        if (!decision.isAllowed()) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.ITEM_DROP, decision);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player, RegionFlag.ITEM_PICKUP)) {
            return;
        }
        RegionDecision decision =
                resolve(event.getItem().getLocation(), RegionFlag.ITEM_PICKUP);
        if (!decision.isAllowed()) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.ITEM_PICKUP, decision);
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
        if (!resolve(origin, RegionFlag.EXPLOSIONS).isAllowed()) {
            cancelAction.run();
            return;
        }
        Iterator<Block> iterator = affectedBlocks.iterator();
        while (iterator.hasNext()) {
            if (!resolve(iterator.next().getLocation(),
                    RegionFlag.EXPLOSIONS).isAllowed()) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!resolve(event.getBlock().getLocation(),
                RegionFlag.FIRE_SPREAD).isAllowed()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() != Material.FIRE
                && event.getBlock().getType() != Material.FIRE) {
            return;
        }
        if (!resolve(event.getBlock().getLocation(),
                RegionFlag.FIRE_SPREAD).isAllowed()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player, RegionFlag.FIRE_SPREAD)) {
            return;
        }
        RegionDecision decision =
                resolve(event.getBlock().getLocation(), RegionFlag.FIRE_SPREAD);
        if (!decision.isAllowed()) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.FIRE_SPREAD, decision);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!resolve(event.getLocation(), RegionFlag.MOB_SPAWN).isAllowed()) {
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
        RegionDecision decision =
                resolve(projectile.getLocation(), RegionFlag.PROJECTILES);
        if (!decision.isAllowed()) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.PROJECTILES, decision);
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
        RegionDecision decision =
                resolve(event.getVehicle().getLocation(), RegionFlag.VEHICLE_USE);
        if (!decision.isAllowed()) {
            event.setCancelled(true);
            sendDeniedMessage(player, RegionFlag.VEHICLE_USE, decision);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        messageService.clearPlayer(event.getPlayer().getUniqueId());
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

    private RegionDecision resolve(Location location, RegionFlag flag) {
        if (location == null) {
            return RegionDecision.allowed(null, 0, flag);
        }
        World world = location.getWorld();
        if (world == null || plugin.getResolver() == null) {
            return RegionDecision.allowed(null, 0, flag);
        }
        return plugin.getResolver().resolveEffective(
                world.getName(), world.getUID().toString(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                flag);
    }

    private void sendDeniedMessage(Player player, RegionFlag flag,
                                   RegionDecision decision) {
        messageService.sendDenied(player, flag, decision);
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
