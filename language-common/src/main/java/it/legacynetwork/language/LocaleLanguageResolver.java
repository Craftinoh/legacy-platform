package it.legacynetwork.language;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LocaleLanguageResolver {

    private static final Map<String, Language> ALIAS_MAP = buildAliasMap();

    private static Map<String, Language> buildAliasMap() {
        Map<String, Language> map = new HashMap<String, Language>();
        for (Language language : Language.values()) {
            for (String alias : language.getAliases()) {
                map.put(alias, language);
            }
        }
        return map;
    }

    public Language resolve(String locale) {
        if (locale == null || locale.trim().isEmpty()) {
            return Language.ENGLISH;
        }
        String normalized = locale.trim().replace('-', '_').toLowerCase(Locale.ROOT);
        Language exact = ALIAS_MAP.get(normalized);
        if (exact != null) {
            return exact;
        }
        String languagePart = normalized.split("_", 2)[0];
        Language byBase = ALIAS_MAP.get(languagePart);
        if (byBase != null) {
            return byBase;
        }
        return Language.ENGLISH;
    }

    public Language resolveOrDefault(String locale) {
        return resolve(locale);
    }
}
