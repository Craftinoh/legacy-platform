package it.legacynetwork.items.listener;

import it.legacynetwork.items.cooldown.ItemCooldownService;
import it.legacynetwork.items.item.CustomItemGiveService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {
    private final ItemCooldownService cooldownService;
    private final CustomItemGiveService giveService;

    public PlayerQuitListener(ItemCooldownService cooldownService,
                               CustomItemGiveService giveService) {
        this.cooldownService = cooldownService;
        this.giveService = giveService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldownService.clearPlayer(event.getPlayer().getUniqueId());
        giveService.clearCache(event.getPlayer());
    }
}
