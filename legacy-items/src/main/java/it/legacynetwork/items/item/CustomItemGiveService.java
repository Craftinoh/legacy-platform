package it.legacynetwork.items.item;

import it.legacynetwork.items.config.LegacyItemsConfiguration;
import it.legacynetwork.items.definition.CustomItemDefinition;
import it.legacynetwork.items.definition.CustomItemTrigger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomItemGiveService {
    private final JavaPlugin plugin;
    private final CustomItemRegistry registry;
    private final CustomItemFactory factory;
    private final CustomItemMatcher matcher;
    private final LegacyItemsConfiguration config;
    private final Map<UUID, Map<Integer, String>> slotCache = new ConcurrentHashMap<>();

    public CustomItemGiveService(JavaPlugin plugin,
                                  CustomItemRegistry registry,
                                  CustomItemFactory factory,
                                  CustomItemMatcher matcher,
                                  LegacyItemsConfiguration config) {
        this.plugin = plugin;
        this.registry = registry;
        this.factory = factory;
        this.matcher = matcher;
        this.config = config;
    }

    public void giveTriggeredItems(Player player, CustomItemTrigger trigger) {
        if (config.isRemoveCustomItemsBeforeGive()) {
            removeCustomItems(player);
        }
        for (CustomItemDefinition def : registry.getByTrigger(trigger)) {
            if (!canGive(player, def)) {
                continue;
            }
            scheduleGive(player, def);
        }
    }

    private boolean canGive(Player player, CustomItemDefinition def) {
        if (!def.isEnabled()) {
            return false;
        }
        if (!def.isAllowedInWorld(player.getWorld().getName())) {
            return false;
        }
        if (def.isPermissionRequired() && !def.getPermissionNode().isEmpty()
                && !player.hasPermission(def.getPermissionNode())) {
            return false;
        }
        return true;
    }

    private void scheduleGive(Player player, CustomItemDefinition definition) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            giveItemNow(player, definition);
        }, config.getGiveDelayTicks());
    }

    public void giveItemNow(Player player, CustomItemDefinition definition) {
        int bukkitSlot = definition.getSlot() - 1;
        Inventory inv = player.getInventory();

        if (definition.getFlags().isUnique()) {
            removePreviousCopies(player, definition);
        }

        ItemStack newItem = factory.createItem(player, definition);
        if (newItem == null) {
            return;
        }

        ItemStack existing = inv.getItem(bukkitSlot);
        if (existing != null && existing.getType() != Material.AIR) {
            if (definition.getFlags().isReplaceExisting()) {
                inv.setItem(bukkitSlot, newItem);
            } else if (config.isDebug()) {
                plugin.getLogger().info("Slot " + definition.getSlot()
                        + " occupato per " + player.getName());
                return;
            }
        } else {
            inv.setItem(bukkitSlot, newItem);
        }

        Map<Integer, String> playerSlots = slotCache.computeIfAbsent(
                player.getUniqueId(), k -> new ConcurrentHashMap<>());
        playerSlots.put(bukkitSlot, definition.getId());
    }

    private void removePreviousCopies(Player player, CustomItemDefinition definition) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                CustomItemDefinition matched = matcher.match(item);
                if (matched != null && matched.getId().equals(definition.getId())) {
                    inv.clear(i);
                }
            }
        }
    }

    public void removeCustomItems(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && matcher.isCustomItem(item)) {
                inv.clear(i);
            }
        }
    }

    public void rebuildOnLanguageChange(Player player) {
        Map<Integer, String> playerSlots = slotCache.get(player.getUniqueId());
        if (playerSlots == null) {
            return;
        }
        for (Map.Entry<Integer, String> entry : playerSlots.entrySet()) {
            CustomItemDefinition def = registry.get(entry.getValue());
            if (def != null && def.isEnabled() && canGive(player, def)) {
                ItemStack newItem = factory.createItem(player, def);
                if (newItem != null) {
                    player.getInventory().setItem(entry.getKey(), newItem);
                }
            }
        }
    }

    public Map<Integer, String> getSlotCache(Player player) {
        return slotCache.get(player.getUniqueId());
    }

    public void clearCache(Player player) {
        slotCache.remove(player.getUniqueId());
    }
}
