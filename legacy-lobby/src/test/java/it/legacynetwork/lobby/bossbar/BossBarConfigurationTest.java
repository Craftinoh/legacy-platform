package it.legacynetwork.lobby.bossbar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossBarConfigurationTest {

    @Test
    void loadsBarsFromConfig(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("bossbar.yml").toFile();
        String yaml = "enabled: true\n" +
                "update:\n" +
                "  ticks: 5\n" +
                "rotation:\n" +
                "  enabled: true\n" +
                "  mode: SEQUENTIAL\n" +
                "  interval-ticks: 100\n" +
                "bars:\n" +
                "  welcome:\n" +
                "    enabled: true\n" +
                "    priority: 10\n" +
                "    display-ticks: 100\n" +
                "    languages:\n" +
                "      it:\n" +
                "        text: \"&6Benvenuto\"\n" +
                "      en:\n" +
                "        text: \"&6Welcome\"\n" +
                "    progress:\n" +
                "      type: STATIC\n" +
                "      value: 1.0\n" +
                "      fallback: 1.0\n";
        Files.write(file.toPath(), yaml.getBytes());
        BossBarConfiguration config = BossBarConfiguration.load(file);
        assertTrue(config.isEnabled());
        assertEquals(5, config.getUpdateTicks());
        assertEquals(BossBarRotationMode.SEQUENTIAL, config.getRotationMode());
        assertEquals(1, config.getBars().size());
        assertNotNull(config.getBar("welcome"));
        assertEquals("welcome", config.getBar("welcome").getId());
        assertEquals(10, config.getBar("welcome").getPriority());
    }

    @Test
    void multipleBarsSortedByPriority(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("bossbar.yml").toFile();
        String yaml = "bars:\n" +
                "  bar1:\n" +
                "    priority: 30\n" +
                "    display-ticks: 100\n" +
                "    progress:\n" +
                "      type: STATIC\n" +
                "      value: 1.0\n" +
                "      fallback: 1.0\n" +
                "  bar2:\n" +
                "    priority: 10\n" +
                "    display-ticks: 100\n" +
                "    progress:\n" +
                "      type: STATIC\n" +
                "      value: 1.0\n" +
                "      fallback: 1.0\n" +
                "  bar3:\n" +
                "    priority: 20\n" +
                "    display-ticks: 100\n" +
                "    progress:\n" +
                "      type: STATIC\n" +
                "      value: 1.0\n" +
                "      fallback: 1.0\n";
        Files.write(file.toPath(), yaml.getBytes());
        BossBarConfiguration config = BossBarConfiguration.load(file);
        assertEquals(3, config.getBars().size());
    }

    @Test
    void disabledBossbar(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("bossbar.yml").toFile();
        String yaml = "enabled: false\n"
                + "bars:\n"
                + "  welcome:\n"
                + "    display-ticks: 100\n"
                + "    progress:\n"
                + "      type: STATIC\n"
                + "      value: 1.0\n"
                + "      fallback: 1.0\n";
        Files.write(file.toPath(), yaml.getBytes());
        BossBarConfiguration config = BossBarConfiguration.load(file);
        assertFalse(config.isEnabled());
    }

    @Test
    void randomRotationMode(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("bossbar.yml").toFile();
        String yaml = "rotation:\n" +
                "  mode: RANDOM\n" +
                "  interval-ticks: 50\n";
        Files.write(file.toPath(), yaml.getBytes());
        BossBarConfiguration config = BossBarConfiguration.load(file);
        assertEquals(BossBarRotationMode.RANDOM, config.getRotationMode());
        assertEquals(50, config.getRotationIntervalTicks());
    }
}
