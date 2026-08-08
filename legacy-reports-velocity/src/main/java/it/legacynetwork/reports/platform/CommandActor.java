package it.legacynetwork.reports.platform;

import it.legacynetwork.reports.message.ChatLine;

import java.util.UUID;

/**
 * Chi ha eseguito un comando.
 *
 * <p>Un giocatore ha un UUID, la console no: e' l'unica distinzione che serve
 * alla logica, e tenerla qui permette di provare i comandi senza avviare un
 * proxy.</p>
 */
public interface CommandActor {

    /**
     * Identificatore del giocatore, oppure {@code null} per la console.
     */
    UUID uniqueId();

    String name();

    boolean hasPermission(String node);

    void send(ChatLine line);

    default boolean isPlayer() {
        return uniqueId() != null;
    }
}
