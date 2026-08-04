package it.legacynetwork.language.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import it.legacynetwork.language.velocity.service.ProxyLanguageService;

public final class PlayerConnectedListener {
    private final ProxyLanguageService languageService;

    public PlayerConnectedListener(ProxyLanguageService languageService) {
        this.languageService = languageService;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        languageService.initialize(event.getPlayer());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        languageService.remove(event.getPlayer());
    }
}
