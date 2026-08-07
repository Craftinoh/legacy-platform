package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.player.InventorySnapshot;
import it.legacynetwork.chickenwars.player.PendingRestoreService;
import it.legacynetwork.chickenwars.player.PlayerSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Ingresso e uscita dal server durante una partita.
 */
public final class ConnectionListener implements Listener {

    private final ArenaManager arenas;
    private final PendingRestoreService pendingRestores;

    public ConnectionListener(ArenaManager arenas,
                              PendingRestoreService pendingRestores) {
        this.arenas = arenas;
        this.pendingRestores = pendingRestores;
    }

    /**
     * Riapplica l'inventario salvato a chi si era disconnesso in partita.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        pendingRestores.restore(event.getPlayer());
    }

    /**
     * Rimuove dalla partita chi si disconnette, conservandone l'inventario.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Game game = arenas.getGameOf(player);
        if (game == null) {
            return;
        }

        PlayerSession session = game.getSession(player.getUniqueId());
        InventorySnapshot snapshot = session == null ? null : session.getSnapshot();

        game.leave(player, false);

        if (snapshot != null) {
            pendingRestores.register(player.getUniqueId(), snapshot);
        }
    }
}
