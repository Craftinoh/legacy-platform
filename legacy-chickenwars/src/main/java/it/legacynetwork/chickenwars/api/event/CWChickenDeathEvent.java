package it.legacynetwork.chickenwars.api.event;

import it.legacynetwork.chickenwars.chicken.RoyalChicken;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameTeam;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Lanciato quando una Gallina Reale viene eliminata.
 *
 * <p>Evento non annullabile: la squadra ha gia' perso la possibilita' di
 * respawn.</p>
 */
public final class CWChickenDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final GameTeam owner;
    private final RoyalChicken chicken;
    private final Player killer;

    public CWChickenDeathEvent(Game game, GameTeam owner, RoyalChicken chicken,
                               Player killer) {
        this.game = game;
        this.owner = owner;
        this.chicken = chicken;
        this.killer = killer;
    }

    public Game getGame() {
        return game;
    }

    public GameTeam getOwner() {
        return owner;
    }

    public RoyalChicken getChicken() {
        return chicken;
    }

    /**
     * @return chi ha inferto il colpo fatale, oppure {@code null}
     */
    public Player getKiller() {
        return killer;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
