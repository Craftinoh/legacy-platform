package it.legacynetwork.screenshare.platform;

import it.legacynetwork.screenshare.message.ChatLine;

import java.util.UUID;

/**
 * Chi ha eseguito un comando: un giocatore ha un UUID, la console no.
 */
public interface CommandActor {

    UUID uniqueId();

    String name();

    boolean hasPermission(String node);

    void send(ChatLine line);

    default boolean isPlayer() {
        return uniqueId() != null;
    }
}
