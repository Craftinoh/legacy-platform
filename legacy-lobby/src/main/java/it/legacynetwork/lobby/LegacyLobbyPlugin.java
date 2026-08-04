package it.legacynetwork.lobby;

import it.legacynetwork.language.LanguageProtocol;
import it.legacynetwork.language.TranslationService;
import it.legacynetwork.lobby.config.LobbyConfiguration;
import it.legacynetwork.lobby.language.BackendLanguageService;
import it.legacynetwork.lobby.language.LanguagePluginMessageListener;
import it.legacynetwork.lobby.listener.LobbyJoinListener;
import it.legacynetwork.lobby.listener.LobbyQuitListener;
import it.legacynetwork.lobby.scoreboard.LobbyScoreboardRenderer;
import it.legacynetwork.lobby.scoreboard.LobbyScoreboardService;
import it.legacynetwork.lobby.translation.PropertiesTranslationLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class LegacyLobbyPlugin extends JavaPlugin {
    private BackendLanguageService languageService;
    private LobbyScoreboardService scoreboardService;
    private String channel;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("translations/lobby_it.properties", false);
        saveResource("translations/lobby_en.properties", false);
        try {
            LobbyConfiguration configuration =
                    LobbyConfiguration.from(getConfig());
            TranslationService translations =
                    new PropertiesTranslationLoader(this).load();
            languageService = new BackendLanguageService();
            LobbyScoreboardRenderer renderer =
                    new LobbyScoreboardRenderer(translations, configuration);
            scoreboardService = new LobbyScoreboardService(
                    this, configuration, languageService, renderer);
            channel = configuration.getLanguageChannel();

            LanguagePluginMessageListener messageListener =
                    new LanguagePluginMessageListener(
                            this,
                            channel,
                            new LanguageProtocol(),
                            languageService,
                            scoreboardService);
            getServer().getMessenger().registerIncomingPluginChannel(
                    this, channel, messageListener);
            getServer().getMessenger().registerOutgoingPluginChannel(this, channel);
            getServer().getPluginManager().registerEvents(
                    new LobbyJoinListener(
                            configuration,
                            languageService,
                            translations,
                            scoreboardService),
                    this);
            getServer().getPluginManager().registerEvents(
                    new LobbyQuitListener(languageService, scoreboardService),
                    this);
            scoreboardService.start();
            getLogger().info("LegacyLobby inizializzato.");
        } catch (IOException | RuntimeException exception) {
            getLogger().severe(
                    "Impossibile inizializzare LegacyLobby: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (scoreboardService != null) {
            scoreboardService.close();
        }
        if (languageService != null) {
            languageService.clear();
        }
        if (channel != null) {
            getServer().getMessenger().unregisterIncomingPluginChannel(this, channel);
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, channel);
        }
    }
}
