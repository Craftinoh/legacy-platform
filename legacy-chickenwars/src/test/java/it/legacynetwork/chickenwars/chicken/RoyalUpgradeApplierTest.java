package it.legacynetwork.chickenwars.chicken;

import it.legacynetwork.chickenwars.model.SimpleLocation;
import it.legacynetwork.chickenwars.upgrade.RoyalUpgradeType;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import it.legacynetwork.chickenwars.upgrade.UpgradeConfigLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoyalUpgradeApplierTest {

    private TeamUpgradeService upgrades;
    private RoyalUpgradeApplier applier;
    private RoyalChicken chicken;

    @BeforeEach
    void setUp() throws Exception {
        upgrades = new TeamUpgradeService();
        String yaml = "team-upgrades:\n"
                + "  HASTE:\n"
                + "    icon: GOLD_PICKAXE\n"
                + "    effect: FAST_DIGGING\n"
                + "    levels:\n"
                + "      1:\n"
                + "        amplifier: 0\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 2}\n"
                + "royal-upgrades:\n"
                + "  ROYAL_ARMOR:\n"
                + "    icon: IRON_CHESTPLATE\n"
                + "    levels:\n"
                + "      1:\n"
                + "        value: 0.25\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 2}\n"
                + "      2:\n"
                + "        value: 0.25\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 4}\n"
                + "      3:\n"
                + "        value: 0.25\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 8}\n"
                + "  ROYAL_VITALITY:\n"
                + "    icon: GOLDEN_APPLE\n"
                + "    levels:\n"
                + "      1:\n"
                + "        value: 5.0\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 2}\n"
                + "      2:\n"
                + "        value: 10.0\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 4}\n"
                + "      3:\n"
                + "        value: 15.0\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 8}\n"
                + "  ROYAL_GUARD:\n"
                + "    icon: IRON_FENCE\n"
                + "    levels:\n"
                + "      1:\n"
                + "        duration-ticks: 40\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 2}\n"
                + "      2:\n"
                + "        duration-ticks: 40\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 4}\n";
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        upgrades.setCatalog(UpgradeConfigLoader.load(config));
        applier = new RoyalUpgradeApplier(upgrades);

        ChickenSettings settings = ChickenSettings.fromSection(null);
        chicken = new RoyalChicken("red",
                SimpleLocation.parse("world,0,0,0"), settings);
    }

    @Test
    void armorReductionLivelloZero() {
        double reduction = applier.resolveArmorReduction("a1", "red");
        assertEquals(0.0, reduction, 0.001);
    }

    @Test
    void armorReductionDopoUpgrade() {
        upgrades.getState("a1", "red")
                .reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_ARMOR, 3);
        double reduction = applier.resolveArmorReduction("a1", "red");
        assertEquals(0.25, reduction, 0.001);
    }

    @Test
    void armorReductionCumulativo() {
        upgrades.getState("a1", "red")
                .reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_ARMOR, 3);
        upgrades.getState("a1", "red")
                .reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_ARMOR, 3);
        double reduction = applier.resolveArmorReduction("a1", "red");
        assertEquals(0.50, reduction, 0.001);
    }

    @Test
    void vitalityAumentaMassimo() {
        upgrades.getState("a1", "red")
                .reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_VITALITY, 3);
        double before = chicken.getVitals().getMaxHealth();

        double added = applier.applyVitality("a1", "red", chicken);
        assertEquals(5.0, added, 0.001);
        assertTrue(chicken.getVitals().getMaxHealth() > before);
    }

    @Test
    void vitalityNonApplicaDueVolte() {
        upgrades.getState("a1", "red")
                .reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_VITALITY, 3);
        applier.applyVitality("a1", "red", chicken);
        double maxAfterFirst = chicken.getVitals().getMaxHealth();

        double added = applier.applyVitality("a1", "red", chicken);
        assertEquals(0.0, added, 0.001);
        assertEquals(maxAfterFirst, chicken.getVitals().getMaxHealth(), 0.001);
    }

    @Test
    void vitalitySoloDeltaNonCura() {
        upgrades.getState("a1", "red")
                .reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_VITALITY, 3);
        chicken.getVitals().applyDamage(5.0);
        double before = chicken.getVitals().getHealth();

        applier.applyVitality("a1", "red", chicken);
        assertEquals(before + 5.0, chicken.getVitals().getHealth(), 0.001);
    }

    @Test
    void vitalityNonApplicaAGallinaMorta() {
        upgrades.getState("a1", "red")
                .reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_VITALITY, 3);
        chicken.getVitals().kill();

        double added = applier.applyVitality("a1", "red", chicken);
        assertEquals(0.0, added, 0.001);
    }

    @Test
    void getRoyalLevelRitornaZeroSeNull() {
        int level = applier.getRoyalLevel("nonexistent", "nonexistent",
                RoyalUpgradeType.ROYAL_ARMOR);
        assertEquals(0, level);
    }
}
