package it.legacynetwork.menu.model;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MenuInventoryHolder implements InventoryHolder {
    private final String menuId;

    public MenuInventoryHolder(String menuId) {
        this.menuId = menuId;
    }

    public String getMenuId() {
        return menuId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
