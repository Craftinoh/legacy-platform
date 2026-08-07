package it.legacynetwork.chickenwars.lobby;

import it.legacynetwork.chickenwars.routing.TransferGateway;
import org.bukkit.entity.Player;

/** Trasferisce al server lobby configurato, senza indirizzi hardcoded. */
public final class ReturnLobbyService {
    private final TransferGateway gateway;
    private final String serverName;

    public ReturnLobbyService(TransferGateway gateway, String serverName) {
        if (gateway == null) {
            throw new IllegalArgumentException("Gateway lobby mancante");
        }
        this.gateway = gateway;
        this.serverName = serverName == null ? "" : serverName.trim();
    }

    public boolean transfer(Player player) {
        if (player == null || !player.isOnline() || serverName.isEmpty()) {
            return false;
        }
        gateway.connect(player.getUniqueId(), serverName);
        return true;
    }
}
