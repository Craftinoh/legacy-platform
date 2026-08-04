package it.legacynetwork.items.listener;

import it.legacynetwork.items.item.CustomItemMatcher;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class InventoryProtectionListener implements Listener {
    private final CustomItemMatcher matcher;

    public InventoryProtectionListener(CustomItemMatcher matcher) {
        this.matcher = matcher;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean customClicked = current != null && matcher.isCustomItem(current);
        boolean customCursor = cursor != null
                && cursor.getType() != Material.AIR
                && matcher.isCustomItem(cursor);
        if (customClicked || customCursor) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        for (ItemStack item : event.getNewItems().values()) {
            if (matcher.isCustomItem(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
