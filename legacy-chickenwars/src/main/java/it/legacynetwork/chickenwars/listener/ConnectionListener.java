package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.player.InventorySnapshot;
import it.legacynetwork.chickenwars.player.PendingRestoreService;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.player.ReconnectService;
import it.legacynetwork.chickenwars.shop.QuickBuyService;
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
    private final ReconnectService reconnects;
    private final QuickBuyService quickBuy;
    private final GameServices services;

    public ConnectionListener(ArenaManager arenas,
                              PendingRestoreService pendingRestores,
                              ReconnectService reconnects,
                              QuickBuyService quickBuy,
                              GameServices services) {
        this.arenas = arenas;
        this.pendingRestores = pendingRestores;
        this.reconnects = reconnects;
        this.quickBuy = quickBuy;
        this.services = services;
    }

    /**
     * Riapplica l'inventario salvato a chi si era disconnesso in partita.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        pendingRestores.restore(event.getPlayer());
        // I preset arrivano dal repository: quando sara' un database il flusso
        // restera' identico.
        quickBuy.load(event.getPlayer().getUniqueId());
    }

    /**
     * Rimuove dalla partita chi si disconnette, conservandone l'inventario.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        quickBuy.unload(player.getUniqueId());

        Game game = arenas.getGameOf(player);
        if (game == null) {
            return;
        }

        PlayerSession session = game.getSession(player.getUniqueId());
        InventorySnapshot snapshot = session == null ? null : session.getSnapshot();

        // Uscire mentre si e' in combattimento vale come morte: risorse,
        // downgrade e reset della spada seguono lo stesso orchestratore della
        // morte normale, quindi avvengono una sola volta.
        game.handleCombatLogout(player);

        // Conserva solo cio' che e' permanente: valute e consumabili spariscono
        // con la sessione, quindi non sono recuperabili con logout e login.
        if (session != null) {
            reconnects.remember(session);
        }

        game.leave(player, false);

        services.getBaseEntryTracker().forget(player.getUniqueId());
        services.getHealPool().forget(player.getUniqueId());
        services.getTeamEffects().forget(player.getUniqueId());

        if (snapshot != null) {
            pendingRestores.register(player.getUniqueId(), snapshot);
        }
    }
}
