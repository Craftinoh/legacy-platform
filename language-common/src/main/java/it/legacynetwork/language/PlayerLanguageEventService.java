package it.legacynetwork.language;

import java.util.UUID;

public interface PlayerLanguageEventService {

    void registerListener(PlayerLanguageChangeListener listener);

    void unregisterListener(PlayerLanguageChangeListener listener);

    void fireLanguageChanged(UUID playerId,
                             Language previousLanguage,
                             Language newLanguage);

    void registerResultListener(PlayerLanguageChangeResultListener listener);

    void unregisterResultListener(PlayerLanguageChangeResultListener listener);

    void fireLanguageChangeResult(UUID playerId,
                                  Language requestedLanguage,
                                  LanguageChangeResult result);
}
