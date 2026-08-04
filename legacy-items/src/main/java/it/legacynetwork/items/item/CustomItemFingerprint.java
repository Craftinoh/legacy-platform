package it.legacynetwork.items.item;

import it.legacynetwork.items.definition.CustomItemDefinition;

public final class CustomItemFingerprint {
    private CustomItemFingerprint() {
    }

    public static String compute(CustomItemDefinition definition) {
        StringBuilder builder = new StringBuilder();
        builder.append(definition.getId()).append('|');
        builder.append(definition.getMaterial()).append('|');
        builder.append(definition.getData()).append('|');
        builder.append(definition.getSlot()).append('|');
        return builder.toString();
    }

    public static String computeFromStack(
            org.bukkit.inventory.ItemStack item, CustomItemDefinition def) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        StringBuilder builder = new StringBuilder();
        builder.append(def.getId()).append('|');
        builder.append(item.getType().name()).append('|');
        builder.append(item.getDurability()).append('|');
        builder.append(meta.hasDisplayName() ? meta.getDisplayName() : "").append('|');
        builder.append(def.getSlot());
        return builder.toString();
    }
}
