package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.protection.VoidFallGuard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * Impedisce di espellere oggetti mentre si cade nel vuoto.
 *
 * <p>Blocca soltanto le azioni che possono far uscire oggetti dall'inventario:
 * il resto della gestione inventario resta intatto, e fuori dalla condizione di
 * caduta il drop normale continua a funzionare.</p>
 *
 * <p>La protezione vale per ogni oggetto, non solo per le valute: gli oggetti
 * restano nell'inventario fino alla gestione della morte.</p>
 */
public final class VoidProtectionListener implements Listener {

    private final ArenaManager arenas;
    private final VoidFallGuard guard;

    public VoidProtectionListener(ArenaManager arenas, VoidFallGuard guard) {
        this.arenas = arenas;
        this.guard = guard;
    }

    /**
     * Blocca Q e Ctrl+Q durante la caduta.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        if (isProtected(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Blocca i percorsi di espulsione dal menu inventario.
     *
     * <p>Sono coperti il tasto drop sullo slot, il drop dell'intera pila e il
     * click fuori dalla finestra con oggetto sul cursore.</p>
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!isProtected(player)) {
            return;
        }

        InventoryAction action = event.getAction();
        boolean drops = action == InventoryAction.DROP_ONE_SLOT
                || action == InventoryAction.DROP_ALL_SLOT
                || action == InventoryAction.DROP_ONE_CURSOR
                || action == InventoryAction.DROP_ALL_CURSOR;

        // Slot negativo significa click fuori dalla finestra: con un oggetto
        // sul cursore equivale a lanciarlo a terra.
        boolean outside = event.getSlot() == -999 || event.getRawSlot() < 0;

        if (drops || outside) {
            event.setCancelled(true);
        }
    }

    /**
     * Blocca il trascinamento che termina fuori dalla finestra.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!isProtected(player)) {
            return;
        }
        for (Integer rawSlot : event.getRawSlots()) {
            if (rawSlot == null || rawSlot.intValue() < 0) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Verifica in un unico punto se il giocatore e' nella condizione protetta.
     */
    private boolean isProtected(Player player) {
        Game game = arenas.getGameOf(player);
        return game != null
                && guard.isFallingIntoVoid(player, game.getDefinition());
    }
}
