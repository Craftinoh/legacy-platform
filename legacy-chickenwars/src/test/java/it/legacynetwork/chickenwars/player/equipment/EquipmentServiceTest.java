package it.legacynetwork.chickenwars.player.equipment;

import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.shop.PurchaseResult;
import it.legacynetwork.chickenwars.shop.ShopConfigLoader;
import it.legacynetwork.chickenwars.shop.ShopConfiguration;
import it.legacynetwork.chickenwars.shop.ShopItemDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regole di progressione applicate dal servizio equipaggiamento.
 *
 * <p>Verifica la parte indipendente da Bukkit: confronto dei tier, downgrade
 * per morte e comportamento del reconnect.</p>
 */
class EquipmentServiceTest {

    private EquipmentService service;
    private ShopConfiguration catalog;
    private PlayerSession session;

    @BeforeEach
    void setUp() throws Exception {
        service = new EquipmentService(EquipmentSettings.fromSection(null));
        catalog = ShopConfigLoader.load(parse(
                "categories:\n"
                        + "  armor:\n"
                        + "    icon: IRON_CHESTPLATE\n"
                        + "    slot: 1\n"
                        + "  tools:\n"
                        + "    icon: IRON_PICKAXE\n"
                        + "    slot: 2\n"
                        + "items:\n"
                        + "  armor_chainmail:\n"
                        + "    category: armor\n"
                        + "    material: CHAINMAIL_BOOTS\n"
                        + "    tier: ARMOR\n"
                        + "    level: CHAINMAIL\n"
                        + "  armor_iron:\n"
                        + "    category: armor\n"
                        + "    material: IRON_BOOTS\n"
                        + "    tier: ARMOR\n"
                        + "    level: IRON\n"
                        + "  shears:\n"
                        + "    category: tools\n"
                        + "    material: SHEARS\n"
                        + "    tier: SHEARS\n"
                        + "  pickaxe_3:\n"
                        + "    category: tools\n"
                        + "    material: IRON_PICKAXE\n"
                        + "    tier: PICKAXE\n"
                        + "    level: TIER_3\n"
                        + "pricing:\n"
                        + "  solo_duel:\n"
                        + "    armor_chainmail: {currency: IRON, amount: 40}\n"
                        + "    armor_iron: {currency: GOLD, amount: 12}\n"
                        + "    shears: {currency: IRON, amount: 20}\n"
                        + "    pickaxe_3: {currency: GOLD, amount: 3}\n"
                        + "  doubles:\n"
                        + "    armor_chainmail: {currency: IRON, amount: 40}\n"
                        + "    armor_iron: {currency: GOLD, amount: 12}\n"
                        + "    shears: {currency: IRON, amount: 20}\n"
                        + "    pickaxe_3: {currency: GOLD, amount: 3}\n"
                        + "  trio:\n"
                        + "    armor_chainmail: {currency: IRON, amount: 40}\n"
                        + "    armor_iron: {currency: GOLD, amount: 12}\n"
                        + "    shears: {currency: IRON, amount: 20}\n"
                        + "    pickaxe_3: {currency: GOLD, amount: 3}\n"));
        session = new PlayerSession(UUID.randomUUID(), "Tester", "arena", null);
    }

    private YamlConfiguration parse(String yaml) throws Exception {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(yaml);
        return configuration;
    }

    private ShopItemDefinition item(String id) {
        ShopItemDefinition definition = catalog.getItem(id);
        assertNotNull(definition, "articolo mancante nel catalogo di test: " + id);
        return definition;
    }

    @Test
    void unTierSuperiorePuoEssereAcquistato() {
        assertSame(PurchaseResult.SUCCESS,
                service.checkTier(session, item("armor_chainmail")));
    }

    @Test
    void nonSiPuoAcquistareLoStessoTier() {
        session.getEquipmentState().upgradeArmor(ArmorTier.IRON);

        assertSame(PurchaseResult.ALREADY_OWNED,
                service.checkTier(session, item("armor_iron")));
    }

    @Test
    void nonSiPuoAcquistareUnTierInferiore() {
        session.getEquipmentState().upgradeArmor(ArmorTier.IRON);

        assertSame(PurchaseResult.LOWER_TIER,
                service.checkTier(session, item("armor_chainmail")));
    }

    @Test
    void leCesoieSiAcquistanoUnaVoltaSola() {
        assertSame(PurchaseResult.SUCCESS,
                service.checkTier(session, item("shears")));

        session.getEquipmentState().unlockShears();

        assertSame(PurchaseResult.ALREADY_OWNED,
                service.checkTier(session, item("shears")));
    }

    @Test
    void ilTierArmaturaSopravviveAllaMorte() {
        session.getEquipmentState().upgradeArmor(ArmorTier.DIAMOND);
        session.getEquipmentState().unlockShears();

        assertTrue(service.handleDeath(session));

        assertSame(ArmorTier.DIAMOND, session.getEquipmentState().getArmorTier());
        assertTrue(session.getEquipmentState().ownsShears());
    }

    @Test
    void laSpadaTornaAlLegnoDopoLaMorte() {
        session.getEquipmentState().upgradeSword(SwordTier.DIAMOND);

        service.handleDeath(session);

        assertSame(SwordTier.WOOD, session.getEquipmentState().getSwordTier());
    }

    @Test
    void piccioneEdAsciaScendonoDiUnTierPerMorte() {
        session.getEquipmentState().upgradePickaxe(ToolTier.TIER_4);
        session.getEquipmentState().upgradeAxe(ToolTier.TIER_3);

        service.handleDeath(session);

        assertSame(ToolTier.TIER_3, session.getEquipmentState().getPickaxeTier());
        assertSame(ToolTier.TIER_2, session.getEquipmentState().getAxeTier());
    }

    @Test
    void eventiDuplicatiSullaStessaMorteNonRaddoppianoIlDowngrade() {
        session.getEquipmentState().upgradePickaxe(ToolTier.TIER_4);

        assertTrue(service.handleDeath(session));
        // La morte non e' ancora stata chiusa: la sequenza resta la stessa.
        assertFalse(service.handleDeath(session));
        assertFalse(service.handleDeath(session));

        assertSame(ToolTier.TIER_3, session.getEquipmentState().getPickaxeTier());
    }

    @Test
    void morteDistinteApplicanoUnDowngradeCiascuna() {
        session.getEquipmentState().upgradePickaxe(ToolTier.TIER_4);

        assertTrue(service.handleDeath(session));
        session.completeDeath();
        assertTrue(service.handleDeath(session));

        assertSame(ToolTier.TIER_2, session.getEquipmentState().getPickaxeTier());
    }

    @Test
    void ilReconnectNonApplicaUnSecondoDowngrade() {
        session.getEquipmentState().upgradePickaxe(ToolTier.TIER_3);
        session.getEquipmentState().upgradeAxe(ToolTier.TIER_2);
        session.getEquipmentState().upgradeSword(SwordTier.IRON);

        service.prepareReconnect(session);

        assertSame(ToolTier.TIER_3, session.getEquipmentState().getPickaxeTier());
        assertSame(ToolTier.TIER_2, session.getEquipmentState().getAxeTier());
        assertSame(SwordTier.WOOD, session.getEquipmentState().getSwordTier());
    }

    @Test
    void gliStrumentiNonScendonoSottoIlMinimoConfigurato() throws Exception {
        EquipmentService restricted = new EquipmentService(
                EquipmentSettings.fromSection(parse(
                        "equipment:\n"
                                + "  minimum-tool-tier: TIER_2\n")
                        .getConfigurationSection("equipment")));
        session.getEquipmentState().upgradePickaxe(ToolTier.TIER_2);

        restricted.handleDeath(session);

        assertSame(ToolTier.TIER_2, session.getEquipmentState().getPickaxeTier());
    }

    @Test
    void laSequenzaDiMorteAvanzaSoloQuandoLaMorteEuChiusa() {
        assertEquals(1L, session.beginDeath());
        assertEquals(1L, session.beginDeath());
        assertTrue(session.isDeathInProgress());

        session.completeDeath();

        assertEquals(2L, session.beginDeath());
    }
}
