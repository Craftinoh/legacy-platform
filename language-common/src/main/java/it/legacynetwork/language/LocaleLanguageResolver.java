package it.legacynetwork.language;

import java.util.Locale;

public final class LocaleLanguageResolver {
    public Language resolve(String locale) {
        if (locale == null || locale.trim().isEmpty()) {
            return Language.ENGLISH;
        }
        String normalized = locale.trim().replace('-', '_').toLowerCase(Locale.ROOT);
        String languagePart = normalized.split("_", 2)[0];
        if ("it".equals(languagePart)) {
            return Language.ITALIAN;
        }
        return Language.ENGLISH;
    }
}
