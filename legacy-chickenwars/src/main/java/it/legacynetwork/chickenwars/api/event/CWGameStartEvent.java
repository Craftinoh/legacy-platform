package it.legacynetwork.chickenwars.api.event;

import it.legacynetwork.chickenwars.game.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Lanciato quando una partita passa allo stato di gioco.
 *
 * <p>Evento non annullabile: a questo punto squadre, galline e generatori sono
 * gia' stati creati.</p>
 */
public final class CWGameStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;

    public CWGameStartEvent(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
