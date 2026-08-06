package it.legacynetwork.menu;

import it.legacynetwork.language.LocalizedConfigurationResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuRendererTest {

    private YamlConfiguration load() {
        File f = new File("src/main/resources/menus/server-selector.yml");
        if (!f.exists()) f = new File("legacy-menu/src/main/resources/menus/server-selector.yml");
        if (!f.exists()) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(f);
    }

    @Test
    void schemaVersionIsTwo() {
        assertEquals(2, load().getInt("schema-version"));
    }

    @Test
    void all31TitlesPresent() {
        Map<String, Object> titles = load().getConfigurationSection("titles")
                .getValues(false);
        assertEquals(31, titles.size());
    }

    @Test
    void bedwarsItemHasTranslations() {
        assertNotNull(load().getConfigurationSection(
                "items.bedwars.translations.en"));
    }

    @Test
    void kitpvpItemHasTranslations() {
        assertNotNull(load().getConfigurationSection(
                "items.kitpvp.translations.en"));
    }

    @Test
    void closeItemHasTranslations() {
        assertNotNull(load().getConfigurationSection(
                "items.close.translations.en"));
    }

    @Test
    void bedwarsHas31Languages() {
        int count = load().getConfigurationSection(
                "items.bedwars.translations").getValues(false).size();
        assertEquals(31, count);
    }

    @Test
    void kitpvpHas31Languages() {
        int count = load().getConfigurationSection(
                "items.kitpvp.translations").getValues(false).size();
        assertEquals(31, count);
    }

    @Test
    void closeHas31Languages() {
        int count = load().getConfigurationSection(
                "items.close.translations").getValues(false).size();
        assertEquals(31, count);
    }

    @Test
    void italianTitleDifferentFromEnglish() {
        String en = load().getString("titles.en");
        String it = load().getString("titles.it");
        assertFalse(en.equals(it));
    }

    @Test
    void resolverReturnsTitleForEnglish() {
        Map<String, Object> titles = load().getConfigurationSection("titles")
                .getValues(false);
        String title = LocalizedConfigurationResolver.resolveTitle(titles, "en");
        assertNotNull(title);
        assertFalse(title.isEmpty());
    }

    @Test
    void resolverReturnsTitleFallbackForUnknown() {
        Map<String, Object> titles = load().getConfigurationSection("titles")
                .getValues(false);
        String title = LocalizedConfigurationResolver.resolveTitle(titles, "zz");
        assertEquals(titles.get("en"), title);
    }
}
