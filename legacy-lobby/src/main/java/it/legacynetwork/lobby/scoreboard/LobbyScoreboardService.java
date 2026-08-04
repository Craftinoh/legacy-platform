package it.legacynetwork.lobby.scoreboard;

import it.legacynetwork.language.Language;
import it.legacynetwork.lobby.config.ScoreboardConfiguration;
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
    private final BackendLanguageService languageService;
    private final Map<UUID, PlayerScoreboard> scoreboards = new HashMap<>();
    private LobbyScoreboardRenderer renderer;
    private ScoreboardConfiguration configuration;
    private BukkitTask updateTask;

    public LobbyScoreboardService(JavaPlugin plugin,
                                  BackendLanguageService languageService,
                                  LobbyScoreboardRenderer renderer,
                                  ScoreboardConfiguration configuration) {
        this.plugin = plugin;
        this.languageService = languageService;
        this.renderer = renderer;
        this.configuration = configuration;
    }

    public void start() {
        assertMainThread();
        if (!configuration.isEnabled()) {
            return;
        }
        if (updateTask != null) {
            updateTask.cancel();
        }
        updateTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::refreshAll,
                configuration.getUpdateTicks(),
                configuration.getUpdateTicks());
    }

    public void show(Player player) {
        assertMainThread();
        if (!configuration.isEnabled()) {
            return;
        }
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Language language = languageService.get(player.getUniqueId());
        PlayerScoreboard playerScoreboard = new PlayerScoreboard(
                scoreboard, renderer.title(player, language));
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
                renderer.title(player, language),
                renderer.render(player, language, Bukkit.getOnlinePlayers().size()));
    }

    public void refreshAll() {
        assertMainThread();
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void reload(LobbyScoreboardRenderer newRenderer,
                       ScoreboardConfiguration newConfiguration) {
        assertMainThread();
        this.renderer = newRenderer;
        this.configuration = newConfiguration;
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        start();
        for (Player player : Bukkit.getOnlinePlayers()) {
            remove(player);
            show(player);
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
