package it.legacynetwork.items.listener;

import it.legacynetwork.items.definition.CustomItemTrigger;
import it.legacynetwork.items.item.CustomItemGiveService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PlayerRespawnListener implements Listener {
    private final CustomItemGiveService giveService;

    public PlayerRespawnListener(CustomItemGiveService giveService) {
        this.giveService = giveService;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        giveService.giveTriggeredItems(event.getPlayer(), CustomItemTrigger.RESPAWN);
    }
}
