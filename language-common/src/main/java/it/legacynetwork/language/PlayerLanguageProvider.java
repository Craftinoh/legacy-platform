package it.legacynetwork.language;

import java.util.UUID;

public interface PlayerLanguageProvider {

    Language getLanguage(UUID playerId);
}
