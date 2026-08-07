package it.legacynetwork.lobby;

import it.legacynetwork.language.TranslationInstaller;
import it.legacynetwork.lobby.bossbar.BossBarConfiguration;
import it.legacynetwork.lobby.bossbar.BossBarDefinition;
import it.legacynetwork.lobby.config.ScoreboardConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyVisualLocalizationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void bundledScoreboardContainsAllSupportedLanguages() throws Exception {
        Path file = copyResource("scoreboard.yml");
        ScoreboardConfiguration configuration =
                ScoreboardConfiguration.load(file.toFile());

        assertEquals(31, configuration.getLanguages().size());
        for (String language : TranslationInstaller.ALL_LANGUAGES) {
            assertTrue(configuration.hasLanguage(language),
                    "Missing scoreboard language: " + language);
            assertNotNull(configuration.getLanguage(language));
        }
        assertNotEquals(
                configuration.getLanguage("en").getLines(),
                configuration.getLanguage("fr").getLines());
    }

    @Test
    void scoreboardOverlayPreservesAdministratorValues() throws Exception {
        Path file = temporaryDirectory.resolve("custom-scoreboard.yml");
        Files.write(file, (
                "enabled: true\n"
                        + "languages:\n"
                        + "  en:\n"
                        + "    title: '&cCUSTOM'\n"
                        + "    lines: ['&fCustom line']\n")
                .getBytes(StandardCharsets.UTF_8));

        ScoreboardConfiguration configuration;
        try (InputStream defaults = resource("scoreboard.yml")) {
            configuration = ScoreboardConfiguration.load(
                    file.toFile(), defaults);
        }

        assertEquals(31, configuration.getLanguages().size());
        assertEquals("&cCUSTOM", configuration.getLanguage("en").getTitle());
        assertEquals("&fCustom line",
                configuration.getLanguage("en").getLines().get(0));
        assertTrue(configuration.hasLanguage("fr"));
    }

    @Test
    void bundledBossBarsContainAllSupportedLanguages() throws Exception {
        Path file = copyResource("bossbar.yml");
        BossBarConfiguration configuration =
                BossBarConfiguration.load(file.toFile());

        assertEquals(4, configuration.getBars().size());
        for (BossBarDefinition bar : configuration.getBars().values()) {
            assertEquals(31, bar.getLanguageTexts().size(), bar.getId());
            for (String language : TranslationInstaller.ALL_LANGUAGES) {
                assertTrue(bar.hasLanguage(language),
                        bar.getId() + " missing " + language);
            }
            assertNotEquals(bar.getText("en"), bar.getText("fr"));
        }
    }

    @Test
    void bossBarOverlayPreservesAdministratorText() throws Exception {
        Path file = temporaryDirectory.resolve("custom-bossbar.yml");
        Files.write(file, (
                "enabled: true\n"
                        + "bars:\n"
                        + "  welcome:\n"
                        + "    enabled: true\n"
                        + "    priority: 1\n"
                        + "    display-ticks: 100\n"
                        + "    languages:\n"
                        + "      en:\n"
                        + "        text: '&cCUSTOM BOSSBAR'\n"
                        + "    progress:\n"
                        + "      type: STATIC\n"
                        + "      value: 1.0\n")
                .getBytes(StandardCharsets.UTF_8));

        BossBarConfiguration configuration;
        try (InputStream defaults = resource("bossbar.yml")) {
            configuration = BossBarConfiguration.load(file.toFile(), defaults);
        }

        BossBarDefinition welcome = configuration.getBar("welcome");
        assertNotNull(welcome);
        assertEquals(31, welcome.getLanguageTexts().size());
        assertEquals("&cCUSTOM BOSSBAR", welcome.getText("en"));
        assertTrue(welcome.hasLanguage("fr"));
        assertNotNull(configuration.getBar("network"));
    }

    private Path copyResource(String name) throws Exception {
        Path target = temporaryDirectory.resolve(name);
        try (InputStream input = resource(name)) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private InputStream resource(String name) {
        InputStream input = getClass().getClassLoader()
                .getResourceAsStream(name);
        assertNotNull(input, name);
        return input;
    }
}
