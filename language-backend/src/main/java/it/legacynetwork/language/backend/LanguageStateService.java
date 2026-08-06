package it.legacynetwork.language.backend;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageChangeListener;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LanguageStateService {

    private final Map<UUID, LanguageState> cache = new ConcurrentHashMap<>();
    private final List<PlayerLanguageChangeListener> listeners =
            new CopyOnWriteArrayList<>();
    private final String fallbackCode;

    public LanguageStateService(String fallbackCode) {
        this.fallbackCode = fallbackCode != null ? fallbackCode : "en";
    }

    public Language getLanguage(UUID playerId) {
        LanguageState state = cache.get(playerId);
        if (state == null) {
            return Language.findByInput(fallbackCode).orElse(Language.ENGLISH);
        }
        return state.language;
    }

    public LanguageState getState(UUID playerId) {
        return cache.get(playerId);
    }

    public void updateState(UUID playerId, Language language, String locale) {
        cache.put(playerId, new LanguageState(language, locale));
    }

    public void removeState(UUID playerId) {
        cache.remove(playerId);
    }

    public void clear() {
        cache.clear();
        listeners.clear();
    }

    public void registerListener(PlayerLanguageChangeListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(PlayerLanguageChangeListener listener) {
        listeners.remove(listener);
    }

    public void fireLanguageChanged(UUID playerId, Language previous, Language current) {
        for (PlayerLanguageChangeListener listener : listeners) {
            try {
                listener.onLanguageChanged(playerId, previous, current);
            } catch (Exception ignored) {
            }
        }
    }

    static final class LanguageState {
        final Language language;
        final String locale;

        LanguageState(Language language, String locale) {
            this.language = language;
            this.locale = locale;
        }
    }
}
