package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.setup.SetupMenu;
import it.legacynetwork.chickenwars.setup.SetupService;
import it.legacynetwork.chickenwars.setup.SetupTool;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Interazioni dell'editor guidato delle arene.
 *
 * <p>Finche' la sessione e' aperta gli strumenti non possono essere piazzati,
 * spostati o lasciati cadere: ogni click con uno strumento in mano esegue la
 * relativa azione.</p>
 */
public final class SetupListener implements Listener {

    private final SetupService setup;

    public SetupListener(SetupService setup) {
        this.setup = setup;
    }

    /**
     * Esegue l'azione dello strumento impugnato.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!setup.isEditing(player)) {
            return;
        }

        ItemStack held = event.getItem();
        if (held == null) {
            return;
        }
        SetupTool tool = SetupTool.find(
                player.getInventory().getHeldItemSlot(), held.getType());
        if (tool == null) {
            return;
        }

        // Evita che gli strumenti vengano piazzati come blocchi.
        event.setCancelled(true);
        if (!event.getAction().name().startsWith("RIGHT_CLICK")) {
            return;
        }
        setup.handleTool(player, tool);
    }

    /**
     * Instrada i click nei menu dell'editor.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        SetupMenu menu = resolveMenu(event.getInventory());

        if (menu == null) {
            // Impedisce di riorganizzare o rimuovere gli strumenti.
            if (setup.isEditing(player)) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() < 0
                || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }
        setup.handleMenuClick(player, menu, event.getRawSlot(),
                event.isRightClick());
    }

    /**
     * Impedisce il trascinamento negli inventari dell'editor.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (resolveMenu(event.getInventory()) != null) {
            event.setCancelled(true);
            return;
        }
        if (event.getWhoClicked() instanceof Player
                && setup.isEditing((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    /**
     * Impedisce di perdere gli strumenti dell'editor.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDropItem(PlayerDropItemEvent event) {
        if (setup.isEditing(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Impedisce di piazzare gli strumenti nel mondo.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!setup.isEditing(event.getPlayer())) {
            return;
        }
        ItemStack held = event.getItemInHand();
        if (held != null && SetupTool.find(
                event.getPlayer().getInventory().getHeldItemSlot(),
                held.getType()) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Chiude la sessione se l'amministratore si disconnette.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (setup.isEditing(event.getPlayer())) {
            setup.exit(event.getPlayer(), true);
        }
    }

    private SetupMenu resolveMenu(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof SetupMenu ? (SetupMenu) holder : null;
    }
}
