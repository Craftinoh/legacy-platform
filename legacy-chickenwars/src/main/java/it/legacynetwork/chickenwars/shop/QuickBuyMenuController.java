package it.legacynetwork.chickenwars.shop;

import java.util.List;

/**
 * Traduce i click dei menu in azioni, senza toccare Bukkit.
 *
 * <p>Contiene la disposizione dei pulsanti e le regole di interazione: quali
 * click acquistano, quali assegnano, quali eliminano. Essendo priva di stato e
 * di dipendenze dal server, e' interamente verificabile con test unitari.</p>
 */
public final class QuickBuyMenuController {

    /** Primo slot utilizzabile per contenuti, sotto il separatore. */
    public static final int FIRST_CONTENT_SLOT = 18;

    /** Ultimo slot dell'inventario a sei righe. */
    public static final int LAST_SLOT = 53;

    /** Apre la gestione preset dalla griglia Quick Buy. */
    public static final int PRESETS_BUTTON_SLOT = 45;

    /** Torna alla griglia Quick Buy dalla gestione preset. */
    public static final int BACK_TO_QUICK_BUY_SLOT = 45;

    /** Crea un nuovo preset. */
    public static final int CREATE_PRESET_SLOT = 49;

    /** Torna alle categorie dello shop. */
    public static final int BACK_TO_SHOP_SLOT = 53;

    /** Slot occupati dalle schede dei preset. */
    private static final int[] PRESET_SLOTS = {
            19, 20, 21, 22, 23, 24, 25
    };

    /**
     * Click su una categoria normale dello shop.
     *
     * @param slot         slot cliccato
     * @param click        tipo di click
     * @param itemIdAtSlot articolo presente nello slot, eventualmente nullo
     * @return l'azione da eseguire, mai nulla
     */
    public QuickBuyAction resolveShopClick(int slot, ShopClickType click,
                                           String itemIdAtSlot) {
        if (slot < FIRST_CONTENT_SLOT || slot > LAST_SLOT
                || itemIdAtSlot == null || click == null) {
            return QuickBuyAction.none();
        }
        // Lo shift sinistro prende in carico l'articolo: lo slot di
        // destinazione viene poi scelto dal giocatore nel Quick Buy.
        if (click == ShopClickType.SHIFT_LEFT) {
            return QuickBuyAction.withItem(
                    QuickBuyAction.Type.PICK_UP_ITEM, -1, itemIdAtSlot);
        }
        return QuickBuyAction.withItem(
                QuickBuyAction.Type.PURCHASE, slot, itemIdAtSlot);
    }

    /**
     * Click sulla griglia Quick Buy.
     *
     * @param slot           slot cliccato
     * @param click          tipo di click
     * @param itemIdAtSlot   articolo assegnato allo slot, eventualmente nullo
     * @param pendingItemId  articolo in attesa di collocazione, eventualmente
     *                       nullo
     * @return l'azione da eseguire, mai nulla
     */
    public QuickBuyAction resolveQuickBuyClick(int slot, ShopClickType click,
                                               String itemIdAtSlot,
                                               String pendingItemId) {
        if (click == null || slot < FIRST_CONTENT_SLOT || slot > LAST_SLOT) {
            return QuickBuyAction.none();
        }
        if (slot == PRESETS_BUTTON_SLOT) {
            return QuickBuyAction.simple(QuickBuyAction.Type.OPEN_PRESETS);
        }
        if (slot == BACK_TO_SHOP_SLOT) {
            return QuickBuyAction.simple(QuickBuyAction.Type.OPEN_SHOP);
        }
        if (!QuickBuyService.isQuickBuySlot(slot)) {
            return QuickBuyAction.none();
        }

        // Con un articolo in attesa ogni slot diventa una destinazione: e' cosi'
        // che il giocatore sceglie la posizione, sostituendo l'eventuale
        // articolo gia' presente.
        if (pendingItemId != null) {
            return QuickBuyAction.withItem(
                    QuickBuyAction.Type.PLACE_ITEM, slot, pendingItemId);
        }
        if (itemIdAtSlot == null) {
            return QuickBuyAction.none();
        }
        // Solo l'azione deliberata svuota lo slot: un click sinistro distratto
        // non deve mai cancellare una configurazione.
        if (click.isDestructive()) {
            return QuickBuyAction.onSlot(QuickBuyAction.Type.CLEAR_SLOT, slot);
        }
        return QuickBuyAction.withItem(
                QuickBuyAction.Type.PURCHASE, slot, itemIdAtSlot);
    }

    /**
     * Click sulla gestione preset.
     *
     * @param slot      slot cliccato
     * @param click     tipo di click
     * @param presetIds preset del giocatore, nell'ordine mostrato
     * @return l'azione da eseguire, mai nulla
     */
    public QuickBuyAction resolvePresetClick(int slot, ShopClickType click,
                                             List<String> presetIds) {
        if (click == null) {
            return QuickBuyAction.none();
        }
        if (slot == CREATE_PRESET_SLOT) {
            return QuickBuyAction.simple(QuickBuyAction.Type.CREATE_PRESET);
        }
        if (slot == BACK_TO_QUICK_BUY_SLOT) {
            return QuickBuyAction.simple(QuickBuyAction.Type.OPEN_QUICK_BUY);
        }
        if (slot == BACK_TO_SHOP_SLOT) {
            return QuickBuyAction.simple(QuickBuyAction.Type.OPEN_SHOP);
        }

        int index = presetIndexOf(slot);
        if (index < 0 || presetIds == null || index >= presetIds.size()) {
            return QuickBuyAction.none();
        }
        String presetId = presetIds.get(index);

        if (click.isDestructive()) {
            return QuickBuyAction.onPreset(
                    QuickBuyAction.Type.DELETE_PRESET, presetId);
        }
        if (click == ShopClickType.LEFT) {
            return QuickBuyAction.onPreset(
                    QuickBuyAction.Type.SELECT_PRESET, presetId);
        }
        // Gli altri click non producono effetti: evita selezioni involontarie.
        return QuickBuyAction.none();
    }

    /**
     * Slot che ospita la scheda del preset di indice indicato.
     *
     * @return lo slot, oppure {@code -1} se l'indice eccede lo spazio
     */
    public int presetSlot(int index) {
        return index >= 0 && index < PRESET_SLOTS.length
                ? PRESET_SLOTS[index] : -1;
    }

    /**
     * Indice del preset mostrato nello slot indicato.
     *
     * @return l'indice, oppure {@code -1} se lo slot non ospita un preset
     */
    public int presetIndexOf(int slot) {
        for (int index = 0; index < PRESET_SLOTS.length; index++) {
            if (PRESET_SLOTS[index] == slot) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Numero massimo di preset rappresentabili contemporaneamente.
     */
    public int getVisiblePresetCapacity() {
        return PRESET_SLOTS.length;
    }
}
