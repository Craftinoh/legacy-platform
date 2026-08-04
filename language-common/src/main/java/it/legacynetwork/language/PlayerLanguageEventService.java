package it.legacynetwork.language;

public interface PlayerLanguageEventService {

    void registerListener(PlayerLanguageChangeListener listener);

    void unregisterListener(PlayerLanguageChangeListener listener);

    void fireLanguageChanged(java.util.UUID playerId,
                              Language previousLanguage,
                              Language newLanguage);
}
