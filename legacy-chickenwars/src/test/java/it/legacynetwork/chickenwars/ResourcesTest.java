package it.legacynetwork.chickenwars;

import it.legacynetwork.chickenwars.chicken.ChickenSettings;
import it.legacynetwork.chickenwars.command.HelpTopic;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.setup.SetupTool;
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

    @Test
    void ogniArticoloDelloShopHaMaterialeEValutaValidi() throws Exception {
        ConfigurationSection categories =
                load("shop.yml").getConfigurationSection("categories");
        assertNotNull(categories);

        int checked = 0;
        for (String categoryId : categories.getKeys(false)) {
            ConfigurationSection category =
                    categories.getConfigurationSection(categoryId);
            assertNotNull(category, categoryId);
            assertNotNull(Material.matchMaterial(category.getString("icon", "")),
                    "icona non valida in " + categoryId);

            ConfigurationSection items = category.getConfigurationSection("items");
            assertNotNull(items, "categoria senza articoli: " + categoryId);
            for (String itemId : items.getKeys(false)) {
                ConfigurationSection item = items.getConfigurationSection(itemId);
                assertNotNull(item, itemId);
                assertNotNull(Material.matchMaterial(item.getString("material", "")),
                        "materiale non valido in " + itemId);
                assertNotNull(ResourceType.fromString(item.getString("currency")),
                        "valuta non valida in " + itemId);
                assertTrue(item.getInt("price", 0) > 0,
                        "prezzo non valido in " + itemId);
                checked++;
            }
        }
        assertTrue(checked > 0);
    }
}
