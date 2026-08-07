package it.legacynetwork.chickenwars.shop;

/**
 * Tipo di click ricevuto da un menu dello shop.
 *
 * <p>Rende la decisione indipendente da {@code InventoryClickEvent}, cosi' il
 * controller delle azioni resta verificabile senza un server.</p>
 */
public enum ShopClickType {

    LEFT,
    RIGHT,
    SHIFT_LEFT,
    SHIFT_RIGHT;

    /**
     * Costruisce il tipo a partire dai flag dell'evento Bukkit.
     */
    public static ShopClickType of(boolean shift, boolean right) {
        if (shift) {
            return right ? SHIFT_RIGHT : SHIFT_LEFT;
        }
        return right ? RIGHT : LEFT;
    }

    public boolean isShift() {
        return this == SHIFT_LEFT || this == SHIFT_RIGHT;
    }

    public boolean isRight() {
        return this == RIGHT || this == SHIFT_RIGHT;
    }

    /**
     * Indica se il click e' l'azione deliberata usata per le operazioni
     * distruttive, che non devono mai partire da un click sinistro semplice.
     */
    public boolean isDestructive() {
        return this == SHIFT_RIGHT;
    }
}
