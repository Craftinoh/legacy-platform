package it.legacynetwork.language;

import java.util.UUID;

public interface PlayerLanguageChangeListener {

    void onLanguageChanged(UUID playerId, Language previousLanguage, Language newLanguage);
}
