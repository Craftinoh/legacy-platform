package it.legacynetwork.items.listener;

import it.legacynetwork.items.config.LegacyItemsConfiguration;
import it.legacynetwork.items.definition.CustomItemTrigger;
import it.legacynetwork.items.item.CustomItemGiveService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final CustomItemGiveService giveService;
    private final LegacyItemsConfiguration config;

    public PlayerJoinListener(CustomItemGiveService giveService,
                               LegacyItemsConfiguration config) {
        this.giveService = giveService;
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!config.isEnabled()) {
            return;
        }
        giveService.giveTriggeredItems(player, CustomItemTrigger.JOIN);
    }
}
