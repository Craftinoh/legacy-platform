package it.legacynetwork.chickenwars.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Categoria dello shop, mostrata nella riga di navigazione superiore.
 */
public final class ShopCategoryDefinition {

    /** Identificatore riservato alla categoria Quick Buy. */
    public static final String QUICK_BUY_ID = "quick_buy";

    private final String id;
    private final Material icon;
    private final byte iconData;
    private final int slot;
    private final List<ShopItemDefinition> items =
            new ArrayList<ShopItemDefinition>();

    ShopCategoryDefinition(String id, Material icon, byte iconData, int slot) {
        this.id = id;
        this.icon = icon;
        this.iconData = iconData;
        this.slot = slot;
    }

    void addItem(ShopItemDefinition item) {
        if (item != null) {
            items.add(item);
        }
    }

    /**
     * Indica se questa categoria e' il Quick Buy, che ha un rendering proprio.
     */
    public boolean isQuickBuy() {
        return QUICK_BUY_ID.equals(id);
    }

    /**
     * Chiave del nome localizzato nei file lingua.
     */
    public String getNameKey() {
        return "shop.categories." + id;
    }

    public String getId() {
        return id;
    }

    public Material getIcon() {
        return icon;
    }

    public byte getIconData() {
        return iconData;
    }

    public int getSlot() {
        return slot;
    }

    public List<ShopItemDefinition> getItems() {
        return Collections.unmodifiableList(items);
    }
}
