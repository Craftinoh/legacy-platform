package it.legacynetwork.reports.platform;

import it.legacynetwork.reports.message.ChatLine;

import java.util.UUID;

/**
 * Giocatore collegato al proxy.
 *
 * <p>Espone soltanto cio' che Velocity conosce davvero: identita', server di
 * destinazione, latenza della connessione e permessi. Nessun inventario, nessuno
 * stato di combattimento, nessuna cronologia chat.</p>
 */
public interface OnlinePlayer {

    UUID uniqueId();

    String name();

    /**
     * Server a cui il giocatore e' collegato, vuoto se in transito.
     */
    String serverId();

    /**
     * Latenza della connessione col proxy, in millisecondi.
     */
    long pingMillis();

    boolean hasPermission(String node);

    void send(ChatLine line);
}
