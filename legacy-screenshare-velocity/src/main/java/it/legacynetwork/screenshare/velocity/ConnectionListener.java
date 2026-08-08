package it.legacynetwork.screenshare.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import it.legacynetwork.screenshare.service.ScreenshareService;
import it.legacynetwork.screenshare.session.ActiveSessionRegistry;

import java.util.UUID;

/**
 * Disconnessioni e rientri durante un controllo.
 *
 * <p>Il bersaglio che se ne va chiude la sessione in violazione; lo staffer che
 * se ne va segue la politica configurata. Nessun comando di punizione viene
 * eseguito: la violazione va alla porta dedicata.</p>
 */
public final class ConnectionListener {

    private final ScreenshareService service;
    private final ActiveSessionRegistry registry;

    public ConnectionListener(ScreenshareService service,
                              ActiveSessionRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (registry.isLocked(playerId)) {
            service.onTargetDisconnect(playerId);
            return;
        }
        if (registry.sessionOfStaff(playerId).isPresent()) {
            service.onStaffDisconnect(playerId);
        }
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (registry.sessionOfStaff(playerId).isPresent()) {
            service.onStaffReconnect(playerId);
        }
    }
}
