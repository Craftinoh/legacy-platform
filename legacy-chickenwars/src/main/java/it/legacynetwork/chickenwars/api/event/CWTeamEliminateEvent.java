package it.legacynetwork.chickenwars.api.event;

import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameTeam;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Lanciato quando una squadra esce definitivamente dalla partita.
 *
 * <p>Evento non annullabile.</p>
 */
public final class CWTeamEliminateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final GameTeam team;

    public CWTeamEliminateEvent(Game game, GameTeam team) {
        this.game = game;
        this.team = team;
    }

    public Game getGame() {
        return game;
    }

    public GameTeam getTeam() {
        return team;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
