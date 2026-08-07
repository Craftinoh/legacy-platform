package it.legacynetwork.menu.lang;

import it.legacynetwork.language.Language;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageMenuAssetsTest {

    @Test
    void everySupportedLanguageHasAValidMojangTexture()
            throws Exception {
        YamlConfiguration configuration = load("flag-textures.yml");

        for (Language language : Language.values()) {
            String path = "languages." + language.getCode();
            String country = configuration.getString(path + ".country");
            String texture = configuration.getString(path + ".texture");

            assertNotNull(country,
                    "Missing country for " + language.getCode());
            assertFalse(country.trim().isEmpty(),
                    "Blank country for " + language.getCode());
            assertTrue(SkullTextureUtil.isValidTexture(texture),
                    "Invalid texture for " + language.getCode());
        }
    }

    @Test
    void rejectsMalformedOrUntrustedTextures() {
        assertFalse(SkullTextureUtil.isValidTexture(""));
        assertFalse(SkullTextureUtil.isValidTexture("not-base64"));
        assertFalse(SkullTextureUtil.isValidTexture(
                java.util.Base64.getEncoder().encodeToString(
                        "{\"textures\":{\"SKIN\":{\"url\":\"https://example.com/texture/aabbccdd\"}}}"
                                .getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void bundledMenuHasCompleteEnglishItalianAndFrenchCopy()
            throws Exception {
        YamlConfiguration configuration = load("language-menu.yml");
        List<String> languages = Arrays.asList("en", "it", "fr");
        List<String> strings = Arrays.asList(
                "menu.title",
                "menu.language-name",
                "menu.language-name-selected",
                "menu.status.selected",
                "menu.status.available",
                "menu.page",
                "menu.close",
                "menu.back",
                "menu.next",
                "menu.prev",
                "change.success",
                "change.already-selected",
                "change.pending",
                "change.cooldown",
                "change.rate-limited",
                "change.unavailable",
                "change.timeout",
                "change.error");
        List<String> lists = Arrays.asList(
                "menu.lore.selected",
                "menu.lore.unselected",
                "menu.page-lore");

        for (String language : languages) {
            String root = "languages." + language + ".";
            for (String key : strings) {
                String value = configuration.getString(root + key);
                assertNotNull(value, language + " missing " + key);
                assertFalse(value.trim().isEmpty(),
                        language + " blank " + key);
            }
            for (String key : lists) {
                assertFalse(configuration.getStringList(root + key).isEmpty(),
                        language + " missing list " + key);
            }
        }
    }

    private YamlConfiguration load(String resource) throws Exception {
        InputStream input = getClass().getClassLoader()
                .getResourceAsStream(resource);
        assertNotNull(input, "Missing resource " + resource);
        try (InputStreamReader reader = new InputStreamReader(
                input, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }
}
