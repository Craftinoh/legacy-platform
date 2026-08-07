package it.legacynetwork.chickenwars.player;

import it.legacynetwork.chickenwars.player.equipment.ArmorTier;
import it.legacynetwork.chickenwars.player.equipment.SwordTier;
import it.legacynetwork.chickenwars.player.equipment.ToolTier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conservazione dello stato permanente tra logout e rientro.
 */
class ReconnectServiceTest {

    private PlayerSession session(UUID playerId, String arenaId) {
        return new PlayerSession(playerId, "Tester", arenaId, null);
    }

    @Test
    void conservaSoloGliElementiPermanenti() {
        ReconnectService service = new ReconnectService();
        UUID playerId = UUID.randomUUID();

        PlayerSession first = session(playerId, "arena");
        first.setTeamId("red");
        first.getEquipmentState().upgradeArmor(ArmorTier.IRON);
        first.getEquipmentState().unlockShears();
        first.getEquipmentState().upgradePickaxe(ToolTier.TIER_3);
        first.getEquipmentState().upgradeAxe(ToolTier.TIER_2);
        first.getEquipmentState().upgradeSword(SwordTier.DIAMOND);
        first.getEquipmentState().selectQuickBuyPreset("rush");

        service.remember(first);

        PlayerSession second = session(playerId, "arena");
        assertNotNull(service.restore(second));

        assertSame(ArmorTier.IRON, second.getEquipmentState().getArmorTier());
        assertTrue(second.getEquipmentState().ownsShears());
        assertSame(ToolTier.TIER_3, second.getEquipmentState().getPickaxeTier());
        assertSame(ToolTier.TIER_2, second.getEquipmentState().getAxeTier());
        assertSame("rush",
                second.getEquipmentState().getSelectedQuickBuyPreset());
        assertSame("red", second.getTeamId());

        // La spada acquistata non e' permanente.
        assertSame(SwordTier.WOOD, second.getEquipmentState().getSwordTier());
    }

    @Test
    void ilRientroNonApplicaUnSecondoDowngrade() {
        ReconnectService service = new ReconnectService();
        UUID playerId = UUID.randomUUID();

        PlayerSession first = session(playerId, "arena");
        first.getEquipmentState().upgradePickaxe(ToolTier.TIER_4);
        service.remember(first);

        PlayerSession second = session(playerId, "arena");
        service.restore(second);

        assertSame(ToolTier.TIER_4, second.getEquipmentState().getPickaxeTier());
        assertFalse(second.isDeathInProgress());
    }

    @Test
    void nonRipristinaSuUnArenaDiversa() {
        ReconnectService service = new ReconnectService();
        UUID playerId = UUID.randomUUID();

        PlayerSession first = session(playerId, "arena");
        first.getEquipmentState().upgradeArmor(ArmorTier.DIAMOND);
        service.remember(first);

        assertFalse(service.canRestore(playerId, "altra_arena"));
        assertNull(service.restore(session(playerId, "altra_arena")));
    }

    @Test
    void loStatoVieneConsumatoUnaVoltaSola() {
        ReconnectService service = new ReconnectService();
        UUID playerId = UUID.randomUUID();

        PlayerSession first = session(playerId, "arena");
        first.getEquipmentState().upgradeArmor(ArmorTier.CHAINMAIL);
        service.remember(first);

        assertNotNull(service.restore(session(playerId, "arena")));
        assertNull(service.restore(session(playerId, "arena")));
        assertFalse(service.hasSnapshot(playerId));
    }

    @Test
    void dimenticareImpedisceIlRipristino() {
        ReconnectService service = new ReconnectService();
        UUID playerId = UUID.randomUUID();

        service.remember(session(playerId, "arena"));
        service.forget(playerId);

        assertFalse(service.canRestore(playerId, "arena"));
    }
}
