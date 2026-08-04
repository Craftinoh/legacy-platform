package it.legacynetwork.language;

import java.util.EnumMap;
import java.util.Map;

public final class TranslationService {
    private final Map<Language, TranslationBundle> bundles;

    public TranslationService(Map<Language, TranslationBundle> bundles) {
        this.bundles = new EnumMap<Language, TranslationBundle>(Language.class);
        this.bundles.putAll(bundles);
    }

    public String translate(Language language, String key) {
        return translate(language, key, PlaceholderValues.empty());
    }

    public String translate(Language language, String key, PlaceholderValues placeholders) {
        TranslationBundle bundle = bundles.get(language);
        if (bundle == null) {
            return "missing:" + key;
        }
        return placeholders.apply(bundle.get(key));
    }
}
