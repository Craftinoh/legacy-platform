package it.legacynetwork.lobby.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreboardConfigurationTest {

    @Test
    void loadsDefaultConfiguration(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("scoreboard.yml").toFile();
        String yaml = "enabled: true\n" +
                "update:\n" +
                "  ticks: 20\n" +
                "placeholderapi:\n" +
                "  enabled: true\n" +
                "languages:\n" +
                "  it:\n" +
                "    title: \"&6&lIT\"\n" +
                "    lines:\n" +
                "      - \"&fLinea 1\"\n" +
                "      - \"&fLinea 2\"\n" +
                "  en:\n" +
                "    title: \"&6&lEN\"\n" +
                "    lines:\n" +
                "      - \"&fLine 1\"\n" +
                "      - \"&fLine 2\"\n";
        Files.write(file.toPath(), yaml.getBytes());
        ScoreboardConfiguration config = ScoreboardConfiguration.load(file);
        assertTrue(config.isEnabled());
        assertEquals(20, config.getUpdateTicks());
        assertTrue(config.isPlaceholderApiEnabled());
        assertNotNull(config.getLanguage("it"));
        assertNotNull(config.getLanguage("en"));
        assertEquals("&6&lIT", config.getLanguage("it").getTitle());
        assertEquals(2, config.getLanguage("it").getLines().size());
    }

    @Test
    void fallbackToEnglishWhenItalianMissing(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("scoreboard.yml").toFile();
        String yaml = "enabled: true\n" +
                "update:\n" +
                "  ticks: 10\n" +
                "languages:\n" +
                "  en:\n" +
                "    title: \"&6EN\"\n" +
                "    lines:\n" +
                "      - \"&fOnly English\"\n";
        Files.write(file.toPath(), yaml.getBytes());
        ScoreboardConfiguration config = ScoreboardConfiguration.load(file);
        assertNotNull(config.getLanguage("it"));
        assertEquals("&6EN",
                config.getLanguage("it").getTitle());
    }

    @Test
    void hasDefaultEnglishFallback(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("scoreboard.yml").toFile();
        String yaml = "enabled: true\n";
        Files.write(file.toPath(), yaml.getBytes());
        ScoreboardConfiguration config = ScoreboardConfiguration.load(file);
        assertNotNull(config.getLanguage("en"));
        assertFalse(config.getLanguage("en").getLines().isEmpty());
    }

    @Test
    void disablesCorrectly(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("scoreboard.yml").toFile();
        String yaml = "enabled: false\n";
        Files.write(file.toPath(), yaml.getBytes());
        ScoreboardConfiguration config = ScoreboardConfiguration.load(file);
        assertFalse(config.isEnabled());
    }
}
