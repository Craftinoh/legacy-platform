package it.legacynetwork.menu;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyMenuTranslationTest {

    private static final List<String> LANGUAGES = Arrays.asList(
            "en", "it", "es", "fr", "de", "pt", "pt_br", "nl", "pl", "ro",
            "hu", "cs", "sk", "sl", "hr", "bg", "el", "da", "sv", "no",
            "fi", "is", "et", "lv", "lt", "ga", "mt", "ru", "uk", "tr", "sr");

    @Test
    void all31LanguageFilesExist() {
        String base = "../legacy-menu/src/main/resources/translations/";
        File dir = new File(base);
        assertTrue(dir.exists(), "Translations directory missing");
        for (String lang : LANGUAGES) {
            File f = new File(dir, "messages_" + lang + ".yml");
            assertTrue(f.exists(), "Missing: " + f.getPath());
        }
    }

    @Test
    void noTranslationIsEnglishCopy() {
        String base = "../legacy-menu/src/main/resources/translations/";
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
        String base = "../legacy-menu/src/main/resources/translations/";
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
    void noModernApiUsedInSource() throws Exception {
        String[] sources = {
                "src/main/java/it/legacynetwork/menu/lang/LanguageMenuService.java",
                "src/main/java/it/legacynetwork/menu/lang/SkullTextureUtil.java",
                "src/main/java/it/legacynetwork/menu/lang/FlagTextureService.java"
        };
        String[] banned = {"PLAYER_HEAD", "PlayerProfile",
                "PersistentDataContainer", "NamespacedKey", "CustomModelData",
                "net.kyori.adventure"};

        for (String src : sources) {
            File f = new File(src);
            if (!f.exists()) continue;
            String content = new String(Files.readAllBytes(f.toPath()), "UTF-8");
            for (String ban : banned) {
                assertFalse(content.contains(ban),
                        src + " contains banned API: " + ban);
            }
        }
    }

    @Test
    void skullTextureUtilUsesSkullItem() throws Exception {
        File f = new File("src/main/java/it/legacynetwork/menu/lang/FlagTextureService.java");
        if (!f.exists()) { f = new File("../legacy-menu/src/main/java/it/legacynetwork/menu/lang/FlagTextureService.java"); }
        if (!f.exists()) return;
        String content = new String(Files.readAllBytes(f.toPath()), "UTF-8");
        assertTrue(content.contains("SKULL_ITEM"),
                "FlagTextureService must use SKULL_ITEM");

        File g = new File("src/main/java/it/legacynetwork/menu/lang/SkullTextureUtil.java");
        if (!g.exists()) { g = new File("../legacy-menu/src/main/java/it/legacynetwork/menu/lang/SkullTextureUtil.java"); }
        if (!g.exists()) return;
        String gContent = new String(Files.readAllBytes(g.toPath()), "UTF-8");
        assertTrue(gContent.contains("GameProfile"),
                "SkullTextureUtil must use GameProfile");
    }

    private String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
