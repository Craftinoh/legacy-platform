package it.legacynetwork.lobby.language;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BackendLanguageService implements PlayerLanguageProvider {
    private final Map<UUID, Language> languages =
            new ConcurrentHashMap<UUID, Language>();

    public Language get(UUID playerUuid) {
        Language language = languages.get(playerUuid);
        return language == null ? Language.ENGLISH : language;
    }

    @Override
    public Language getLanguage(UUID playerId) {
        return get(playerId);
    }

    public void update(UUID playerUuid, Language language) {
        languages.put(playerUuid, language);
    }

    public void remove(UUID playerUuid) {
        languages.remove(playerUuid);
    }

    public void clear() {
        languages.clear();
    }
}
