package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.chicken.RoyalChickenRegistry;
import it.legacynetwork.chickenwars.config.ChickenWarsConfig;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.game.GameTeam;
import it.legacynetwork.chickenwars.model.ArenaState;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.player.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.UUID;

/**
 * Combattimento, danno alla Gallina Reale e morte dei giocatori.
 *
 * <p>La morte viene intercettata prima che avvenga davvero: il danno letale
 * viene annullato e sostituito dalla logica di partita, evitando la schermata di
 * respawn e la perdita di oggetti.</p>
 */
public final class CombatListener implements Listener {

    private final ArenaManager arenas;
    private final GameServices services;

    public CombatListener(ArenaManager arenas, GameServices services) {
        this.arenas = arenas;
        this.services = services;
    }

    /**
     * Registra l'aggressore e applica le regole di fuoco amico e protezione.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());

        RoyalChickenRegistry.Entry royal = services.getRoyalRegistry()
                .lookup(event.getEntity().getUniqueId());
        Game chickenGame = royal == null
                ? null : arenas.getGame(royal.getArenaId());
        GameTeam chickenOwner = chickenGame == null
                ? null : chickenGame.getTeam(royal.getTeamId());

        if (chickenOwner != null) {
            event.setCancelled(true);
            if (attacker != null) {
                chickenGame.damageChicken(chickenOwner, attacker, event.getDamage());
            }
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        Game game = arenas.getGameOf(victim);
        if (game == null) {
            return;
        }

        PlayerSession victimSession = game.getSession(victim.getUniqueId());
        if (victimSession == null || !victimSession.getState().isActive()
                || game.getState() != ArenaState.IN_GAME) {
            event.setCancelled(true);
            return;
        }
        if (victimSession.isInvulnerable()) {
            event.setCancelled(true);
            return;
        }

        if (attacker == null) {
            return;
        }
        PlayerSession attackerSession = game.getSession(attacker.getUniqueId());
        if (attackerSession == null || !attackerSession.getState().isActive()) {
            event.setCancelled(true);
            return;
        }
        if (isSameTeam(game, victimSession, attackerSession)
                && !services.getConfig().isFriendlyFire()) {
            event.setCancelled(true);
            return;
        }

        // Colpire annulla la protezione di respawn di chi attacca.
        attackerSession.clearInvulnerability();
        victimSession.recordDamager(attacker.getUniqueId());
    }

    /**
     * Sostituisce il danno letale con la logica di morte della partita.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            // Galline reali, venditori e ologrammi non subiscono danno vanilla:
            // la vita della gallina e' gestita interamente dal plugin.
            if (isInsideAnyArena(event.getEntity().getLocation())) {
                event.setCancelled(true);
            }
            return;
        }

        Player player = (Player) event.getEntity();
        Game game = arenas.getGameOf(player);
        if (game == null) {
            return;
        }

        PlayerSession session = game.getSession(player.getUniqueId());
        if (session == null) {
            return;
        }

        if (game.getState() != ArenaState.IN_GAME
                || session.getState() != PlayerState.PLAYING
                || session.isInvulnerable()) {
            event.setCancelled(true);
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                teleportToSafety(game, player);
            }
            return;
        }

        double remaining = player.getHealth() - event.getFinalDamage();
        boolean lethal = event.getCause() == EntityDamageEvent.DamageCause.VOID
                || remaining <= 0.0D;
        if (!lethal) {
            return;
        }

        event.setCancelled(true);
        player.setHealth(player.getMaxHealth());
        game.handleDeath(player, resolveKiller(game, session));
    }

    /**
     * Riporta allo spawn spettatori chi cade nel vuoto senza essere in gioco.
     */
    private void teleportToSafety(Game game, Player player) {
        if (game.getDefinition().getSpectator() == null) {
            return;
        }
        org.bukkit.Location target = game.getDefinition().getSpectator().toLocation();
        if (target != null) {
            player.teleport(target);
        }
    }

    private Player resolveKiller(Game game, PlayerSession session) {
        ChickenWarsConfig config = services.getConfig();
        UUID killerId = session.getValidDamager(config.getVoidKillCreditSeconds());
        if (killerId == null) {
            return null;
        }
        Player killer = Bukkit.getPlayer(killerId);
        return killer != null && game.contains(killerId) ? killer : null;
    }

    /**
     * Impedisce che la fame interferisca con la partita.
     */
    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (arenas.getGameOf(player) != null) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    /**
     * Consente di lasciare a terra solo le risorse, non l'equipaggiamento.
     */
    @EventHandler(ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        Game game = arenas.getGameOf(event.getPlayer());
        if (game == null) {
            return;
        }
        PlayerSession session = game.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.getState().isActive()) {
            event.setCancelled(true);
        }
    }

    /**
     * Conteggia le risorse raccolte e blocca la raccolta agli spettatori.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPickupProtection(PlayerPickupItemEvent event) {
        Game game = arenas.getGameOf(event.getPlayer());
        if (game == null) {
            return;
        }
        PlayerSession session = game.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.getState().isActive()) {
            event.setCancelled(true);
        }
    }

    /** Registra a MONITOR soltanto la quantita' che Bukkit consegnera'. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNaturalResourcePickup(PlayerPickupItemEvent event) {
        Game game = arenas.getGameOf(event.getPlayer());
        if (game == null || event.getItem().getItemStack() == null) return;
        PlayerSession session = game.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.getState().isActive()) return;
        int stackAmount = event.getItem().getItemStack().getAmount();
        int collected = Math.max(0, stackAmount - event.getRemaining());
        if (collected > 0 && services.getGeneratedResources().pickup(
                event.getItem().getUniqueId(), game.getMatchId(),
                event.getRemaining() <= 0) != null) {
            session.addResources(collected);
        }
    }

    private boolean isSameTeam(Game game, PlayerSession first,
                               PlayerSession second) {
        return first.getTeamId() != null
                && first.getTeamId().equals(second.getTeamId());
    }

    /**
     * Indica se la posizione ricade nella regione di una qualsiasi arena.
     */
    private boolean isInsideAnyArena(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        for (ArenaDefinition definition : arenas.getDefinitions()) {
            if (definition.contains(location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ())) {
                return true;
            }
        }
        return false;
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            Object shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        return null;
    }
}
