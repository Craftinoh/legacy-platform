package it.legacynetwork.lobby.language;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageChangeListener;
import it.legacynetwork.language.PlayerLanguageEventService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BukkitPlayerLanguageEventService implements PlayerLanguageEventService {
    private final List<PlayerLanguageChangeListener> listeners =
            new CopyOnWriteArrayList<>();

    @Override
    public void registerListener(PlayerLanguageChangeListener listener) {
        listeners.add(listener);
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
                listener.onLanguageChanged(playerId, previousLanguage, newLanguage);
            } catch (Exception e) {
            }
        }
    }
}
