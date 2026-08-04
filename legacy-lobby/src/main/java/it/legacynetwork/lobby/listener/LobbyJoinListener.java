package it.legacynetwork.lobby.listener;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.language.TranslationService;
import it.legacynetwork.lobby.config.LobbyConfiguration;
import it.legacynetwork.lobby.language.BackendLanguageService;
import it.legacynetwork.lobby.scoreboard.LobbyScoreboardService;
import it.legacynetwork.lobby.util.LegacyColorTranslator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class LobbyJoinListener implements Listener {
    private final LobbyConfiguration configuration;
    private final BackendLanguageService languageService;
    private final TranslationService translations;
    private final LobbyScoreboardService scoreboardService;

    public LobbyJoinListener(LobbyConfiguration configuration,
                             BackendLanguageService languageService,
                             TranslationService translations,
                             LobbyScoreboardService scoreboardService) {
        this.configuration = configuration;
        this.languageService = languageService;
        this.translations = translations;
        this.scoreboardService = scoreboardService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Language language = languageService.get(player.getUniqueId());
        scoreboardService.show(player);
        if (configuration.isWelcomeEnabled()) {
            String message = translations.translate(
                    language,
                    "welcome",
                    PlaceholderValues.builder().player(player.getName()).build());
            player.sendMessage(LegacyColorTranslator.translate(message));
        }
    }
}
