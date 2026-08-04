package it.legacynetwork.lobby.listener;

import it.legacynetwork.lobby.language.BackendLanguageService;
import it.legacynetwork.lobby.scoreboard.LobbyScoreboardService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class LobbyQuitListener implements Listener {
    private final BackendLanguageService languageService;
    private final LobbyScoreboardService scoreboardService;

    public LobbyQuitListener(BackendLanguageService languageService,
                             LobbyScoreboardService scoreboardService) {
        this.languageService = languageService;
        this.scoreboardService = scoreboardService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboardService.remove(event.getPlayer());
        languageService.remove(event.getPlayer().getUniqueId());
    }
}
