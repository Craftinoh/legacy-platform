package it.legacynetwork.language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LocalizedConfigurationResolver {

    private LocalizedConfigurationResolver() {
    }

    public static String resolveString(Map<String, ?> translations,
                                         String languageCode, String key) {
        if (translations == null) return null;
        Map<?, ?> langData = getLanguageData(translations, languageCode);
        if (langData == null) return null;
        Object value = langData.get(key);
        return value instanceof String ? (String) value : null;
    }

    @SuppressWarnings("unchecked")
    public static List<String> resolveStringList(Map<String, ?> translations,
                                                  String languageCode, String key) {
        if (translations == null) return Collections.emptyList();
        Map<?, ?> langData = getLanguageData(translations, languageCode);
        if (langData == null) return Collections.emptyList();
        Object value = langData.get(key);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        if (value instanceof String) {
            return Collections.singletonList((String) value);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static String resolveTitle(Map<String, ?> titles, String languageCode) {
        if (titles == null) return null;
        String result = (String) titles.get(languageCode);
        if (result != null) return result;
        return (String) titles.get("en");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, ?> translate(Map<String, ?> translations,
                                            String languageCode) {
        if (translations == null) {
            return Collections.emptyMap();
        }
        Map<?, ?> langData = getLanguageData(translations, languageCode);
        if (langData instanceof Map) {
            return (Map<String, ?>) langData;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> getLanguageData(Map<String, ?> translations,
                                              String languageCode) {
        Map<?, ?> langData = (Map<?, ?>) translations.get(languageCode);
        if (langData == null) {
            langData = (Map<?, ?>) translations.get("en");
        }
        return langData;
    }

    @SuppressWarnings("unchecked")
    public static String resolveLegacy(Map<String, ?> root,
                                        String languageCode,
                                        String legacyKey,
                                        String translationsKey,
                                        String fieldKey) {
        if (root == null) return null;
        Map<?, ?> translations = (Map<?, ?>) root.get(translationsKey);
        if (translations != null) {
            String result = resolveString((Map<String, ?>) translations,
                    languageCode, fieldKey);
            if (result != null) return result;
        }
        Object legacy = root.get(legacyKey);
        return legacy instanceof String ? (String) legacy : null;
    }

    @SuppressWarnings("unchecked")
    public static List<String> resolveLegacyList(Map<String, ?> root,
                                                  String languageCode,
                                                  String legacyLoreKey,
                                                  String translationsKey,
                                                  String fieldKey) {
        if (root == null) return Collections.emptyList();
        Map<?, ?> translations = (Map<?, ?>) root.get(translationsKey);
        if (translations != null) {
            List<String> result = resolveStringList(
                    (Map<String, ?>) translations, languageCode, fieldKey);
            if (!result.isEmpty()) return result;
        }
        Object legacy = root.get(legacyLoreKey);
        if (legacy instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) legacy) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        if (legacy instanceof String) {
            return Collections.singletonList((String) legacy);
        }
        return Collections.emptyList();
    }
}
