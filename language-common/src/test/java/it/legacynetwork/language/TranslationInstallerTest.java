package it.legacynetwork.language;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationInstallerTest {

    @Test
    void all31LanguagesListed() {
        assertEquals(31, TranslationInstaller.ALL_LANGUAGES.size());
    }

    @Test
    void includesEnItEsFrDe() {
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("en"));
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("it"));
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("es"));
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("fr"));
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("de"));
    }

    @Test
    void includesSlavicLanguages() {
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("pl"));
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("cs"));
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("ru"));
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("sr"));
    }

    @Test
    void includesNordicLanguages() {
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("sv"));
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("no"));
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("da"));
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("fi"));
    }

    @Test
    void installReturnsZeroWhenNoResources(@TempDir Path tempDir) {
        File dataFolder = tempDir.toFile();
        int installed = TranslationInstaller.install(
                dataFolder, "translations",
                Logger.getAnonymousLogger(),
                getClass().getClassLoader());
        assertEquals(0, installed);
    }

    @Test
    void secondInstallDoesNotOverwrite(@TempDir Path tempDir) throws Exception {
        File dataFolder = tempDir.toFile();
        File translationsDir = new File(dataFolder, "translations");
        translationsDir.mkdirs();

        File testFile = new File(translationsDir, "messages_en.yml");
        Files.write(testFile.toPath(), "admin: modified".getBytes(),
                StandardOpenOption.CREATE);

        int installed = TranslationInstaller.install(
                dataFolder, "translations",
                Logger.getAnonymousLogger(),
                getClass().getClassLoader());
        assertEquals(0, installed);
    }
}
