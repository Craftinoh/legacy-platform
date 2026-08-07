package it.legacynetwork.chickenwars.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Decisioni dei menu Quick Buy, verificate senza server.
 *
 * <p>Il controller e' la sola sede delle regole di interazione: cosa acquista,
 * cosa assegna, cosa elimina e cosa non deve fare nulla.</p>
 */
class QuickBuyMenuControllerTest {

    private static final int QUICK_BUY_SLOT = 19;
    private static final int OTHER_QUICK_BUY_SLOT = 25;

    private QuickBuyMenuController controller;
    private List<String> presets;

    @BeforeEach
    void setUp() {
        controller = new QuickBuyMenuController();
        presets = Arrays.asList("default", "rush", "difesa");
    }

    // ------------------------------------------------------------------
    // Categorie normali dello shop
    // ------------------------------------------------------------------

    @Test
    void ilClickSinistroSuUnArticoloAcquista() {
        QuickBuyAction action = controller.resolveShopClick(20,
                ShopClickType.LEFT, "wool");

        assertSame(QuickBuyAction.Type.PURCHASE, action.getType());
        assertEquals("wool", action.getItemId());
    }

    @Test
    void ilClickDestroSuUnArticoloAcquista() {
        assertSame(QuickBuyAction.Type.PURCHASE,
                controller.resolveShopClick(20, ShopClickType.RIGHT, "wool")
                        .getType());
    }

    @Test
    void loShiftSinistroPrendeInCaricoLArticolo() {
        QuickBuyAction action = controller.resolveShopClick(20,
                ShopClickType.SHIFT_LEFT, "wool");

        assertSame(QuickBuyAction.Type.PICK_UP_ITEM, action.getType());
        assertEquals("wool", action.getItemId());
        // Lo slot viene scelto dopo, nella griglia Quick Buy.
        assertEquals(-1, action.getSlot());
    }

    @Test
    void unoSlotSenzaArticoloNonProduceAzioni() {
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolveShopClick(20, ShopClickType.LEFT, null)
                        .getType());
    }

    @Test
    void iClickSopraLAreaArticoliVengonoIgnorati() {
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolveShopClick(4, ShopClickType.LEFT, "wool")
                        .getType());
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolveShopClick(12, ShopClickType.LEFT, "wool")
                        .getType());
    }

    // ------------------------------------------------------------------
    // Griglia Quick Buy
    // ------------------------------------------------------------------

    @Test
    void ilClickSuUnoSlotAssegnatoAcquista() {
        QuickBuyAction action = controller.resolveQuickBuyClick(QUICK_BUY_SLOT,
                ShopClickType.LEFT, "wool", null);

        assertSame(QuickBuyAction.Type.PURCHASE, action.getType());
        assertEquals("wool", action.getItemId());
    }

    @Test
    void loShiftDestroSuUnoSlotAssegnatoLoSvuota() {
        QuickBuyAction action = controller.resolveQuickBuyClick(QUICK_BUY_SLOT,
                ShopClickType.SHIFT_RIGHT, "wool", null);

        assertSame(QuickBuyAction.Type.CLEAR_SLOT, action.getType());
        assertEquals(QUICK_BUY_SLOT, action.getSlot());
    }

    @Test
    void loShiftSinistroNonSvuotaUnoSlot() {
        // Solo l'azione deliberata cancella: evita perdite accidentali.
        assertSame(QuickBuyAction.Type.PURCHASE,
                controller.resolveQuickBuyClick(QUICK_BUY_SLOT,
                        ShopClickType.SHIFT_LEFT, "wool", null).getType());
    }

    @Test
    void unoSlotVuotoSenzaArticoloInAttesaNonFaNulla() {
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolveQuickBuyClick(QUICK_BUY_SLOT,
                        ShopClickType.LEFT, null, null).getType());
    }

    @Test
    void conUnArticoloInAttesaOgniSlotDiventaDestinazione() {
        QuickBuyAction action = controller.resolveQuickBuyClick(QUICK_BUY_SLOT,
                ShopClickType.LEFT, null, "tnt");

        assertSame(QuickBuyAction.Type.PLACE_ITEM, action.getType());
        assertEquals(QUICK_BUY_SLOT, action.getSlot());
        assertEquals("tnt", action.getItemId());
    }

    @Test
    void unArticoloInAttesaSostituisceQuelloPresente() {
        QuickBuyAction action = controller.resolveQuickBuyClick(QUICK_BUY_SLOT,
                ShopClickType.LEFT, "wool", "tnt");

        // Nessun acquisto accidentale del vecchio articolo.
        assertSame(QuickBuyAction.Type.PLACE_ITEM, action.getType());
        assertEquals("tnt", action.getItemId());
    }

    @Test
    void ilPulsanteGestionePresetApreLaVista() {
        assertSame(QuickBuyAction.Type.OPEN_PRESETS,
                controller.resolveQuickBuyClick(
                        QuickBuyMenuController.PRESETS_BUTTON_SLOT,
                        ShopClickType.LEFT, null, null).getType());
    }

    @Test
    void ilPulsanteRitornoRiapreLoShop() {
        assertSame(QuickBuyAction.Type.OPEN_SHOP,
                controller.resolveQuickBuyClick(
                        QuickBuyMenuController.BACK_TO_SHOP_SLOT,
                        ShopClickType.LEFT, null, null).getType());
    }

    @Test
    void gliSlotFuoriDallaGrigliaNonSonoDestinazioni() {
        // 44 non appartiene alla griglia: nemmeno con un articolo in attesa.
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolveQuickBuyClick(44, ShopClickType.LEFT,
                        null, "tnt").getType());
    }

    // ------------------------------------------------------------------
    // Gestione preset
    // ------------------------------------------------------------------

    @Test
    void ilClickSinistroSelezionaIlPreset() {
        QuickBuyAction action = controller.resolvePresetClick(
                controller.presetSlot(1), ShopClickType.LEFT, presets);

        assertSame(QuickBuyAction.Type.SELECT_PRESET, action.getType());
        assertEquals("rush", action.getPresetId());
    }

    @Test
    void loShiftDestroEliminaIlPreset() {
        QuickBuyAction action = controller.resolvePresetClick(
                controller.presetSlot(2), ShopClickType.SHIFT_RIGHT, presets);

        assertSame(QuickBuyAction.Type.DELETE_PRESET, action.getType());
        assertEquals("difesa", action.getPresetId());
    }

    @Test
    void ilClickDestroSempliceNonEliminaNulla() {
        // L'eliminazione accidentale deve essere impossibile.
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolvePresetClick(controller.presetSlot(0),
                        ShopClickType.RIGHT, presets).getType());
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolvePresetClick(controller.presetSlot(0),
                        ShopClickType.SHIFT_LEFT, presets).getType());
    }

    @Test
    void ilPulsanteCreaProduceLAzioneDiCreazione() {
        assertSame(QuickBuyAction.Type.CREATE_PRESET,
                controller.resolvePresetClick(
                        QuickBuyMenuController.CREATE_PRESET_SLOT,
                        ShopClickType.LEFT, presets).getType());
    }

    @Test
    void ilPulsanteRitornoRiapreIlQuickBuy() {
        assertSame(QuickBuyAction.Type.OPEN_QUICK_BUY,
                controller.resolvePresetClick(
                        QuickBuyMenuController.BACK_TO_QUICK_BUY_SLOT,
                        ShopClickType.LEFT, presets).getType());
    }

    @Test
    void unoSlotSenzaPresetNonProduceAzioni() {
        List<String> single = Collections.singletonList("default");

        assertSame(QuickBuyAction.Type.NONE,
                controller.resolvePresetClick(controller.presetSlot(2),
                        ShopClickType.LEFT, single).getType());
    }

    @Test
    void unaListaVuotaNonProduceAzioni() {
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolvePresetClick(controller.presetSlot(0),
                        ShopClickType.LEFT, Collections.<String>emptyList())
                        .getType());
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolvePresetClick(controller.presetSlot(0),
                        ShopClickType.LEFT, null).getType());
    }

    // ------------------------------------------------------------------
    // Robustezza
    // ------------------------------------------------------------------

    @Test
    void unClickNulloNonProduceMaiAzioni() {
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolveShopClick(20, null, "wool").getType());
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolveQuickBuyClick(QUICK_BUY_SLOT, null,
                        "wool", null).getType());
        assertSame(QuickBuyAction.Type.NONE,
                controller.resolvePresetClick(controller.presetSlot(0), null,
                        presets).getType());
    }

    @Test
    void gliSlotDeiPresetSonoDistintiEReversibili() {
        for (int index = 0; index < controller.getVisiblePresetCapacity();
                index++) {
            int slot = controller.presetSlot(index);
            assertTrue(slot >= 0);
            assertEquals(index, controller.presetIndexOf(slot));
        }
        assertEquals(-1, controller.presetSlot(
                controller.getVisiblePresetCapacity()));
        assertEquals(-1, controller.presetIndexOf(
                QuickBuyMenuController.CREATE_PRESET_SLOT));
    }

    @Test
    void iPulsantiDiNavigazioneNonInvadonoLaGriglia() {
        // Un pulsante dentro la griglia renderebbe uno slot inutilizzabile.
        assertTrue(QuickBuyService.isQuickBuySlot(QUICK_BUY_SLOT));
        assertTrue(QuickBuyService.isQuickBuySlot(OTHER_QUICK_BUY_SLOT));
        assertTrue(!QuickBuyService.isQuickBuySlot(
                QuickBuyMenuController.PRESETS_BUTTON_SLOT));
        assertTrue(!QuickBuyService.isQuickBuySlot(
                QuickBuyMenuController.BACK_TO_SHOP_SLOT));
        assertTrue(!QuickBuyService.isQuickBuySlot(
                QuickBuyMenuController.CREATE_PRESET_SLOT));
    }

    @Test
    void ilTipoDiClickRiflettteIFlagBukkit() {
        assertSame(ShopClickType.LEFT, ShopClickType.of(false, false));
        assertSame(ShopClickType.RIGHT, ShopClickType.of(false, true));
        assertSame(ShopClickType.SHIFT_LEFT, ShopClickType.of(true, false));
        assertSame(ShopClickType.SHIFT_RIGHT, ShopClickType.of(true, true));
        assertTrue(ShopClickType.SHIFT_RIGHT.isDestructive());
        assertTrue(!ShopClickType.SHIFT_LEFT.isDestructive());
        assertTrue(!ShopClickType.RIGHT.isDestructive());
    }

    @Test
    void unAzioneVuotaNonPortaDati() {
        QuickBuyAction none = QuickBuyAction.none();

        assertTrue(none.is(QuickBuyAction.Type.NONE));
        assertEquals(-1, none.getSlot());
        assertNull(none.getItemId());
        assertNull(none.getPresetId());
    }
}
