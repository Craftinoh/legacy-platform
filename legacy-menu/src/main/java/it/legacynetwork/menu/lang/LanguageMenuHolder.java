package it.legacynetwork.menu.lang;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LanguageMenuHolder implements InventoryHolder {
    private final int page;

    public static final String TYPE = "LANGUAGE_MENU";

    public LanguageMenuHolder(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
