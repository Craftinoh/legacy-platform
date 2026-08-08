package it.legacynetwork.screenshare.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.language.TranslationBundle;
import it.legacynetwork.language.TranslationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Traduzioni di LegacyScreenshare.
 *
 * <p>Stesso modello di LegacyLobby e di LegacyReports: {@link TranslationService}
 * di {@code language-common}, bundle inclusi nell'artefatto e catena di
 * fallback — lingua richiesta, fallback configurato, inglese. La lingua del
 * giocatore non viene mai ridotta.</p>
 */
public final class ScreenshareMessages {

    private static final String RESOURCE_PREFIX =
            "/screenshare/translations/messages_";

    private static final String MISSING_PREFIX = "missing:";

    private final TranslationService translations;
    private final Language fallback;

    public ScreenshareMessages(TranslationService translations,
                               Language fallback) {
        if (translations == null || fallback == null) {
            throw new IllegalArgumentException(
                    "Traduzioni screenshare incomplete");
        }
        this.translations = translations;
        this.fallback = fallback;
    }

    public static ScreenshareMessages load(Language fallback) {
        Map<Language, TranslationBundle> bundles =
                new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            Map<String, String> entries = read(language.getCode());
            if (!entries.isEmpty()) {
                bundles.put(language, new TranslationBundle(entries));
            }
        }
        if (!bundles.containsKey(fallback)) {
            bundles.put(fallback, new TranslationBundle(new HashMap<>()));
        }
        return new ScreenshareMessages(new TranslationService(bundles),
                fallback);
    }

    private static Map<String, String> read(String code) {
        Map<String, String> entries = new HashMap<>();
        InputStream stream = ScreenshareMessages.class.getResourceAsStream(
                RESOURCE_PREFIX + code + ".properties");
        if (stream == null) {
            return entries;
        }
        try {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(stream,
                    StandardCharsets.UTF_8));
            for (String key : properties.stringPropertyNames()) {
                entries.put(key, properties.getProperty(key));
            }
        } catch (IOException unreadable) {
            return entries;
        } finally {
            closeQuietly(stream);
        }
        return entries;
    }

    private static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // Chiusura best effort del bundle incluso nell'artefatto.
        }
    }

    public String get(Language language, String key) {
        return get(language, key, PlaceholderValues.empty());
    }

    public String get(Language language, String key,
                      PlaceholderValues placeholders) {
        Language target = language == null ? fallback : language;
        String text = translations.translate(target, key, placeholders);
        if (isMissing(text, key) && target != fallback) {
            text = translations.translate(fallback, key, placeholders);
        }
        if (isMissing(text, key) && fallback != Language.ENGLISH) {
            text = translations.translate(Language.ENGLISH, key, placeholders);
        }
        return text;
    }

    /**
     * Indica se la chiave e' risolvibile, catena di fallback compresa.
     */
    public boolean has(Language language, String key) {
        return !isMissing(get(language, key), key);
    }

    private static boolean isMissing(String text, String key) {
        return text == null || text.equals(MISSING_PREFIX + key);
    }

    public Language getFallback() {
        return fallback;
    }
}
