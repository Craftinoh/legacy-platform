package it.legacynetwork.lobby.scoreboard;

import it.legacynetwork.language.Language;
import it.legacynetwork.lobby.config.LobbyConfiguration;
import it.legacynetwork.lobby.language.BackendLanguageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LobbyScoreboardService implements AutoCloseable {
    private final JavaPlugin plugin;
    private final LobbyConfiguration configuration;
    private final BackendLanguageService languageService;
    private final LobbyScoreboardRenderer renderer;
    private final Map<UUID, PlayerScoreboard> scoreboards =
            new HashMap<UUID, PlayerScoreboard>();
    private BukkitTask updateTask;

    public LobbyScoreboardService(JavaPlugin plugin,
                                  LobbyConfiguration configuration,
                                  BackendLanguageService languageService,
                                  LobbyScoreboardRenderer renderer) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.languageService = languageService;
        this.renderer = renderer;
    }

    public void start() {
        assertMainThread();
        if (!configuration.isScoreboardEnabled()) {
            return;
        }
        updateTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                new Runnable() {
                    @Override
                    public void run() {
                        refreshAll();
                    }
                },
                configuration.getScoreboardUpdateTicks(),
                configuration.getScoreboardUpdateTicks());
    }

    public void show(Player player) {
        assertMainThread();
        if (!configuration.isScoreboardEnabled()) {
            return;
        }
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        PlayerScoreboard playerScoreboard = new PlayerScoreboard(
                scoreboard, renderer.title(languageService.get(player.getUniqueId())));
        scoreboards.put(player.getUniqueId(), playerScoreboard);
        player.setScoreboard(scoreboard);
        refresh(player);
    }

    public void refresh(Player player) {
        assertMainThread();
        PlayerScoreboard scoreboard = scoreboards.get(player.getUniqueId());
        if (scoreboard == null || !player.isOnline()) {
            return;
        }
        Language language = languageService.get(player.getUniqueId());
        scoreboard.update(
                renderer.title(language),
                renderer.render(language, Bukkit.getOnlinePlayers().size()));
    }

    public void refreshAll() {
        assertMainThread();
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void remove(Player player) {
        assertMainThread();
        scoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    @Override
    public void close() {
        assertMainThread();
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        scoreboards.clear();
    }

    private void assertMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "Le scoreboard Bukkit devono essere gestite nel main thread");
        }
    }
}
