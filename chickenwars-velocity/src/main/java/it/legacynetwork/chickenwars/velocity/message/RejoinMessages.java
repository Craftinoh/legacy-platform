package it.legacynetwork.chickenwars.velocity.message;

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
 * Traduzioni del comando di rientro.
 *
 * <p>Riusa {@link TranslationService} di {@code language-common}: il proxy non
 * possiede un secondo sistema linguistico e nessun testo visibile e' scritto in
 * Java.</p>
 */
public final class RejoinMessages {

    private static final String RESOURCE_PREFIX =
            "/chickenwars/translations/messages_";

    private final TranslationService translations;
    private final Language fallback;

    public RejoinMessages(TranslationService translations, Language fallback) {
        if (translations == null || fallback == null) {
            throw new IllegalArgumentException("Traduzioni rejoin incomplete");
        }
        this.translations = translations;
        this.fallback = fallback;
    }

    /**
     * Carica i bundle inclusi nell'artefatto.
     *
     * <p>Le lingue senza file ricadono su quella predefinita, esattamente come
     * fa il resto della rete.</p>
     */
    public static RejoinMessages load(Language fallback) {
        Map<Language, TranslationBundle> bundles =
                new EnumMap<Language, TranslationBundle>(Language.class);
        for (Language language : Language.values()) {
            Map<String, String> entries = read(language.getCode());
            if (!entries.isEmpty()) {
                bundles.put(language, new TranslationBundle(entries));
            }
        }
        if (!bundles.containsKey(fallback)) {
            bundles.put(fallback, new TranslationBundle(
                    new HashMap<String, String>()));
        }
        return new RejoinMessages(new TranslationService(bundles), fallback);
    }

    private static Map<String, String> read(String code) {
        Map<String, String> entries = new HashMap<String, String>();
        InputStream stream = RejoinMessages.class.getResourceAsStream(
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

    /**
     * Traduce una chiave nella lingua indicata.
     */
    public String get(Language language, String key) {
        return translations.translate(
                language == null ? fallback : language, key);
    }

    /**
     * Traduce una chiave sostituendo i segnaposto.
     */
    public String get(Language language, String key,
                      PlaceholderValues placeholders) {
        return translations.translate(language == null ? fallback : language,
                key, placeholders);
    }

    public Language getFallback() {
        return fallback;
    }
}
