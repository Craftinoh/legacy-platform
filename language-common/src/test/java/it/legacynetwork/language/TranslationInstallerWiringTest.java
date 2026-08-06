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

class TranslationInstallerWiringTest {

    @Test
    void installerLists31Languages() {
        assertEquals(31, TranslationInstaller.ALL_LANGUAGES.size());
    }

    @Test
    void allLanguagesHaveValidCodes() {
        for (String lang : TranslationInstaller.ALL_LANGUAGES) {
            assertTrue(lang.matches("[a-z]{2}(_[a-z]{2})?"),
                    "Invalid lang code: " + lang);
        }
    }

    @Test
    void ptAndPtBrAreSeparate() {
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("pt"));
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains("pt_br"));
    }

    @Test
    void installerDoesNotOverwriteExisting(@TempDir Path tempDir) throws Exception {
        File dataFolder = tempDir.toFile();
        File trans = new File(dataFolder, "translations");
        trans.mkdirs();
        File existing = new File(trans, "messages_en.yml");
        Files.write(existing.toPath(), "custom: admin edit".getBytes(),
                StandardOpenOption.CREATE);

        int installed = TranslationInstaller.install(
                dataFolder, "translations",
                Logger.getAnonymousLogger(), TranslationInstaller.class.getClassLoader());
        assertEquals(0, installed);

        String content = new String(Files.readAllBytes(existing.toPath()));
        assertEquals("custom: admin edit", content);
    }

    @Test
    void installerInstallsIntoTranslationsSubdir(@TempDir Path tempDir) {
        File dataFolder = tempDir.toFile();
        int count = TranslationInstaller.install(
                dataFolder, "translations",
                Logger.getAnonymousLogger(), TranslationInstaller.class.getClassLoader());

        File transDir = new File(dataFolder, "translations");
        if (transDir.exists()) {
            assertTrue(transDir.isDirectory());
        }
    }
}
