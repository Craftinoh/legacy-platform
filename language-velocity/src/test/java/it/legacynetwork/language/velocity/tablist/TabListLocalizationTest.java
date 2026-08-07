package it.legacynetwork.language.velocity.tablist;

import it.legacynetwork.language.TranslationInstaller;
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

class TabListLocalizationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void bundledTabListContainsAllSupportedLanguages() throws Exception {
        Path file = temporaryDirectory.resolve("tablist.yml");
        try (InputStream input = resource()) {
            Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
        }

        TabListConfiguration configuration =
                TabListConfigurationLoader.load(file.toFile());

        assertEquals(31, configuration.getLanguages().size());
        for (String language : TranslationInstaller.ALL_LANGUAGES) {
            assertNotNull(configuration.getLanguage(language),
                    "Missing tab language: " + language);
            assertTrue(configuration.getLanguages().containsKey(language));
        }
        assertNotEquals(
                configuration.getLanguage("en").getHeader(),
                configuration.getLanguage("fr").getHeader());
    }

    @Test
    void overlayAddsMissingLanguagesAndPreservesCustomEnglish() throws Exception {
        Path file = temporaryDirectory.resolve("custom-tablist.yml");
        Files.write(file, (
                "enabled: true\n"
                        + "fallback:\n"
                        + "  language: en\n"
                        + "  server-name: proxy\n"
                        + "languages:\n"
                        + "  en:\n"
                        + "    header: ['&cCUSTOM HEADER']\n"
                        + "    footer: ['&cCUSTOM FOOTER']\n")
                .getBytes(StandardCharsets.UTF_8));

        TabListConfiguration configuration =
                TabListConfigurationLoader.load(file.toFile(), resource());

        assertEquals(31, configuration.getLanguages().size());
        assertEquals("&cCUSTOM HEADER",
                configuration.getLanguage("en").getHeader().get(0));
        assertEquals("&cCUSTOM FOOTER",
                configuration.getLanguage("en").getFooter().get(0));
        assertTrue(configuration.getLanguages().containsKey("fr"));
        assertTrue(configuration.getLanguages().containsKey("no"));
    }

    private InputStream resource() {
        InputStream input = getClass().getClassLoader()
                .getResourceAsStream("tablist.yml");
        assertNotNull(input);
        return input;
    }
}
