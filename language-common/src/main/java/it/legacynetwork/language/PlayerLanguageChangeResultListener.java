package it.legacynetwork.language;

import java.util.UUID;

/**
 * Receives the explicit outcome of a language-change request.
 */
public interface PlayerLanguageChangeResultListener {

    void onLanguageChangeResult(UUID playerId,
                                Language requestedLanguage,
                                LanguageChangeResult result);
}
