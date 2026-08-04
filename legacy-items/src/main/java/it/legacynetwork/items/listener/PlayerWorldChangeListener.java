package it.legacynetwork.items.listener;

import it.legacynetwork.items.config.LegacyItemsConfiguration;
import it.legacynetwork.items.definition.CustomItemDefinition;
import it.legacynetwork.items.definition.CustomItemTrigger;
import it.legacynetwork.items.item.CustomItemGiveService;
import it.legacynetwork.items.item.CustomItemRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class PlayerWorldChangeListener implements Listener {
    private final CustomItemGiveService giveService;
    private final CustomItemRegistry registry;
    private final LegacyItemsConfiguration config;

    public PlayerWorldChangeListener(CustomItemGiveService giveService,
                                      CustomItemRegistry registry,
                                      LegacyItemsConfiguration config) {
        this.giveService = giveService;
        this.registry = registry;
        this.config = config;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        removeDisallowedItems(player);
        giveService.giveTriggeredItems(player, CustomItemTrigger.WORLD_CHANGE);
    }

    private void removeDisallowedItems(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == org.bukkit.Material.AIR) {
                continue;
            }
            CustomItemDefinition def = null;
            for (CustomItemDefinition d : registry.getAll()) {
                if (d.isEnabled() && !d.isAllowedInWorld(player.getWorld().getName())) {
                    if (item.getType() == org.bukkit.Material.matchMaterial(
                            d.getMaterial())) {
                        def = d;
                        break;
                    }
                }
            }
            if (def != null) {
                inv.clear(i);
            }
        }
    }
}
