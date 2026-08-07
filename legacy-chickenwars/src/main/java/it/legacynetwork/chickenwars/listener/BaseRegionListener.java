package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.arena.TeamDefinition;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.game.GameTeam;
import it.legacynetwork.chickenwars.model.ArenaState;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.player.PlayerState;
import it.legacynetwork.chickenwars.region.CuboidRegion;
import it.legacynetwork.chickenwars.trap.TrapTriggerRequest;
import it.legacynetwork.chickenwars.trap.TrapTriggerResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BaseRegionListener implements Listener {

    private final ArenaManager arenas;
    private final GameServices services;

    public BaseRegionListener(ArenaManager arenas, GameServices services) {
        this.arenas = arenas;
        this.services = services;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!hasChangedBlock(event.getFrom(), event.getTo())) {
            return;
        }
        evaluate(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        evaluate(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        evaluate(event.getPlayer(), event.getPlayer().getLocation());
    }

    private void evaluate(Player player, Location location) {
        if (player == null) {
            return;
        }
        Game game = arenas.getGameOf(player);
        if (game == null || game.getState() != ArenaState.IN_GAME) {
            return;
        }
        PlayerSession session = game.getSession(player.getUniqueId());
        if (session == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        String arenaId = game.getDefinition().getId();
        if (location == null || location.getWorld() == null) {
            return;
        }

        String ownTeamId = session.getTeamId();
        GameTeam ownTeam = game.getTeam(ownTeamId);
        boolean eligible = session.getState().isActive()
                && ownTeam != null && ownTeam.isAlive(playerId);

        handleHealPool(game, arenaId, ownTeamId, playerId, location, eligible);
        handleEnemyEntry(game, arenaId, playerId, location, session, eligible);
    }

    private void handleHealPool(Game game, String arenaId, String teamId,
                                UUID playerId, Location location,
                                boolean eligible) {
        GameTeam team = game.getTeam(teamId);
        boolean inside = isInsideOwnBase(team, location);
        services.getHealPool().update(arenaId, teamId, playerId, inside,
                eligible && inside);
    }

    private void handleEnemyEntry(Game game, String arenaId,
                                  UUID playerId, Location location,
                                  PlayerSession session, boolean eligible) {
        for (GameTeam team : game.getTeams()) {
            if (team.getId().equals(session.getTeamId())) {
                continue;
            }
            CuboidRegion region = team.getDefinition().getBaseRegion();
            if (region == null) {
                continue;
            }
            boolean within = region.contains(location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ());
            boolean entry = services.getBaseEntryTracker().update(
                    playerId, arenaId, team.getId(), within);
            if (!entry) {
                continue;
            }
            if (!eligible) {
                continue;
            }
            triggerTrap(game, arenaId, team, playerId, session);
        }
    }

    private void triggerTrap(Game game, String arenaId, GameTeam ownerTeam,
                             UUID intruderId, PlayerSession intruderSession) {
        List<UUID> defenders = new ArrayList<UUID>();
        for (UUID member : ownerTeam.getAliveMembers()) {
            Player defender = Bukkit.getPlayer(member);
            if (defender == null || !defender.isOnline()) {
                continue;
            }
            if (defender.getLocation() == null
                    || defender.getLocation().getWorld() == null) {
                continue;
            }
            CuboidRegion region = ownerTeam.getDefinition().getBaseRegion();
            if (region == null) {
                continue;
            }
            if (region.contains(defender.getLocation().getWorld().getName(),
                    defender.getLocation().getX(),
                    defender.getLocation().getY(),
                    defender.getLocation().getZ())) {
                defenders.add(member);
            }
        }

        TrapTriggerRequest request = TrapTriggerRequest.builder()
                .base(arenaId, ownerTeam.getId())
                .intruder(intruderId, intruderSession.getTeamId())
                .gameRunning(game.getState() == ArenaState.IN_GAME)
                .intruderEligible(true)
                .defenders(defenders)
                .build();

        TrapTriggerResult result = services.getTraps().trigger(request);
        if (!result.isTriggered()) {
            return;
        }

        Player intruder = Bukkit.getPlayer(intruderId);
        if (intruder != null && intruder.isOnline()) {
            services.getMessages().send(intruder, "trap.triggered",
                    "{intruder}", intruder.getName());
        }

        for (UUID defenderId : ownerTeam.getMembers()) {
            Player defender = Bukkit.getPlayer(defenderId);
            if (defender == null || !defender.isOnline()) {
                continue;
            }
            services.getMessages().send(defender, "trap.activated",
                    "{trap}",
                    services.getMessages().get(defender,
                            "trap." + result.getTrapId() + ".name"),
                    "{player}", intruder == null
                            ? services.getMessages().get(defender,
                            "chicken.unknown") : intruder.getName());
        }
    }

    private boolean isInsideOwnBase(GameTeam team, Location location) {
        if (team == null) {
            return false;
        }
        CuboidRegion region = team.getDefinition().getBaseRegion();
        if (region == null || location == null
                || location.getWorld() == null) {
            return false;
        }
        return region.contains(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ());
    }

    private boolean hasChangedBlock(Location from, Location to) {
        if (from == null || to == null) {
            return true;
        }
        if (from.getWorld() != to.getWorld()) {
            return true;
        }
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }
}
