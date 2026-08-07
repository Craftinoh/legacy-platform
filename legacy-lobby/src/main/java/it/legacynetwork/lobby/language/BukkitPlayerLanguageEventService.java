package it.legacynetwork.lobby.language;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.LanguageChangeResult;
import it.legacynetwork.language.PlayerLanguageChangeListener;
import it.legacynetwork.language.PlayerLanguageChangeResultListener;
import it.legacynetwork.language.PlayerLanguageEventService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BukkitPlayerLanguageEventService
        implements PlayerLanguageEventService {
    private final List<PlayerLanguageChangeListener> listeners =
            new CopyOnWriteArrayList<PlayerLanguageChangeListener>();
    private final List<PlayerLanguageChangeResultListener> resultListeners =
            new CopyOnWriteArrayList<PlayerLanguageChangeResultListener>();

    @Override
    public void registerListener(PlayerLanguageChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(PlayerLanguageChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void fireLanguageChanged(UUID playerId,
                                     Language previousLanguage,
                                     Language newLanguage) {
        for (PlayerLanguageChangeListener listener : listeners) {
            try {
                listener.onLanguageChanged(
                        playerId, previousLanguage, newLanguage);
            } catch (RuntimeException ignored) {
                // Legacy compatibility implementation: isolate listeners.
            }
        }
    }

    @Override
    public void registerResultListener(
            PlayerLanguageChangeResultListener listener) {
        if (listener != null) {
            resultListeners.add(listener);
        }
    }

    @Override
    public void unregisterResultListener(
            PlayerLanguageChangeResultListener listener) {
        resultListeners.remove(listener);
    }

    @Override
    public void fireLanguageChangeResult(UUID playerId,
                                         Language requestedLanguage,
                                         LanguageChangeResult result) {
        for (PlayerLanguageChangeResultListener listener : resultListeners) {
            try {
                listener.onLanguageChangeResult(
                        playerId, requestedLanguage, result);
            } catch (RuntimeException ignored) {
                // Legacy compatibility implementation: isolate listeners.
            }
        }
    }
}
