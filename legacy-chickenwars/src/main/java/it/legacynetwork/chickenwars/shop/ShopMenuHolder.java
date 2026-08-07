package it.legacynetwork.chickenwars.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marcatore che identifica gli inventari appartenenti allo shop.
 *
 * <p>Riconoscere il menu dal proprio holder, e non dal titolo, evita che un
 * inventario qualsiasi con lo stesso nome venga trattato come shop.</p>
 */
public final class ShopMenuHolder implements InventoryHolder {

    private final String arenaId;
    private String categoryId;

    public ShopMenuHolder(String arenaId, String categoryId) {
        this.arenaId = arenaId;
        this.categoryId = categoryId;
    }

    public String getArenaId() {
        return arenaId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
