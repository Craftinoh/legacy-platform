package it.legacynetwork.language.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import it.legacynetwork.language.velocity.service.LanguageSynchronizationService;
import it.legacynetwork.language.velocity.service.ProxyLanguageService;

public final class PlayerSettingsListener {
    private final ProxyLanguageService languageService;
    private final LanguageSynchronizationService synchronizationService;

    public PlayerSettingsListener(ProxyLanguageService languageService,
                                  LanguageSynchronizationService synchronizationService) {
        this.languageService = languageService;
        this.synchronizationService = synchronizationService;
    }

    @Subscribe
    public void onSettingsChanged(PlayerSettingsChangedEvent event) {
        if (languageService.updateAutomaticLocale(event.getPlayer())) {
            synchronizationService.synchronize(event.getPlayer());
        }
    }
}
