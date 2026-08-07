package it.legacynetwork.chickenwars.api;

import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameTeam;
import it.legacynetwork.chickenwars.player.PlayerSession;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Implementazione dell'API pubblica basata su {@link ArenaManager}.
 */
public final class ChickenWarsServiceImpl implements ChickenWarsService {

    private static final int API_VERSION = 1;

    private final ArenaManager arenas;

    public ChickenWarsServiceImpl(ArenaManager arenas) {
        if (arenas == null) {
            throw new IllegalArgumentException("ArenaManager mancante");
        }
        this.arenas = arenas;
    }

    @Override
    public Collection<ArenaDefinition> getArenas() {
        return arenas.getDefinitions();
    }

    @Override
    public Collection<Game> getGames() {
        return arenas.getGames();
    }

    @Override
    public Game getGame(String arenaId) {
        return arenas.getGame(arenaId);
    }

    @Override
    public Game getGameOf(UUID playerId) {
        return arenas.getGameOf(playerId);
    }

    @Override
    public boolean isPlaying(Player player) {
        if (player == null) {
            return false;
        }
        Game game = arenas.getGameOf(player);
        if (game == null) {
            return false;
        }
        PlayerSession session = game.getSession(player.getUniqueId());
        return session != null && session.getState().isActive();
    }

    @Override
    public GameTeam getTeamOf(Player player) {
        if (player == null) {
            return null;
        }
        Game game = arenas.getGameOf(player);
        if (game == null) {
            return null;
        }
        PlayerSession session = game.getSession(player.getUniqueId());
        return session == null ? null : game.getTeam(session.getTeamId());
    }

    @Override
    public Game findBestGame() {
        return arenas.findBestGame();
    }

    @Override
    public int getApiVersion() {
        return API_VERSION;
    }
}
