package it.legacynetwork.language;

import java.util.UUID;

/**
 * Backend-facing service used by Bukkit plugins to request an authoritative
 * language change from the proxy.
 */
public interface PlayerLanguageChangeRequestService {

    /**
     * Sends a language-change request for an online player.
     *
     * @return true when the request was sent to the proxy, false when no
     *         carrier/player was available or the payload could not be sent
     */
    boolean requestLanguageChange(UUID playerId, Language language);
}
