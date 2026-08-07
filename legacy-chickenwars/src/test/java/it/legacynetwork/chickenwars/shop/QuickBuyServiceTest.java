package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.persistence.InMemoryQuickBuyRepository;
import it.legacynetwork.chickenwars.persistence.QuickBuyPresetRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickBuyServiceTest {

    private QuickBuyService service;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        service = new QuickBuyService(new InMemoryQuickBuyRepository(),
                Logger.getAnonymousLogger());
        playerId = UUID.randomUUID();
        service.load(playerId);
    }

    @Test
    void defaultPresetIsSelected() {
        QuickBuyPresetRecord selected = service.getSelected(playerId);
        assertNotNull(selected);
        assertEquals(QuickBuyService.DEFAULT_PRESET, selected.getPresetId());
    }

    @Test
    void selectPreset() {
        service.select(playerId, QuickBuyService.DEFAULT_PRESET);
        assertEquals(QuickBuyService.DEFAULT_PRESET,
                service.getSelected(playerId).getPresetId());
    }

    @Test
    void onlyOnePresetSelected() {
        int selected = 0;
        for (QuickBuyPresetRecord r : service.list(playerId)) {
            if (r.isSelected()) selected++;
        }
        assertEquals(1, selected);
    }

    @Test
    void assignItemToSlot() {
        assertTrue(service.assign(playerId, 19, "stone_sword"));
    }

    @Test
    void replaceItemInSlot() {
        service.assign(playerId, 20, "iron_sword");
        service.assign(playerId, 20, "diamond_sword");
    }

    @Test
    void removeItemFromSlot() {
        service.assign(playerId, 21, "shears");
        assertTrue(service.assign(playerId, 21, null));
    }

    @Test
    void doubleAssignSameItemIsIdempotent() {
        service.assign(playerId, 22, "axe");
        assertFalse(service.assign(playerId, 22, "axe"));
    }

    @Test
    void slotOutsideQuickBuyRangeIsRejected() {
        assertFalse(service.assign(playerId, 10, "item"));
        assertFalse(service.assign(playerId, 50, "item"));
    }

    @Test
    void itemRemovedFromConfigIsFiltered() {
        service.assign(playerId, 19, "deleted_item");
        Map<Integer, ShopItemDefinition> slots = service.resolveSlots(
                playerId, ShopConfiguration.empty());
        assertFalse(slots.containsKey(19));
    }

    @Test
    void resolveSlotsReturnsValidItems() {
        service.assign(playerId, 19, "valid_item");
        Map<Integer, ShopItemDefinition> slots = service.resolveSlots(
                playerId, ShopConfiguration.empty());
        assertEquals(0, slots.size());
    }

    @Test
    void fastSlotIsQuickBuySlot() {
        assertTrue(QuickBuyService.isQuickBuySlot(19));
        assertTrue(QuickBuyService.isQuickBuySlot(43));
    }

    @Test
    void unloadRemovesCache() {
        service.unload(playerId);
        QuickBuyPresetRecord selected = service.getSelected(playerId);
        assertNotNull(selected);
    }
}
