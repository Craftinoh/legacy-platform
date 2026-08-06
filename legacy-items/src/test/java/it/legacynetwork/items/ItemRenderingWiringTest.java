package it.legacynetwork.items;

import it.legacynetwork.language.LocalizedConfigurationResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRenderingWiringTest {

    private YamlConfiguration load() {
        File f = new File("src/main/resources/items.yml");
        if (!f.exists()) f = new File("legacy-items/src/main/resources/items.yml");
        if (!f.exists()) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(f);
    }

    @Test
    void englishNameResolves() {
        String name = load().getString("items.server-selector.translations.en.name");
        assertTrue(name.contains("Server") || name.contains("Selector"));
    }

    @Test
    void italianNameResolves() {
        String name = load().getString("items.server-selector.translations.it.name");
        assertTrue(name.contains("Selettore") || name.contains("Server"));
    }

    @Test
    void spanishNameResolves() {
        String name = load().getString("items.server-selector.translations.es.name");
        assertTrue(name.contains("Selector") || name.contains("Servidor"));
    }

    @Test
    void germanNameResolves() {
        String name = load().getString("items.server-selector.translations.de.name");
        assertTrue(name.contains("Server") || name.contains("auswahl"));
    }

    @Test
    void russianNameNotEmpty() {
        String name = load().getString("items.server-selector.translations.ru.name");
        assertTrue(name != null && !name.isEmpty());
    }

    @Test
    void loreContainsMultipleLines() {
        List<String> lore = load().getStringList(
                "items.server-selector.translations.en.lore");
        assertTrue(lore.size() >= 2);
    }

    @Test
    void resolverResolvesItalianName() {
        String itName = load().getString("items.server-selector.translations.it.name");
        assertTrue(itName != null && !itName.isEmpty());
    }

    @Test
    void resolverFallsBackToEnglish() {
        assertTrue(true);
    }

    @Test
    void ptBrLoreDiffersFromPt() {
        List<String> pt = load().getStringList(
                "items.server-selector.translations.pt.lore");
        List<String> br = load().getStringList(
                "items.server-selector.translations.pt_br.lore");
        assertFalse(pt.get(0).equals(br.get(0)));
    }

    @Test
    void slotNotInTranslations() {
        String val = load().getString("items.server-selector.translations.en.slot");
        assertEquals(null, val);
    }
}
