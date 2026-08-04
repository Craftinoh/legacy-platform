package it.legacynetwork.items.language;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;

import java.util.UUID;

public final class DefaultPlayerLanguageProvider implements PlayerLanguageProvider {
    private final String fallbackCode;

    public DefaultPlayerLanguageProvider(String fallbackCode) {
        this.fallbackCode = fallbackCode != null ? fallbackCode : "en";
    }

    @Override
    public Language getLanguage(UUID playerId) {
        if ("it".equalsIgnoreCase(fallbackCode)) {
            return Language.ITALIAN;
        }
        return Language.ENGLISH;
    }
}
