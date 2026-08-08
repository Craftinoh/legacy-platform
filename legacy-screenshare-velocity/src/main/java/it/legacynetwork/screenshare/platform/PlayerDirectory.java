package it.legacynetwork.screenshare.platform;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Giocatori attualmente collegati al proxy.
 */
public interface PlayerDirectory {

    Optional<OnlinePlayer> findByName(String name);

    Optional<OnlinePlayer> findById(UUID playerId);

    /**
     * Giocatori collegati che possiedono il permesso indicato.
     */
    List<OnlinePlayer> withPermission(String node);

    /**
     * Nomi collegati, per i suggerimenti dei comandi.
     */
    List<String> names();
}
