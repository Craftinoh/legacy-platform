package it.legacynetwork.menu.model;

import it.legacynetwork.menu.util.LegacyColorTranslator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public class MenuDefinition {
    private final String id;
    private final boolean enabled;
    private final int size;
    private final Map<String, String> title;
    private final Map<Integer, MenuItem> items;

    public MenuDefinition(String id, boolean enabled, int size,
                          Map<String, String> title, Map<Integer, MenuItem> items) {
        this.id = id;
        this.enabled = enabled;
        this.size = size;
        this.title = title;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSize() {
        return size;
    }

    public Map<String, String> getTitle() {
        return title;
    }

    public Map<Integer, MenuItem> getItems() {
        return items;
    }

    public void open(Player player, String lang) {
        String rawTitle = title.getOrDefault(lang, title.get("en"));
        if (rawTitle == null) {
            rawTitle = "";
        }
        String translatedTitle = LegacyColorTranslator.translate(rawTitle);
        if (translatedTitle.length() > 32) {
            translatedTitle = translatedTitle.substring(0, 32);
        }
        Inventory inv = Bukkit.createInventory(
                new MenuInventoryHolder(id), size, translatedTitle);
        if (items != null) {
            for (Map.Entry<Integer, MenuItem> entry : items.entrySet()) {
                entry.getValue().apply(inv, player, lang);
            }
        }
        player.openInventory(inv);
    }
}
