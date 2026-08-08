package it.legacynetwork.screenshare.velocity;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import it.legacynetwork.screenshare.service.ScreenshareService;
import it.legacynetwork.screenshare.session.ActiveSessionRegistry;
import java.util.UUID;
public final class ConnectionListener {
    private final ScreenshareService service;
    private final ActiveSessionRegistry registry;
    public ConnectionListener(ScreenshareService service, ActiveSessionRegistry registry) { this.service=service; this.registry=registry; }
    @Subscribe public void onDisconnect(DisconnectEvent event) {
        if(service.isShuttingDown()) return;
        UUID id=event.getPlayer().getUniqueId();
        if(registry.isLocked(id)) { service.onTargetDisconnect(id); return; }
        if(registry.sessionOfStaff(id).isPresent()) service.onStaffDisconnect(id);
    }
    @Subscribe public void onPostLogin(PostLoginEvent event) { service.onPlayerReconnect(event.getPlayer().getUniqueId()); }
    @Subscribe public void onServerPostConnect(ServerPostConnectEvent event) {
        UUID id=event.getPlayer().getUniqueId();
        service.onPlayerReconnect(id);
        if(registry.sessionOfStaff(id).isPresent()) service.onStaffReconnect(id);
    }
}
