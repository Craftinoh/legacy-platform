package it.legacynetwork.combat;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyCombatTranslationTest {

    private static final List<String> LANGUAGES = Arrays.asList(
            "en", "it", "es", "fr", "de", "pt", "pt_br", "nl", "pl", "ro",
            "hu", "cs", "sk", "sl", "hr", "bg", "el", "da", "sv", "no",
            "fi", "is", "et", "lv", "lt", "ga", "mt", "ru", "uk", "tr", "sr");

    @Test
    void all31LanguageFilesExist() {
        String base = "../legacy-combat/src/main/resources/translations/";
        File dir = new File(base);
        assertTrue(dir.exists(), "Translations directory missing");
        for (String lang : LANGUAGES) {
            File f = new File(dir, "messages_" + lang + ".yml");
            assertTrue(f.exists(), "Missing: " + f.getPath());
        }
    }

    @Test
    void noTranslationIsEnglishCopy() {
        String base = "../legacy-combat/src/main/resources/translations/";
        File dir = new File(base);
        if (!dir.exists()) return;
        File enFile = new File(dir, "messages_en.yml");
        if (!enFile.exists()) return;
        String english = readFile(enFile);
        for (String lang : LANGUAGES) {
            if ("en".equals(lang)) continue;
            File f = new File(dir, "messages_" + lang + ".yml");
            if (!f.exists()) continue;
            assertFalse(english.equals(readFile(f)),
                    lang + " is a copy of English");
        }
    }

    @Test
    void filesContainRealContent() {
        String base = "../legacy-combat/src/main/resources/translations/";
        File dir = new File(base);
        if (!dir.exists()) return;
        for (String lang : LANGUAGES) {
            File f = new File(dir, "messages_" + lang + ".yml");
            if (!f.exists()) continue;
            String content = readFile(f);
            assertFalse(content.trim().isEmpty(), lang + " is empty");
            assertTrue(content.length() > 10, lang + " too short");
        }
    }

    @Test
    void ptAndPtBrAreDistinct() {
        String base = "../legacy-combat/src/main/resources/translations/";
        File dir = new File(base);
        if (!dir.exists()) return;
        String pt = readFile(new File(dir, "messages_pt.yml"));
        String ptbr = readFile(new File(dir, "messages_pt_br.yml"));
        assertFalse(pt.equals(ptbr), "pt and pt_br must be distinct");
    }

    private String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
