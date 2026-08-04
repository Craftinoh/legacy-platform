package it.legacynetwork.language;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TranslationBundle {
    private final Map<String, String> translations;

    public TranslationBundle(Map<String, String> translations) {
        this.translations = Collections.unmodifiableMap(
                new LinkedHashMap<String, String>(translations));
    }

    public String get(String key) {
        String value = translations.get(key);
        return value == null ? "missing:" + key : value;
    }
}
