package it.legacynetwork.chickenwars.player.equipment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerEquipmentStateTest {

    @Test
    void armorOnlyMovesToHigherPermanentTiers() {
        PlayerEquipmentState state = new PlayerEquipmentState();

        assertTrue(state.upgradeArmor(ArmorTier.IRON));
        assertEquals(ArmorTier.IRON, state.getArmorTier());
        assertFalse(state.upgradeArmor(ArmorTier.CHAINMAIL));
        assertFalse(state.upgradeArmor(ArmorTier.IRON));
        assertEquals(ArmorTier.IRON, state.getArmorTier());
    }

    @Test
    void deathDowngradesToolsExactlyOnceAndKeepsPermanentItems() {
        PlayerEquipmentState state = new PlayerEquipmentState();
        state.unlockShears();
        state.upgradeArmor(ArmorTier.DIAMOND);
        state.upgradePickaxe(ToolTier.TIER_4);
        state.upgradeAxe(ToolTier.TIER_3);
        state.upgradeSword(SwordTier.DIAMOND);

        assertTrue(state.applyDeath(1L));
        assertEquals(ToolTier.TIER_3, state.getPickaxeTier());
        assertEquals(ToolTier.TIER_2, state.getAxeTier());
        assertEquals(SwordTier.WOOD, state.getSwordTier());
        assertEquals(ArmorTier.DIAMOND, state.getArmorTier());
        assertTrue(state.ownsShears());

        assertFalse(state.applyDeath(1L));
        assertEquals(ToolTier.TIER_3, state.getPickaxeTier());
        assertEquals(ToolTier.TIER_2, state.getAxeTier());
    }

    @Test
    void toolsNeverDowngradeBelowFirstTier() {
        PlayerEquipmentState state = new PlayerEquipmentState();
        state.upgradePickaxe(ToolTier.TIER_1);
        state.upgradeAxe(ToolTier.TIER_1);

        state.applyDeath(1L);
        state.applyDeath(2L);

        assertEquals(ToolTier.TIER_1, state.getPickaxeTier());
        assertEquals(ToolTier.TIER_1, state.getAxeTier());
    }

    @Test
    void reconnectDoesNotDowngradeTools() {
        PlayerEquipmentState state = new PlayerEquipmentState();
        state.upgradePickaxe(ToolTier.TIER_3);
        state.upgradeAxe(ToolTier.TIER_2);
        state.upgradeSword(SwordTier.IRON);

        state.prepareReconnect();

        assertEquals(ToolTier.TIER_3, state.getPickaxeTier());
        assertEquals(ToolTier.TIER_2, state.getAxeTier());
        assertEquals(SwordTier.WOOD, state.getSwordTier());
    }
}
