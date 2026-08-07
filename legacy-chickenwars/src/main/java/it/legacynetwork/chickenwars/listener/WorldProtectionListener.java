package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.config.ChickenWarsConfig;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.model.ArenaState;
import it.legacynetwork.chickenwars.player.PlayerSession;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.Iterator;

/**
 * Protezioni del mondo di gioco.
 *
 * <p>Registra le modifiche per il ripristino della mappa e impedisce che la
 * struttura originale dell'arena venga alterata in modo permanente.</p>
 */
public final class WorldProtectionListener implements Listener {

    private final ArenaManager arenas;
    private final GameServices services;

    public WorldProtectionListener(ArenaManager arenas, GameServices services) {
        this.arenas = arenas;
        this.services = services;
    }

    /**
     * Consente la costruzione ai soli giocatori attivi, entro i limiti di quota.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = arenas.getGameOf(player);
        if (game == null) {
            if (isInsideAnyArena(event.getBlock().getLocation())
                    && !player.hasPermission("chickenwars.admin")) {
                event.setCancelled(true);
            }
            return;
        }

        ChickenWarsConfig config = services.getConfig();
        PlayerSession session = game.getSession(player.getUniqueId());
        if (game.getState() != ArenaState.IN_GAME || session == null
                || !session.getState().isActive() || !config.isBlockPlaceAllowed()) {
            event.setCancelled(true);
            return;
        }

        ArenaDefinition definition = game.getDefinition();
        Block block = event.getBlock();
        if (block.getY() > definition.getMaximumBuildY()) {
            event.setCancelled(true);
            services.getMessages().send(player, "world.build-limit",
                    "{limit}", String.valueOf(definition.getMaximumBuildY()));
            return;
        }
        if (!definition.contains(block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ())) {
            event.setCancelled(true);
            services.getMessages().send(player, "world.outside-arena");
            return;
        }

        game.getRestore().recordPlace(block);
    }

    /**
     * Permette di distruggere solo cio' che e' stato costruito in partita.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = arenas.getGameOf(player);
        if (game == null) {
            if (isInsideAnyArena(event.getBlock().getLocation())
                    && !player.hasPermission("chickenwars.admin")) {
                event.setCancelled(true);
            }
            return;
        }

        PlayerSession session = game.getSession(player.getUniqueId());
        if (game.getState() != ArenaState.IN_GAME || session == null
                || !session.getState().isActive()) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlock();
        if (services.getConfig().isBreakPlacedOnly()
                && !game.getRestore().isPlaced(block)) {
            event.setCancelled(true);
            services.getMessages().send(player, "world.protected-block");
            return;
        }

        game.getRestore().recordBreak(block);
    }

    /**
     * Limita le esplosioni ai blocchi effettivamente piazzati in partita.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Game game = findGameAt(event.getLocation());
        if (game == null) {
            return;
        }
        if (!services.getConfig().isExplosionsPlacedOnly()) {
            return;
        }
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (game.getRestore().isPlaced(block)) {
                game.getRestore().recordBreak(block);
            } else {
                iterator.remove();
            }
        }
    }

    /**
     * Blocca lo spawn naturale di creature all'interno delle arene.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        if (services.getConfig().isMobSpawning()) {
            return;
        }
        if (isInsideAnyArena(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * Impedisce la propagazione del fuoco quando disabilitata.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!services.getConfig().isFireSpread()
                && isInsideAnyArena(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * Impedisce che il fuoco consumi i blocchi originali dell'arena.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!services.getConfig().isFireSpread()
                && isInsideAnyArena(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * Mantiene il meteo stabile nei mondi delle arene.
     */
    @EventHandler(ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (services.getConfig().isWeatherCycle() || !event.toWeatherState()) {
            return;
        }
        for (ArenaDefinition definition : arenas.getDefinitions()) {
            if (event.getWorld().getName().equalsIgnoreCase(definition.getWorld())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean isInsideAnyArena(Location location) {
        return findArenaAt(location) != null;
    }

    private ArenaDefinition findArenaAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        for (ArenaDefinition definition : arenas.getDefinitions()) {
            if (definition.contains(location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ())) {
                return definition;
            }
        }
        return null;
    }

    private Game findGameAt(Location location) {
        ArenaDefinition definition = findArenaAt(location);
        return definition == null ? null : arenas.getGame(definition.getId());
    }
}
