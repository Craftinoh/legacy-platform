package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.mode.ModeProfileRegistry;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.player.equipment.ArmorTier;
import it.legacynetwork.chickenwars.player.equipment.ToolTier;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parsing e validazione del nuovo formato {@code shop.yml}.
 */
class ShopConfigLoaderTest {

    private YamlConfiguration parse(String yaml) throws Exception {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(yaml);
        return configuration;
    }

    private ShopConfiguration load(String yaml) throws Exception {
        return ShopConfigLoader.load(parse(yaml));
    }

    private String validYaml() {
        return "categories:\n"
                + "  blocks:\n"
                + "    icon: WOOL\n"
                + "    slot: 1\n"
                + "  armor:\n"
                + "    icon: IRON_CHESTPLATE\n"
                + "    slot: 2\n"
                + "items:\n"
                + "  wool:\n"
                + "    category: blocks\n"
                + "    material: WOOL\n"
                + "    amount: 16\n"
                + "    team-color: true\n"
                + "    slot: 19\n"
                + "  armor_iron:\n"
                + "    category: armor\n"
                + "    material: IRON_BOOTS\n"
                + "    tier: ARMOR\n"
                + "    level: IRON\n"
                + "pricing:\n"
                + "  solo_duel:\n"
                + "    wool: {currency: IRON, amount: 4}\n"
                + "    armor_iron: {currency: GOLD, amount: 12}\n"
                + "  doubles:\n"
                + "    wool: {currency: IRON, amount: 6}\n"
                + "    armor_iron: {currency: GOLD, amount: 14}\n"
                + "  trio:\n"
                + "    wool: {currency: IRON, amount: 8}\n"
                + "    armor_iron: {currency: GOLD, amount: 16}\n";
    }

    @Test
    void leggeCategorieArticoliEProfili() throws Exception {
        ShopConfiguration configuration = load(validYaml());

        assertTrue(configuration.getWarnings().isEmpty(),
                configuration.getWarnings().toString());
        assertEquals(2, configuration.getCategories().size());
        assertEquals(2, configuration.getItems().size());
        assertEquals(3, configuration.getProfiles().size());

        ShopItemDefinition wool = configuration.getItem("wool");
        assertNotNull(wool);
        assertSame(Material.WOOL, wool.getMaterial());
        assertEquals(16, wool.getAmount());
        assertTrue(wool.isTeamColored());
        assertSame(ShopTierKind.CONSUMABLE, wool.getTierKind());

        ShopItemDefinition armor = configuration.getItem("armor_iron");
        assertNotNull(armor);
        assertSame(ShopTierKind.ARMOR, armor.getTierKind());
        assertSame(ArmorTier.IRON, armor.getArmorTier());
        // Gli articoli con tier non sono ripetibili se non dichiarato.
        assertFalse(armor.isRepeatable());
    }

    @Test
    void ogniModalitaRisolveIlProprioProfilo() throws Exception {
        ShopConfiguration configuration = load(validYaml());
        ModeProfileRegistry registry = ModeProfileRegistry.defaults();

        assertEquals(4, configuration.resolveCost("wool",
                registry.get(MatchMode.SOLO)).getAmount());
        assertEquals(6, configuration.resolveCost("wool",
                registry.get(MatchMode.DOUBLES)).getAmount());
        assertEquals(8, configuration.resolveCost("wool",
                registry.get(MatchMode.TRIO)).getAmount());
    }

    @Test
    void duelloESoloCondividonoLoStessoListino() throws Exception {
        ShopConfiguration configuration = load(validYaml());
        ModeProfileRegistry registry = ModeProfileRegistry.defaults();

        assertEquals("solo_duel",
                registry.get(MatchMode.DUEL).getPricingProfile());
        assertEquals("solo_duel",
                registry.get(MatchMode.SOLO).getPricingProfile());
        assertEquals(configuration.resolveCost("wool",
                        registry.get(MatchMode.DUEL)),
                configuration.resolveCost("wool", registry.get(MatchMode.SOLO)));
    }

    @Test
    void rifiutaIPrezziNegativi() throws Exception {
        ShopConfiguration configuration = load(validYaml()
                .replace("wool: {currency: IRON, amount: 4}",
                        "wool: {currency: IRON, amount: -1}"));

        assertNull(configuration.resolveCost("wool", "solo_duel"));
        assertTrue(containsWarning(configuration, "prezzo negativo"));
    }

    @Test
    void rifiutaLeValuteSconosciute() throws Exception {
        ShopConfiguration configuration = load(validYaml()
                .replace("wool: {currency: IRON, amount: 4}",
                        "wool: {currency: RUBINI, amount: 4}"));

        assertNull(configuration.resolveCost("wool", "solo_duel"));
        assertTrue(containsWarning(configuration, "valuta sconosciuta"));
    }

    @Test
    void scartaSoloLArticoloConMaterialeNonValido() throws Exception {
        ShopConfiguration configuration = load(validYaml()
                .replace("    material: WOOL\n", "    material: NON_ESISTE\n"));

        assertNull(configuration.getItem("wool"));
        // Il resto del catalogo resta utilizzabile.
        assertNotNull(configuration.getItem("armor_iron"));
        assertTrue(containsWarning(configuration, "materiale non valido"));
    }

    @Test
    void scartaGliArticoliConCategoriaSconosciuta() throws Exception {
        ShopConfiguration configuration = load(validYaml()
                .replace("    category: blocks\n", "    category: inesistente\n"));

        assertNull(configuration.getItem("wool"));
        assertTrue(containsWarning(configuration, "categoria sconosciuta"));
    }

    @Test
    void richiedeUnLivelloValidoPerGliArticoliConTier() throws Exception {
        ShopConfiguration senzaLivello = load(validYaml()
                .replace("    level: IRON\n", ""));
        assertNull(senzaLivello.getItem("armor_iron"));
        assertTrue(containsWarning(senzaLivello, "manca 'level'"));

        ShopConfiguration livelloErrato = load(validYaml()
                .replace("    level: IRON\n", "    level: MITHRIL\n"));
        assertNull(livelloErrato.getItem("armor_iron"));
        assertTrue(containsWarning(livelloErrato, "non valido per il tier"));
    }

    @Test
    void rifiutaIlLivelloNoneComeAcquistabile() throws Exception {
        ShopConfiguration configuration = load(validYaml()
                .replace("    tier: ARMOR\n    level: IRON\n",
                        "    tier: PICKAXE\n    level: NONE\n"));

        assertNull(configuration.getItem("armor_iron"));
        assertTrue(containsWarning(configuration,
                "non e' un livello acquistabile"));
    }

    @Test
    void segnalaGliArticoliSenzaPrezzoInUnProfilo() throws Exception {
        ShopConfiguration configuration = load(validYaml()
                .replace("    wool: {currency: IRON, amount: 8}\n", ""));

        assertNull(configuration.resolveCost("wool", "trio"));
        assertTrue(containsWarning(configuration, "senza prezzo"));
    }

    @Test
    void segnalaIProfiliMancanti() throws Exception {
        ShopConfiguration configuration = load(validYaml()
                .replace("  trio:\n"
                        + "    wool: {currency: IRON, amount: 8}\n"
                        + "    armor_iron: {currency: GOLD, amount: 16}\n", ""));

        assertNull(configuration.getProfile("trio"));
        assertTrue(containsWarning(configuration,
                "profilo prezzi 'trio' mancante"));
    }

    @Test
    void unaConfigurazioneAssenteNonSollevaEccezioni() {
        ShopConfiguration configuration = ShopConfigLoader.load(null);

        assertTrue(configuration.isEmpty());
        assertFalse(configuration.getWarnings().isEmpty());
    }

    @Test
    void convertIlFormatoStoricoApplicandoloATuttiIProfili() throws Exception {
        ShopConfiguration configuration = load(
                "categories:\n"
                        + "  blocks:\n"
                        + "    icon: WOOL\n"
                        + "    slot: 1\n"
                        + "    items:\n"
                        + "      wool:\n"
                        + "        material: WOOL\n"
                        + "        amount: 16\n"
                        + "        currency: IRON\n"
                        + "        price: 4\n");

        assertNotNull(configuration.getItem("wool"));
        for (String profileId : ShopConfigLoader.EXPECTED_PROFILES) {
            ItemCost cost = configuration.resolveCost("wool", profileId);
            assertNotNull(cost, "profilo " + profileId);
            assertSame(ResourceType.IRON, cost.getCurrency());
            assertEquals(4, cost.getAmount());
        }
        assertTrue(containsWarning(configuration, "formato shop.yml storico"));
    }

    @Test
    void interpretaITierDegliStrumenti() throws Exception {
        ShopConfiguration configuration = load(validYaml()
                .replace("    tier: ARMOR\n    level: IRON\n",
                        "    tier: PICKAXE\n    level: TIER_3\n"));

        ShopItemDefinition pickaxe = configuration.getItem("armor_iron");
        assertNotNull(pickaxe);
        assertSame(ShopTierKind.PICKAXE, pickaxe.getTierKind());
        assertSame(ToolTier.TIER_3, pickaxe.getToolTier());
        assertEquals(3, pickaxe.getTierLevel());
    }

    private boolean containsWarning(ShopConfiguration configuration,
                                    String fragment) {
        for (String warning : configuration.getWarnings()) {
            if (warning.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
