package it.legacynetwork.menu.model;

import it.legacynetwork.menu.util.LegacyColorTranslator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuItem {
    private final String material;
    private final int data;
    private final int amount;
    private final int slot;
    private final Map<String, String> name;
    private final Map<String, List<String>> lore;
    private final Map<String, List<MenuItemAction>> actions;

    public MenuItem(String material, int data, int amount, int slot,
                    Map<String, String> name, Map<String, List<String>> lore,
                    Map<String, List<MenuItemAction>> actions) {
        this.material = material;
        this.data = data;
        this.amount = amount;
        this.slot = slot;
        this.name = name;
        this.lore = lore;
        this.actions = actions;
    }

    public String getMaterial() {
        return material;
    }

    public int getData() {
        return data;
    }

    public int getAmount() {
        return amount;
    }

    public int getSlot() {
        return slot;
    }

    public Map<String, String> getName() {
        return name;
    }

    public Map<String, List<String>> getLore() {
        return lore;
    }

    public Map<String, List<MenuItemAction>> getActions() {
        return actions;
    }

    public void apply(Inventory inv, Player player, String lang) {
        Material mat = Material.getMaterial(material);
        if (mat == null) {
            return;
        }
        ItemStack item = new ItemStack(mat, amount, (short) data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = resolveString(name, lang);
            if (displayName != null) {
                meta.setDisplayName(LegacyColorTranslator.translate(displayName));
            }
            List<String> langLore = resolveList(lore, lang);
            if (langLore != null) {
                List<String> translatedLore = new ArrayList<String>();
                for (String line : langLore) {
                    translatedLore.add(LegacyColorTranslator.translate(line));
                }
                meta.setLore(translatedLore);
            }
            item.setItemMeta(meta);
        }
        inv.setItem(slot - 1, item);
    }

    private String resolveString(Map<String, String> map, String lang) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        String value = map.get(lang);
        if (value == null) {
            value = map.get("en");
        }
        return value;
    }

    private List<String> resolveList(Map<String, List<String>> map, String lang) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        List<String> value = map.get(lang);
        if (value == null) {
            value = map.get("en");
        }
        return value;
    }
}
