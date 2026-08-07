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
    private ShopMenuView view = ShopMenuView.SHOP;
    private String pendingItemId;

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

    public ShopMenuView getView() {
        return view;
    }

    public void setView(ShopMenuView view) {
        this.view = view == null ? ShopMenuView.SHOP : view;
    }

    /**
     * Articolo preso in carico e in attesa di uno slot Quick Buy.
     *
     * <p>Stato transitorio della singola finestra aperta, non una copia dei
     * preset: la configurazione autorevole resta in {@code QuickBuyService}.</p>
     *
     * @return l'articolo in attesa, oppure {@code null}
     */
    public String getPendingItemId() {
        return pendingItemId;
    }

    public void setPendingItemId(String pendingItemId) {
        this.pendingItemId = pendingItemId == null
                || pendingItemId.trim().isEmpty() ? null : pendingItemId.trim();
    }

    public boolean hasPendingItem() {
        return pendingItemId != null;
    }

    public void clearPendingItem() {
        this.pendingItemId = null;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
