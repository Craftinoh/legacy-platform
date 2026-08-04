package it.legacynetwork.lobby.translation;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.TranslationBundle;
import it.legacynetwork.language.TranslationService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class PropertiesTranslationLoader {
    private final JavaPlugin plugin;

    public PropertiesTranslationLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public TranslationService load() throws IOException {
        Map<Language, TranslationBundle> bundles =
                new EnumMap<Language, TranslationBundle>(Language.class);
        bundles.put(Language.ITALIAN,
                loadBundle("translations/lobby_it.properties"));
        bundles.put(Language.ENGLISH,
                loadBundle("translations/lobby_en.properties"));
        return new TranslationService(bundles);
    }

    private TranslationBundle loadBundle(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = plugin.getResource(resource)) {
            if (inputStream == null) {
                throw new IOException("Risorsa traduzioni non trovata: " + resource);
            }
            try (InputStreamReader reader =
                         new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }
        return new TranslationBundle(values);
    }
}
