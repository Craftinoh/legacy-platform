package it.legacynetwork.lobby.listener;

import it.legacynetwork.lobby.language.BackendLanguageService;
import it.legacynetwork.lobby.bossbar.LegacyBossBarService;
import it.legacynetwork.lobby.scoreboard.LobbyScoreboardService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class LobbyQuitListener implements Listener {
    private final BackendLanguageService languageService;
    private final LobbyScoreboardService scoreboardService;
    private final LegacyBossBarService bossBarService;

    public LobbyQuitListener(BackendLanguageService languageService,
                             LobbyScoreboardService scoreboardService,
                             LegacyBossBarService bossBarService) {
        this.languageService = languageService;
        this.scoreboardService = scoreboardService;
        this.bossBarService = bossBarService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        bossBarService.remove(event.getPlayer());
        scoreboardService.remove(event.getPlayer());
        languageService.remove(event.getPlayer().getUniqueId());
    }
}
