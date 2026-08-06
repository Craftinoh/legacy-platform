package it.legacynetwork.regions.config;

import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionFlag;
import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionConfigLoaderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void saveAndLoadRoundTripPreservesRegion() throws Exception {
        File file = temporaryDirectory.resolve("regions.yml").toFile();
        Map<RegionFlag, FlagState> flags =
                new HashMap<RegionFlag, FlagState>();
        flags.put(RegionFlag.BUILD, FlagState.DENY);
        flags.put(RegionFlag.INTERACT, FlagState.ALLOW);

        List<CuboidRegion> regions = new ArrayList<CuboidRegion>();
        regions.add(new CuboidRegion(
                "Lobby_Main", "world", "",
                20, 80, 30, -10, 0, -40,
                100, flags));

        RegionConfigLoader.saveRegionsStrict(file, regions);
        List<CuboidRegion> loaded =
                RegionConfigLoader.loadRegionsStrict(file);

        assertEquals(1, loaded.size());
        CuboidRegion region = loaded.get(0);
        assertEquals("lobby_main", region.getId());
        assertEquals("world", region.getWorldName());
        assertEquals(-10, region.getMinX());
        assertEquals(-40, region.getMinZ());
        assertEquals(20, region.getMaxX());
        assertEquals(30, region.getMaxZ());
        assertEquals(100, region.getPriority());
        assertEquals(FlagState.DENY, region.getFlag(RegionFlag.BUILD));
        assertEquals(FlagState.ALLOW, region.getFlag(RegionFlag.INTERACT));
    }

    @Test
    void invalidFlagRejectsWholeFile() throws Exception {
        File file = write("regions:\n"
                + "  lobby:\n"
                + "    world:\n"
                + "      name: world\n"
                + "      uuid: ''\n"
                + "    minimum: {x: 0, y: 0, z: 0}\n"
                + "    maximum: {x: 10, y: 10, z: 10}\n"
                + "    priority: 1\n"
                + "    flags:\n"
                + "      unknown-flag: DENY\n");

        assertThrows(InvalidConfigurationException.class,
                () -> RegionConfigLoader.loadRegionsStrict(file));
    }

    @Test
    void missingCoordinateRejectsWholeFile() throws Exception {
        File file = write("regions:\n"
                + "  lobby:\n"
                + "    world:\n"
                + "      name: world\n"
                + "      uuid: ''\n"
                + "    minimum: {x: 0, y: 0}\n"
                + "    maximum: {x: 10, y: 10, z: 10}\n"
                + "    priority: 1\n"
                + "    flags: {}\n");

        assertThrows(InvalidConfigurationException.class,
                () -> RegionConfigLoader.loadRegionsStrict(file));
    }

    @Test
    void failedPublicLoadReturnsNullInsteadOfEmptySnapshot() throws Exception {
        File file = write("regions: [not-a-section]\n");
        assertEquals(null, RegionConfigLoader.loadRegions(file));
    }

    @Test
    void atomicSaveLeavesNoTemporaryFile() throws Exception {
        File file = temporaryDirectory.resolve("regions.yml").toFile();
        List<CuboidRegion> regions = new ArrayList<CuboidRegion>();
        regions.add(new CuboidRegion(
                "lobby", "world", "",
                0, 0, 0, 10, 10, 10,
                0, new HashMap<RegionFlag, FlagState>()));

        RegionConfigLoader.saveRegionsStrict(file, regions);

        assertTrue(file.isFile());
        assertTrue(!new File(file.getParentFile(), "regions.yml.tmp").exists());
    }

    private File write(String content) throws Exception {
        Path file = temporaryDirectory.resolve("regions-"
                + System.nanoTime() + ".yml");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }
}
