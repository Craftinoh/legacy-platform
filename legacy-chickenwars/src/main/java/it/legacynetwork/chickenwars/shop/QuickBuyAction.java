package it.legacynetwork.chickenwars.shop;

/**
 * Azione decisa da un click, prima di qualunque effetto su Bukkit.
 *
 * <p>Il controller produce questo valore e il servizio lo esegue: la decisione
 * resta cosi' verificabile con dati puri.</p>
 */
public final class QuickBuyAction {

    /** Tipologie di azione riconosciute dai menu Quick Buy. */
    public enum Type {
        /** Nessun effetto. */
        NONE,
        /** Acquisto dell'articolo nello slot. */
        PURCHASE,
        /** Presa in carico di un articolo da collocare in uno slot. */
        PICK_UP_ITEM,
        /** Collocazione dell'articolo in attesa nello slot indicato. */
        PLACE_ITEM,
        /** Svuotamento dello slot indicato. */
        CLEAR_SLOT,
        /** Apertura della gestione preset. */
        OPEN_PRESETS,
        /** Ritorno alla griglia Quick Buy. */
        OPEN_QUICK_BUY,
        /** Ritorno alle categorie dello shop. */
        OPEN_SHOP,
        /** Creazione di un nuovo preset. */
        CREATE_PRESET,
        /** Selezione del preset indicato. */
        SELECT_PRESET,
        /** Eliminazione del preset indicato. */
        DELETE_PRESET
    }

    private static final QuickBuyAction NOTHING =
            new QuickBuyAction(Type.NONE, -1, null, null);

    private final Type type;
    private final int slot;
    private final String itemId;
    private final String presetId;

    private QuickBuyAction(Type type, int slot, String itemId,
                           String presetId) {
        this.type = type;
        this.slot = slot;
        this.itemId = itemId;
        this.presetId = presetId;
    }

    public static QuickBuyAction none() {
        return NOTHING;
    }

    static QuickBuyAction simple(Type type) {
        return new QuickBuyAction(type, -1, null, null);
    }

    static QuickBuyAction onSlot(Type type, int slot) {
        return new QuickBuyAction(type, slot, null, null);
    }

    static QuickBuyAction withItem(Type type, int slot, String itemId) {
        return new QuickBuyAction(type, slot, itemId, null);
    }

    static QuickBuyAction onPreset(Type type, String presetId) {
        return new QuickBuyAction(type, -1, null, presetId);
    }

    public Type getType() {
        return type;
    }

    /**
     * @return lo slot coinvolto, oppure {@code -1} se l'azione non lo usa
     */
    public int getSlot() {
        return slot;
    }

    /**
     * @return l'articolo coinvolto, oppure {@code null}
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * @return il preset coinvolto, oppure {@code null}
     */
    public String getPresetId() {
        return presetId;
    }

    public boolean is(Type expected) {
        return type == expected;
    }

    @Override
    public String toString() {
        return "QuickBuyAction{" + type + ", slot=" + slot
                + ", item=" + itemId + ", preset=" + presetId + '}';
    }
}
