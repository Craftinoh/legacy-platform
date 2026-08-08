package it.legacynetwork.chickenwars.message;

import it.legacynetwork.language.TranslationInstaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationResourcesTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void packagesAndInstallsEverySupportedLanguage() throws Exception {
        int installed = TranslationInstaller.install(
                temporaryDirectory.toFile(),
                "translations",
                Logger.getLogger("TranslationResourcesTest"),
                getClass().getClassLoader());

        assertEquals(TranslationInstaller.ALL_LANGUAGES.size(), installed);

        Path translations = temporaryDirectory.resolve("translations");
        for (String code : TranslationInstaller.ALL_LANGUAGES) {
            Path file = translations.resolve("messages_" + code + ".yml");
            assertTrue(Files.isRegularFile(file), "Risorsa mancante: " + code);

            String yaml = new String(
                    Files.readAllBytes(file), StandardCharsets.UTF_8);
            assertTrue(yaml.contains("prefix:"),
                    "Catalogo privo della chiave prefix: " + code);
            assertTrue(yaml.contains("command:"),
                    "Catalogo privo della sezione command: " + code);
        }

        int installedAgain = TranslationInstaller.install(
                temporaryDirectory.toFile(),
                "translations",
                Logger.getLogger("TranslationResourcesTest"),
                getClass().getClassLoader());
        assertEquals(0, installedAgain,
                "I file personalizzabili non devono essere sovrascritti");
    }
}
