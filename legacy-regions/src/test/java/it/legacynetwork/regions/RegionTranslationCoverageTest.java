package it.legacynetwork.regions;

import it.legacynetwork.language.TranslationInstaller;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RegionTranslationCoverageTest {

    private static final List<String> REQUIRED_KEYS = Arrays.asList(
            "generic", "build", "block-break", "block-place", "pvp",
            "damage", "fall-damage", "hunger", "item-drop", "item-pickup",
            "explosions", "fire-spread", "mob-spawn", "projectiles",
            "vehicle-use", "interact");

    @Test
    void all31RegionCatalogsContainEveryDeniedMessage() throws Exception {
        assertEquals(31, TranslationInstaller.ALL_LANGUAGES.size());
        for (String language : TranslationInstaller.ALL_LANGUAGES) {
            String path = "translations/messages_" + language + ".yml";
            try (InputStream input = getClass().getClassLoader()
                    .getResourceAsStream(path)) {
                assertNotNull(input, path);
                YamlConfiguration configuration =
                        YamlConfiguration.loadConfiguration(
                                new InputStreamReader(input,
                                        StandardCharsets.UTF_8));
                for (String key : REQUIRED_KEYS) {
                    String value = configuration.getString("denied." + key);
                    assertNotNull(value, path + " denied." + key);
                    assertFalse(value.trim().isEmpty(),
                            path + " denied." + key);
                }
            }
        }
    }

    @Test
    void frenchAndEnglishCatalogsAreActuallyDifferent() throws Exception {
        YamlConfiguration english = load("en");
        YamlConfiguration french = load("fr");
        assertNotEquals(english.getString("denied.build"),
                french.getString("denied.build"));
        assertNotEquals(english.getString("denied.interact"),
                french.getString("denied.interact"));
    }

    private YamlConfiguration load(String language) throws Exception {
        String path = "translations/messages_" + language + ".yml";
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(path)) {
            assertNotNull(input, path);
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
        }
    }
}
