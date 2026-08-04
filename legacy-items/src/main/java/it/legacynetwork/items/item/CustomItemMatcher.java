package it.legacynetwork.items.item;

import it.legacynetwork.items.definition.CustomItemDefinition;
import it.legacynetwork.items.util.LegacyColorTranslator;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CustomItemMatcher {
    private final CustomItemRegistry registry;

    public CustomItemMatcher(CustomItemRegistry registry) {
        this.registry = registry;
    }

    public CustomItemDefinition match(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        for (CustomItemDefinition def : registry.getAll()) {
            if (!def.isEnabled()) {
                continue;
            }
            Material expected = Material.matchMaterial(def.getMaterial());
            if (expected == null || item.getType() != expected) {
                continue;
            }
            if (item.getDurability() != (short) def.getData()) {
                continue;
            }
            return def;
        }
        return null;
    }

    public boolean isCustomItem(ItemStack item) {
        return match(item) != null;
    }

    public boolean isProtected(ItemStack item) {
        CustomItemDefinition def = match(item);
        if (def == null) {
            return false;
        }
        return def.getFlags().isPreventMove()
                || def.getFlags().isPreventSwap()
                || def.getFlags().isPreventDrop();
    }
}
