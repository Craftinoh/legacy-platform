package it.legacynetwork.language;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public final class TranslationInstaller {

    public static final List<String> ALL_LANGUAGES = Arrays.asList(
            "en", "it", "es", "fr", "de", "pt", "pt_br", "nl", "pl", "ro",
            "hu", "cs", "sk", "sl", "hr", "bg", "el", "da", "sv", "no",
            "fi", "is", "et", "lv", "lt", "ga", "mt", "ru", "uk", "tr", "sr");

    private TranslationInstaller() {
    }

    public static int install(File dataFolder, String translationsDir,
                               Logger logger, ClassLoader loader) {
        File dir = new File(dataFolder, translationsDir);
        dir.mkdirs();
        int installed = 0;
        for (String lang : ALL_LANGUAGES) {
            String resource = translationsDir + "/messages_" + lang + ".yml";
            String fileName = "messages_" + lang + ".yml";
            try (InputStream in = loader.getResourceAsStream(resource)) {
                if (in == null) {
                    logger.warning("Risorsa mancante: " + resource);
                    continue;
                }
                File target = new File(dir, fileName);
                if (!target.exists()) {
                    Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    installed++;
                }
            } catch (Exception e) {
                logger.warning("Installazione fallita " + resource + ": " + e.getMessage());
            }
        }
        return installed;
    }
}
