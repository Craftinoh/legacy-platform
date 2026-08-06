package it.legacynetwork.language;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationParityTest {

    private static final List<String> LANGUAGES = Arrays.asList(
            "en", "it", "es", "fr", "de", "pt", "pt_br", "nl", "pl", "ro",
            "hu", "cs", "sk", "sl", "hr", "bg", "el", "da", "sv", "no",
            "fi", "is", "et", "lv", "lt", "ga", "mt", "ru", "uk", "tr", "sr");

    private static final String[] MODULES = {
            "legacy-lobby", "legacy-items", "legacy-menu", "legacy-combat", "language-backend"
    };

    @Test
    void legacyLobbyHasAll31Languages() {
        assertAllLanguages("legacy-lobby");
    }

    @Test
    void legacyItemsHasAll31Languages() {
        assertAllLanguages("legacy-items");
    }

    @Test
    void legacyMenuHasAll31Languages() {
        assertAllLanguages("legacy-menu");
    }

    @Test
    void legacyCombatHasAll31Languages() {
        assertAllLanguages("legacy-combat");
    }

    @Test
    void languageBackendHasAll31Languages() {
        assertAllLanguages("language-backend");
    }

    @Test
    void noTranslationIsEnglishCopy() {
        for (String module : MODULES) {
            String base = "../" + module + "/src/main/resources/translations/";
            File dir = new File(base);
            if (!dir.exists()) continue;

            String english = readFile(new File(dir, "messages_en.yml"));
            for (String lang : LANGUAGES) {
                if ("en".equals(lang)) continue;
                File f = new File(dir, "messages_" + lang + ".yml");
                if (!f.exists()) continue;
                String content = readFile(f);
                assertFalse(english.equals(content),
                        module + "/" + lang + " is a copy of English");
            }
        }
    }

    @Test
    void allFilesContainContent() {
        for (String module : MODULES) {
            String base = "../" + module + "/src/main/resources/translations/";
            File dir = new File(base);
            if (!dir.exists()) continue;
            for (String lang : LANGUAGES) {
                File f = new File(dir, "messages_" + lang + ".yml");
                if (!f.exists()) continue;
                String content = readFile(f);
                assertFalse(content.trim().isEmpty(),
                        module + "/" + lang + " is empty");
                assertTrue(content.length() > 10,
                        module + "/" + lang + " too short");
            }
        }
    }

    private void assertAllLanguages(String module) {
        String base = "../" + module + "/src/main/resources/translations/";
        File dir = new File(base);
        assertTrue(dir.exists(), module + " translations dir missing at " + dir.getAbsolutePath());
        for (String lang : LANGUAGES) {
            File f = new File(dir, "messages_" + lang + ".yml");
            assertTrue(f.exists(), "Missing: " + f.getPath());
        }
    }

    private String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
