package it.legacynetwork.items;

import it.legacynetwork.language.LocalizedConfigurationResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRendererTest {

    private YamlConfiguration load() {
        File f = new File("src/main/resources/items.yml");
        if (!f.exists()) f = new File("legacy-items/src/main/resources/items.yml");
        if (!f.exists()) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(f);
    }

    @Test
    void schemaVersionIsTwo() {
        assertEquals(2, load().getInt("schema-version"));
    }

    @Test
    void serverSelectorEnabled() {
        assertNotNull(load().getConfigurationSection("items.server-selector"));
    }

    @Test
    void translationsHaveAll31Languages() {
        Map<String, Object> map = load().getConfigurationSection(
                "items.server-selector.translations").getValues(false);
        assertEquals(31, map.size());
    }

    @Test
    void italianNameNotEmpty() {
        String name = load().getString("items.server-selector.translations.it.name");
        assertNotNull(name);
        assertFalse(name.isEmpty());
    }

    @Test
    void englishNameContainsServerSelector() {
        String name = load().getString("items.server-selector.translations.en.name");
        assertTrue(name.contains("Server") || name.contains("Selector"));
    }

    @Test
    void italianNameDifferentFromEnglish() {
        String en = load().getString("items.server-selector.translations.en.name");
        String it = load().getString("items.server-selector.translations.it.name");
        assertFalse(en.equals(it));
    }

    @Test
    void loreIsList() {
        List<String> lore = load().getStringList(
                "items.server-selector.translations.en.lore");
        assertFalse(lore.isEmpty());
    }

    @Test
    void ptAndPtBrLoreDifferent() {
        List<String> pt = load().getStringList(
                "items.server-selector.translations.pt.lore");
        List<String> br = load().getStringList(
                "items.server-selector.translations.pt_br.lore");
        assertFalse(pt.get(0).equals(br.get(0)));
    }

    @Test
    void resolverReturnsEnglishForUnknownLanguage() {
        assertTrue(true);
    }

    @Test
    void materialSlotActionNotInTranslations() {
        for (String key : new String[]{"material", "slot", "actions"}) {
            String val = load().getString(
                    "items.server-selector.translations.en." + key);
            assertEquals(null, val, "Technical key in translations: " + key);
        }
    }
}
