package it.legacynetwork.items.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyItemsConfigurationTest {

    @Test
    void defaultValues() {
        YamlConfiguration config = new YamlConfiguration();
        LegacyItemsConfiguration itemsConfig = LegacyItemsConfiguration.from(config);
        assertEquals("lobby-01", itemsConfig.getServerId());
        assertEquals("en", itemsConfig.getLanguageFallback());
        assertEquals(2, itemsConfig.getGiveDelayTicks());
        assertTrue(itemsConfig.isEnabled());
        assertTrue(itemsConfig.isOverwriteExistingSlot());
    }

    @Test
    void customServerId() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("server.id", "bedwars-01");
        LegacyItemsConfiguration itemsConfig = LegacyItemsConfiguration.from(config);
        assertEquals("bedwars-01", itemsConfig.getServerId());
    }

    @Test
    void debugEnabled() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("debug.enabled", true);
        LegacyItemsConfiguration itemsConfig = LegacyItemsConfiguration.from(config);
        assertTrue(itemsConfig.isDebug());
    }
}
