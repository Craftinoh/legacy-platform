package it.legacynetwork.chickenwars.shop;

/**
 * Schermata attualmente mostrata dall'inventario dello shop.
 *
 * <p>Le due viste condividono lo stesso inventario e lo stesso holder: cambia
 * solo il contenuto disegnato, quindi non esiste un secondo sistema di menu.</p>
 */
public enum ShopMenuView {

    /** Categorie e articoli, compresa la griglia Quick Buy. */
    SHOP,
    /** Gestione dei preset Quick Buy. */
    PRESETS
}
