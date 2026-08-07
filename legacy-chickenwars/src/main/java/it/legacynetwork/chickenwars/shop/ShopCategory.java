package it.legacynetwork.chickenwars.shop;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Categoria dello shop, mostrata nella riga superiore del menu.
 */
public final class ShopCategory {

    private final String id;
    private final String italianName;
    private final String englishName;
    private final Material icon;
    private final byte iconData;
    private final int slot;
    private final List<ShopItem> items = new ArrayList<ShopItem>();

    ShopCategory(String id, String italianName, String englishName,
                 Material icon, byte iconData, int slot) {
        this.id = id;
        this.italianName = italianName;
        this.englishName = englishName;
        this.icon = icon;
        this.iconData = iconData;
        this.slot = slot;
    }

    void addItem(ShopItem item) {
        if (item != null) {
            items.add(item);
        }
    }

    /**
     * Crea l'icona della categoria per la riga di navigazione.
     *
     * @param language lingua del giocatore
     * @param selected indica se la categoria e' quella aperta
     * @return l'icona da inserire nell'inventario
     */
    @SuppressWarnings("deprecation")
    public ItemStack createIcon(String language, boolean selected) {
        ItemStack stack = new ItemStack(icon, 1, (short) iconData);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = getDisplayName(language);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    (selected ? "&a&l" : "&e") + name));
            if (selected) {
                meta.setLore(Collections.singletonList(
                        ChatColor.GRAY + "> " + ChatColor.WHITE
                                + ("it".equalsIgnoreCase(language)
                                ? "categoria aperta" : "open category")));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public String getDisplayName(String language) {
        if ("it".equalsIgnoreCase(language)) {
            return italianName;
        }
        return englishName == null || englishName.isEmpty()
                ? italianName : englishName;
    }

    public String getId() {
        return id;
    }

    public int getSlot() {
        return slot;
    }

    public List<ShopItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
