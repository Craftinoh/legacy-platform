package it.legacynetwork.chickenwars.api.event;

import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameTeam;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Lanciato al termine di una partita, prima del ripristino della mappa.
 *
 * <p>Evento non annullabile.</p>
 */
public final class CWGameEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final GameTeam winner;

    public CWGameEndEvent(Game game, GameTeam winner) {
        this.game = game;
        this.winner = winner;
    }

    public Game getGame() {
        return game;
    }

    /**
     * @return la squadra vincitrice, oppure {@code null} in caso di pareggio
     */
    public GameTeam getWinner() {
        return winner;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
