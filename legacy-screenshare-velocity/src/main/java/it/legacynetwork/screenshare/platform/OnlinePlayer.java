package it.legacynetwork.screenshare.platform;

import it.legacynetwork.screenshare.message.ChatLine;

import java.util.UUID;

/**
 * Giocatore collegato al proxy.
 *
 * <p>Solo cio' che Velocity conosce: identita', server corrente e permessi. Il
 * proxy non vede inventario, posizione o stato di combattimento, e nulla qui
 * finge il contrario.</p>
 */
public interface OnlinePlayer {

    UUID uniqueId();

    String name();

    /**
     * Server a cui il giocatore e' collegato, vuoto se in transito.
     */
    String serverId();

    boolean hasPermission(String node);

    void send(ChatLine line);
}
