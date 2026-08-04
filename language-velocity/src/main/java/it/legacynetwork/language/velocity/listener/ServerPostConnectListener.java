package it.legacynetwork.language.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import it.legacynetwork.language.velocity.service.LanguageSynchronizationService;

public final class ServerPostConnectListener {
    private final LanguageSynchronizationService synchronizationService;

    public ServerPostConnectListener(
            LanguageSynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        synchronizationService.synchronize(event.getPlayer());
    }
}
