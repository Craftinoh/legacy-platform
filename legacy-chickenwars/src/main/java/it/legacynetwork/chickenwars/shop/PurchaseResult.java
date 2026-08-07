package it.legacynetwork.chickenwars.shop;

/**
 * Esito di un tentativo di acquisto.
 *
 * <p>Ogni esito corrisponde a una chiave localizzata, cosi' il messaggio
 * mostrato al giocatore resta fuori dal codice Java.</p>
 */
public enum PurchaseResult {

    /** Acquisto completato e articolo consegnato. */
    SUCCESS("shop.purchased"),
    /** Il giocatore possiede gia' esattamente questo tier. */
    ALREADY_OWNED("shop.already-owned"),
    /** Il tier richiesto e' inferiore a quello posseduto. */
    LOWER_TIER("shop.lower-tier"),
    /** Risorse insufficienti. */
    NOT_ENOUGH("shop.not-enough"),
    /** Nessuno slot libero per consegnare l'articolo. */
    INVENTORY_FULL("shop.inventory-full"),
    /** Articolo privo di prezzo nel profilo economico della partita. */
    UNAVAILABLE("shop.unavailable-item"),
    /** Permesso mancante. */
    NO_PERMISSION("shop.no-permission");

    private final String messageKey;

    PurchaseResult(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
