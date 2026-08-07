package it.legacynetwork.chickenwars;

import it.legacynetwork.chickenwars.chicken.ChickenSettings;
import it.legacynetwork.chickenwars.command.HelpTopic;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.player.equipment.ArmorTier;
import it.legacynetwork.chickenwars.player.equipment.EquipmentSettings;
import it.legacynetwork.chickenwars.player.equipment.SwordTier;
import it.legacynetwork.chickenwars.player.equipment.ToolTier;
import it.legacynetwork.chickenwars.setup.SetupTool;
import it.legacynetwork.chickenwars.shop.ShopCategoryDefinition;
import it.legacynetwork.chickenwars.shop.ShopConfigLoader;
import it.legacynetwork.chickenwars.shop.ShopConfiguration;
import it.legacynetwork.chickenwars.shop.ShopItemDefinition;
import it.legacynetwork.chickenwars.upgrade.RoyalUpgradeType;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeType;
import it.legacynetwork.chickenwars.upgrade.TrapDefinition;
import it.legacynetwork.chickenwars.upgrade.UpgradeCatalog;
import it.legacynetwork.chickenwars.upgrade.UpgradeConfigLoader;
import it.legacynetwork.chickenwars.upgrade.UpgradeResult;
import it.legacynetwork.chickenwars.world.WorldTemplate;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica che le risorse incluse nel JAR siano YAML validi e coerenti.
 *
 * <p>Un refuso in un file lingua o in shop.yml si manifesterebbe altrimenti solo
 * a server avviato.</p>
 */
class ResourcesTest {

    /**
     * Incantesimi ed effetti sono risolti dal registro popolato dal server:
     * in una JVM di test risultano sempre sconosciuti. Le relative segnalazioni
     * non descrivono quindi un errore di configurazione e vengono escluse,
     * mentre la loro forma testuale resta verificata separatamente.
     */
    private static boolean isRegistryWarning(String warning) {
        return warning.contains("incantesimo sconosciuto")
                || warning.contains("effetto non valido")
                || warning.contains("effetto sconosciuto")
                // Conseguenza diretta: senza registro nessun effetto risolve,
                // quindi la voce risulta priva di effetti utili.
                || warning.contains("nessun effetto utile");
    }

    private YamlConfiguration load(String resource) throws Exception {
        InputStream stream = getClass().getResourceAsStream("/" + resource);
        assertNotNull(stream, "risorsa mancante: " + resource);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(
                    new String(buffer.toByteArray(), Charset.forName("UTF-8")));
            return configuration;
        } finally {
            closeQuietly(stream);
        }
    }

    private void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // Chiusura best effort in ambito di test.
        }
    }

    @Test
    void tuttiIFileYamlSonoValidi() throws Exception {
        assertFalse(load("config.yml").getKeys(true).isEmpty());
        assertFalse(load("chickens.yml").getKeys(true).isEmpty());
        assertFalse(load("shop.yml").getKeys(true).isEmpty());
        assertFalse(load("plugin.yml").getKeys(true).isEmpty());
    }

    @Test
    void leDueLingueEspongonoLeStesseChiavi() throws Exception {
        Set<String> italian = new TreeSet<String>(load("messages_it.yml").getKeys(true));
        Set<String> english = new TreeSet<String>(load("messages_en.yml").getKeys(true));

        Set<String> onlyItalian = new TreeSet<String>(italian);
        onlyItalian.removeAll(english);
        Set<String> onlyEnglish = new TreeSet<String>(english);
        onlyEnglish.removeAll(italian);

        assertTrue(onlyItalian.isEmpty(), "chiavi assenti in inglese: " + onlyItalian);
        assertTrue(onlyEnglish.isEmpty(), "chiavi assenti in italiano: " + onlyEnglish);
    }

    @Test
    void ogniStrumentoDellEditorHaNomeEDescrizione() throws Exception {
        YamlConfiguration italian = load("messages_it.yml");
        YamlConfiguration english = load("messages_en.yml");

        for (SetupTool tool : SetupTool.values()) {
            assertNotNull(italian.getString(tool.getNameKey()),
                    "nome italiano mancante: " + tool.getNameKey());
            assertNotNull(english.getString(tool.getNameKey()),
                    "nome inglese mancante: " + tool.getNameKey());
            assertFalse(italian.getStringList(tool.getLoreKey()).isEmpty(),
                    "descrizione italiana mancante: " + tool.getLoreKey());
            assertFalse(english.getStringList(tool.getLoreKey()).isEmpty(),
                    "descrizione inglese mancante: " + tool.getLoreKey());
        }
    }

    @Test
    void gliSlotDegliStrumentiSonoUniciEDentroLaBarraRapida() {
        Set<Integer> slots = new TreeSet<Integer>();
        for (SetupTool tool : SetupTool.values()) {
            assertTrue(tool.getSlot() >= 0 && tool.getSlot() <= 8,
                    "slot fuori dalla barra rapida: " + tool);
            assertTrue(slots.add(tool.getSlot()), "slot duplicato: " + tool);
            assertNotNull(tool.getMaterial());
        }
    }

    @Test
    void ogniSezioneDellaGuidaHaContenutoInEntrambeLeLingue() throws Exception {
        YamlConfiguration italian = load("messages_it.yml");
        YamlConfiguration english = load("messages_en.yml");

        for (HelpTopic topic : HelpTopic.values()) {
            assertFalse(italian.getStringList(topic.getMessageKey()).isEmpty(),
                    "guida italiana mancante: " + topic.getMessageKey());
            assertFalse(english.getStringList(topic.getMessageKey()).isEmpty(),
                    "guida inglese mancante: " + topic.getMessageKey());
        }
    }

    @Test
    void gliAliasDelleSezioniSonoUnivoci() {
        Set<String> seen = new TreeSet<String>();
        for (HelpTopic topic : HelpTopic.values()) {
            assertFalse(topic.getAliases().isEmpty(),
                    "sezione senza alias: " + topic);
            for (String alias : topic.getAliases()) {
                assertTrue(seen.add(alias), "alias duplicato: " + alias);
                assertSame(topic, HelpTopic.find(alias));
            }
        }
    }

    @Test
    void laRicercaPerNomeNonInventaCorrispondenze() {
        // find() e' una ricerca pura: il ripiego sull'indice spetta a
        // HelpService.resolve, verificato in HelpServiceTest.
        assertNull(HelpTopic.find(""));
        assertNull(HelpTopic.find("  "));
        assertNull(HelpTopic.find(null));
        assertNull(HelpTopic.find("inesistente"));
        assertSame(HelpTopic.SETUP, HelpTopic.find("setup"));
    }

    @Test
    void iTemplateMondoSonoInterpretatiInModoTollerante() {
        assertSame(WorldTemplate.VOID, WorldTemplate.fromString("void"));
        assertSame(WorldTemplate.VOID, WorldTemplate.fromString(" VOID "));
        assertSame(WorldTemplate.FLAT, WorldTemplate.fromString("Flat"));
        assertSame(WorldTemplate.NORMAL, WorldTemplate.fromString("normal"));
        assertNull(WorldTemplate.fromString("superflat"));
        assertNull(WorldTemplate.fromString(null));
        assertTrue(WorldTemplate.VOID.needsCustomGenerator());
        assertFalse(WorldTemplate.FLAT.needsCustomGenerator());
    }

    @Test
    void soloIMondiCreatiDalPluginRicevonoUnGeneratore() {
        // EXISTING descrive un mondo adottato: non deve mai essere usato per
        // creare, ne' imporre una generazione a una mappa gia' esistente.
        assertFalse(WorldTemplate.EXISTING.isCreatable());
        assertFalse(WorldTemplate.EXISTING.needsCustomGenerator());

        assertTrue(WorldTemplate.VOID.isCreatable());
        assertTrue(WorldTemplate.FLAT.isCreatable());
        assertTrue(WorldTemplate.NORMAL.isCreatable());
    }

    /**
     * La sezione {@code equipment} spedita deve coprire ogni tier: un materiale
     * mancante lascerebbe il giocatore senza quel pezzo dopo il respawn.
     */
    @Test
    void configYmlDescriveTuttiITierDiEquipaggiamento() throws Exception {
        EquipmentSettings settings = EquipmentSettings.fromSection(
                load("config.yml").getConfigurationSection("equipment"));

        for (ArmorTier tier : ArmorTier.values()) {
            assertNotNull(settings.getLeggings(tier), "leggings " + tier);
            assertNotNull(settings.getBoots(tier), "stivali " + tier);
        }
        for (SwordTier tier : SwordTier.values()) {
            assertNotNull(settings.getSword(tier), "spada " + tier);
        }
        for (ToolTier tier : ToolTier.values()) {
            if (tier == ToolTier.NONE) {
                assertNull(settings.getPickaxe(tier));
                assertNull(settings.getAxe(tier));
                continue;
            }
            assertNotNull(settings.getPickaxe(tier), "piccone " + tier);
            assertNotNull(settings.getAxe(tier), "ascia " + tier);
        }
        assertNotNull(settings.getShears());
        assertNotNull(settings.getHelmet());
        assertNotNull(settings.getChestplate());
        assertSame(ToolTier.TIER_1, settings.getMinimumToolTier());
    }

    /**
     * Lo {@code upgrades.yml} incluso deve caricarsi senza anomalie
     * strutturali: livelli contigui, valute valide, profili completi.
     */
    @Test
    void loUpgradesInclusoSiCaricaSenzaAnomalie() throws Exception {
        UpgradeCatalog catalog = UpgradeConfigLoader.load(load("upgrades.yml"));

        java.util.List<String> structural = new java.util.ArrayList<String>();
        for (String warning : catalog.getWarnings()) {
            if (!isRegistryWarning(warning)) {
                structural.add(warning);
            }
        }

        assertTrue(structural.isEmpty(),
                "anomalie in upgrades.yml: " + structural);
        assertFalse(catalog.isEmpty());
        assertEquals(3, catalog.getMaximumTraps());
    }

    /**
     * Verifica la forma di effetti e valute in {@code upgrades.yml}, che il
     * registro Bukkit non permette di risolvere fuori dal server.
     */
    @Test
    void gliEffettiDegliUpgradeHannoUnaFormaValida() throws Exception {
        YamlConfiguration upgrades = load("upgrades.yml");

        ConfigurationSection teamUpgrades =
                upgrades.getConfigurationSection("team-upgrades");
        assertNotNull(teamUpgrades);
        for (String id : teamUpgrades.getKeys(false)) {
            String effect = teamUpgrades.getString(id + ".effect");
            if (effect != null) {
                assertTrue(effect.matches("[A-Z][A-Z_]*"),
                        "effetto malformato in " + id + ": " + effect);
            }
            assertNotNull(TeamUpgradeType.fromString(id),
                    "tipo upgrade sconosciuto: " + id);
        }

        ConfigurationSection traps =
                upgrades.getConfigurationSection("traps.types");
        assertNotNull(traps);
        for (String id : traps.getKeys(false)) {
            for (String path : new String[]{"intruder-effects",
                    "defender-effects"}) {
                for (String raw : traps.getStringList(id + "." + path)) {
                    assertTrue(raw.matches("[A-Z][A-Z_]*:\\d+:\\d+"),
                            "effetto malformato in " + id + ": " + raw);
                }
            }
        }
    }

    /**
     * Ogni upgrade e ogni trappola devono avere nome e descrizione tradotti.
     */
    @Test
    void ogniUpgradeEuLocalizzato() throws Exception {
        UpgradeCatalog catalog = UpgradeConfigLoader.load(load("upgrades.yml"));
        YamlConfiguration italian = load("messages_it.yml");
        YamlConfiguration english = load("messages_en.yml");

        for (TeamUpgradeType type : catalog.getTeamUpgrades().keySet()) {
            assertNotNull(italian.getString(type.getNameKey()), type.name());
            assertNotNull(english.getString(type.getNameKey()), type.name());
            assertFalse(italian.getStringList(type.getLoreKey()).isEmpty(),
                    type.name());
        }
        for (RoyalUpgradeType type : catalog.getRoyalUpgrades().keySet()) {
            assertNotNull(italian.getString(type.getNameKey()), type.name());
            assertNotNull(english.getString(type.getNameKey()), type.name());
        }
        for (TrapDefinition trap : catalog.getTraps().values()) {
            assertNotNull(italian.getString(trap.getNameKey()), trap.getId());
            assertNotNull(english.getString(trap.getNameKey()), trap.getId());
        }
    }

    /**
     * Ogni esito di acquisto deve avere un messaggio in entrambe le lingue.
     */
    @Test
    void ogniEsitoDiAcquistoEuLocalizzato() throws Exception {
        YamlConfiguration italian = load("messages_it.yml");
        YamlConfiguration english = load("messages_en.yml");

        for (UpgradeResult result : UpgradeResult.values()) {
            assertNotNull(italian.getString(result.getMessageKey()),
                    result.name());
            assertNotNull(english.getString(result.getMessageKey()),
                    result.name());
        }
    }

    @Test
    void chickensYmlProduceImpostazioniCoerenti() throws Exception {
        ChickenSettings settings = ChickenSettings.fromSection(
                load("chickens.yml").getConfigurationSection("default"));

        assertEquals(100.0D, settings.getHealth(), 0.0001D);
        assertEquals(25.0D, settings.getShield(), 0.0001D);
        assertTrue(settings.isHologramEnabled());
        assertFalse(settings.getHologramLines().isEmpty());
        assertTrue(settings.isFeedingEnabled());
        assertEquals(Material.SEEDS, settings.getFeedMaterial());
        assertTrue(settings.isLastFeatherEnabled());
    }

    /**
     * Lo {@code shop.yml} incluso deve caricarsi senza alcuna anomalia: nessun
     * materiale sconosciuto, nessuna valuta errata, nessun articolo privo di
     * prezzo in uno dei tre profili economici.
     */
    @Test
    void loShopInclusoSiCaricaSenzaAnomalie() throws Exception {
        ShopConfiguration configuration =
                ShopConfigLoader.load(load("shop.yml"));

        java.util.List<String> structural = new java.util.ArrayList<String>();
        for (String warning : configuration.getWarnings()) {
            if (!isRegistryWarning(warning)) {
                structural.add(warning);
            }
        }

        assertTrue(structural.isEmpty(), "anomalie in shop.yml: " + structural);
        assertFalse(configuration.isEmpty());
        assertFalse(configuration.getItems().isEmpty());

        for (String profileId : ShopConfigLoader.EXPECTED_PROFILES) {
            assertNotNull(configuration.getProfile(profileId),
                    "profilo mancante: " + profileId);
        }
    }

    /**
     * Verifica la forma di incantesimi ed effetti, che il registro Bukkit non
     * permette di risolvere fuori dal server.
     */
    @Test
    void incantesimiEdEffettiHannoUnaFormaValida() throws Exception {
        ConfigurationSection items =
                load("shop.yml").getConfigurationSection("items");
        assertNotNull(items);

        for (String itemId : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(itemId);
            assertNotNull(item, itemId);

            ConfigurationSection enchantments =
                    item.getConfigurationSection("enchantments");
            if (enchantments != null) {
                for (String key : enchantments.getKeys(false)) {
                    assertTrue(key.matches("[A-Z][A-Z_]*"),
                            "nome incantesimo malformato in " + itemId + ": " + key);
                    assertTrue(enchantments.getInt(key, 0) > 0,
                            "livello incantesimo non valido in " + itemId);
                }
            }

            for (String effect : item.getStringList("effects")) {
                assertTrue(effect.matches("[A-Z][A-Z_]*:\\d+:\\d+"),
                        "effetto malformato in " + itemId + ": " + effect);
            }
        }
    }

    /**
     * Ogni categoria dichiarata nel menu deve avere nome tradotto in entrambe
     * le lingue incluse, e ogni articolo nome e descrizione.
     */
    @Test
    void ogniVoceDelloShopEuLocalizzata() throws Exception {
        ShopConfiguration configuration =
                ShopConfigLoader.load(load("shop.yml"));
        YamlConfiguration italian = load("messages_it.yml");
        YamlConfiguration english = load("messages_en.yml");

        for (ShopCategoryDefinition category : configuration.getCategories()) {
            assertNotNull(italian.getString(category.getNameKey()),
                    "categoria senza nome italiano: " + category.getId());
            assertNotNull(english.getString(category.getNameKey()),
                    "categoria senza nome inglese: " + category.getId());
        }

        for (ShopItemDefinition item : configuration.getItems().values()) {
            assertNotNull(italian.getString(item.getNameKey()),
                    "articolo senza nome italiano: " + item.getId());
            assertNotNull(english.getString(item.getNameKey()),
                    "articolo senza nome inglese: " + item.getId());
            assertFalse(italian.getStringList(item.getLoreKey()).isEmpty(),
                    "articolo senza descrizione italiana: " + item.getId());
            assertFalse(english.getStringList(item.getLoreKey()).isEmpty(),
                    "articolo senza descrizione inglese: " + item.getId());
        }

        for (ResourceType currency : ResourceType.values()) {
            String key = "shop.currency."
                    + currency.name().toLowerCase(java.util.Locale.ROOT);
            assertNotNull(italian.getString(key), "valuta non tradotta: " + key);
            assertNotNull(english.getString(key), "valuta non tradotta: " + key);
        }
    }
}
