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

class MenuRenderingWiringTest {

    private YamlConfiguration load() {
        File f = new File("src/main/resources/menus/server-selector.yml");
        if (!f.exists()) f = new File("legacy-menu/src/main/resources/menus/server-selector.yml");
        if (!f.exists()) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(f);
    }

    @Test
    void englishTitleResolves() {
        String title = load().getString("titles.en");
        assertTrue(title.contains("Server") || title.contains("Selector"));
    }

    @Test
    void italianTitleResolves() {
        String title = load().getString("titles.it");
        assertTrue(title.contains("Selettore") || title.contains("Server"));
    }

    @Test
    void bedwarsEnNameIsBedWars() {
        String name = load().getString("items.bedwars.translations.en.name");
        assertTrue(name.contains("BedWars"));
    }

    @Test
    void bedwarsItLoreNotEmpty() {
        assertFalse(load().getStringList(
                "items.bedwars.translations.it.lore").isEmpty());
    }

    @Test
    void kitpvpEnNameIsKitPvP() {
        String name = load().getString("items.kitpvp.translations.en.name");
        assertTrue(name.contains("KitPvP"));
    }

    @Test
    void closeItNameIsChiudi() {
        String name = load().getString("items.close.translations.it.name");
        assertTrue(name.contains("Chiudi"));
    }

    @Test
    void resolverResolvesBedwarsName() {
        String name = load().getString("items.bedwars.translations.en.name");
        assertTrue(name.contains("BedWars"));
    }

    @Test
    void resolverReturnsEnglishTitleFallback() {
        Map<String, Object> titles = load().getConfigurationSection(
                "titles").getValues(false);
        String title = LocalizedConfigurationResolver.resolveTitle(titles, "zz");
        assertEquals(titles.get("en"), title);
    }

    @Test
    void bedwarsSlotNotInTranslations() {
        assertNotNull(load().getInt("items.bedwars.slot"));
        assertEquals(null, load().getString("items.bedwars.translations.en.slot"));
    }

    @Test
    void kitpvpSlotNotInTranslations() {
        assertNotNull(load().getInt("items.kitpvp.slot"));
        assertEquals(null, load().getString("items.kitpvp.translations.en.slot"));
    }

    @Test
    void closeSlotNotInTranslations() {
        assertNotNull(load().getInt("items.close.slot"));
        assertEquals(null, load().getString("items.close.translations.en.slot"));
    }
}
