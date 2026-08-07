package it.legacynetwork.chickenwars.api.event;

import it.legacynetwork.chickenwars.chicken.RoyalChicken;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameTeam;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Lanciato prima che una Gallina Reale subisca danno.
 *
 * <p>Evento annullabile: annullarlo impedisce sia il danno sia gli effetti
 * associati. Il danno puo' inoltre essere modificato con
 * {@link #setDamage(double)}.</p>
 */
public final class CWChickenDamageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final GameTeam owner;
    private final RoyalChicken chicken;
    private final Player attacker;

    private double damage;
    private boolean cancelled;

    public CWChickenDamageEvent(Game game, GameTeam owner, RoyalChicken chicken,
                                Player attacker, double damage) {
        this.game = game;
        this.owner = owner;
        this.chicken = chicken;
        this.attacker = attacker;
        this.damage = damage;
    }

    public Game getGame() {
        return game;
    }

    /** Squadra proprietaria della gallina colpita. */
    public GameTeam getOwner() {
        return owner;
    }

    public RoyalChicken getChicken() {
        return chicken;
    }

    /**
     * @return l'autore del colpo, oppure {@code null} se non e' un giocatore
     */
    public Player getAttacker() {
        return attacker;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = Math.max(0.0D, damage);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
