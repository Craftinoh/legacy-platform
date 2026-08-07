package it.legacynetwork.lobby.language;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;

import java.util.UUID;

/**
 * Thin adapter around the authoritative LanguageBackend service.
 * LegacyLobby must never keep a second language cache or register a competing
 * PlayerLanguageProvider implementation.
 */
public final class BackendLanguageService implements PlayerLanguageProvider {
    private final PlayerLanguageProvider provider;
    private volatile Language fallback;

    public BackendLanguageService(PlayerLanguageProvider provider,
                                  String fallbackLanguage) {
        this.provider = provider;
        setFallbackLanguage(fallbackLanguage);
    }

    public void setFallbackLanguage(String fallbackLanguage) {
        this.fallback = Language.findByInput(fallbackLanguage)
                .orElse(Language.ENGLISH);
    }

    public Language get(UUID playerUuid) {
        if (provider == null || playerUuid == null) {
            return fallback;
        }
        try {
            Language language = provider.getLanguage(playerUuid);
            return language != null ? language : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        } catch (LinkageError error) {
            return fallback;
        }
    }

    @Override
    public Language getLanguage(UUID playerId) {
        return get(playerId);
    }

    /**
     * Kept for source compatibility with the old listener. The authoritative
     * cache belongs to LanguageBackend and is updated only by proxy sync.
     */
    @Deprecated
    public void update(UUID playerUuid, Language language) {
        // no-op by design
    }

    @Deprecated
    public void remove(UUID playerUuid) {
        // no-op by design
    }

    @Deprecated
    public void clear() {
        // no-op by design
    }
}
