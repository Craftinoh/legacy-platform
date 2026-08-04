package it.legacynetwork.items.listener;

import it.legacynetwork.items.item.CustomItemMatcher;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public final class PlayerDropListener implements Listener {
    private final CustomItemMatcher matcher;

    public PlayerDropListener(CustomItemMatcher matcher) {
        this.matcher = matcher;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (matcher.isCustomItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
