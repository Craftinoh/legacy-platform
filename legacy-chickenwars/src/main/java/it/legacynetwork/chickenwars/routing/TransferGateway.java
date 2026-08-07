package it.legacynetwork.chickenwars.routing;

import java.util.UUID;

/** Confine verso proxy/messaging: trasferisce solo prenotazioni valide. */
public interface TransferGateway {
    void transfer(UUID playerId, String serverName, String reservationId,
                  String arenaId);
}
