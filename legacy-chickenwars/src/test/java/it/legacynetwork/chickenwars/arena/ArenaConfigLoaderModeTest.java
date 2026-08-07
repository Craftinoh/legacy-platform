package it.legacynetwork.chickenwars.arena;

import it.legacynetwork.chickenwars.mode.MatchMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaConfigLoaderModeTest {

    @TempDir
    Path tempDirectory;

    @Test
    void readsExplicitModeAndSavesItBack() throws Exception {
        File file = tempDirectory.resolve("arena.yml").toFile();
        YamlConfiguration source = new YamlConfiguration();
        source.set("arena.id", "test");
        source.set("arena.mode", "doubles");
        source.set("arena.players-per-team", 2);
        source.save(file);

        ArenaDefinition loaded = ArenaConfigLoader.load(file,
                Logger.getLogger("ArenaConfigLoaderModeTest"));
        assertNotNull(loaded);
        assertEquals(MatchMode.DOUBLES, loaded.getMode());

        loaded.setMode(MatchMode.TRIO);
        assertTrue(ArenaConfigLoader.save(file, loaded,
                Logger.getLogger("ArenaConfigLoaderModeTest")));

        YamlConfiguration saved = YamlConfiguration.loadConfiguration(file);
        assertEquals("trio", saved.getString("arena.mode"));
    }

    @Test
    void legacyArenaWithoutModeIsInferred() throws Exception {
        File file = tempDirectory.resolve("legacy.yml").toFile();
        YamlConfiguration source = new YamlConfiguration();
        source.set("arena.id", "legacy");
        source.set("arena.players-per-team", 2);
        source.save(file);

        ArenaDefinition loaded = ArenaConfigLoader.load(file,
                Logger.getLogger("ArenaConfigLoaderModeTest"));
        assertNotNull(loaded);
        assertEquals(MatchMode.DOUBLES, loaded.getMode());
    }
}
