package it.legacynetwork.chickenwars.shop;

/**
 * Schermata attualmente mostrata dall'inventario dello shop.
 *
 * <p>Le viste condividono lo stesso inventario e lo stesso holder: cambia
 * solo il contenuto disegnato, quindi non esiste un secondo sistema di menu.</p>
 */
public enum ShopMenuView {

    /** Categorie e articoli, compresa la griglia Quick Buy. */
    SHOP,
    /** Gestione dei preset Quick Buy. */
    PRESETS,
    /** Menu principale della Gallina Reale. */
    CHICKEN_ROOT,
    /** Upgrade di squadra (Protezione, Affilatezza, Haste, Heal Pool). */
    TEAM_UPGRADES,
    /** Gestione delle trappole. */
    TRAPS,
    /** Potenziamenti della Gallina Reale (Vitality, Armor, Guard). */
    ROYAL_UPGRADES
}
