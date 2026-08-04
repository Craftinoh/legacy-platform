package it.legacynetwork.lobby.language;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.LanguageProtocol;
import it.legacynetwork.language.LanguageProtocolAction;
import it.legacynetwork.language.LanguageProtocolException;
import it.legacynetwork.language.LanguageProtocolMessage;
import it.legacynetwork.lobby.scoreboard.LobbyScoreboardService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Optional;

public final class LanguagePluginMessageListener implements PluginMessageListener {
    private final JavaPlugin plugin;
    private final String channel;
    private final LanguageProtocol protocol;
    private final BackendLanguageService languageService;
    private final LobbyScoreboardService scoreboardService;

    public LanguagePluginMessageListener(JavaPlugin plugin,
                                         String channel,
                                         LanguageProtocol protocol,
                                         BackendLanguageService languageService,
                                         LobbyScoreboardService scoreboardService) {
        this.plugin = plugin;
        this.channel = channel;
        this.protocol = protocol;
        this.languageService = languageService;
        this.scoreboardService = scoreboardService;
    }

    @Override
    public void onPluginMessageReceived(String incomingChannel,
                                        Player carrier,
                                        byte[] payload) {
        if (!channel.equals(incomingChannel)) {
            return;
        }
        try {
            LanguageProtocolMessage message = protocol.deserialize(payload);
            if (message.getAction() != LanguageProtocolAction.LANGUAGE_SYNC
                    || !message.getPlayerUuid().equals(carrier.getUniqueId())) {
                return;
            }
            final Player player = Bukkit.getPlayer(message.getPlayerUuid());
            if (player == null || !player.isOnline()
                    || !player.getUniqueId().equals(message.getPlayerUuid())) {
                return;
            }
            Optional<Language> language =
                    Language.findByInput(message.getLanguageCode());
            if (!language.isPresent()) {
                return;
            }
            final Language selectedLanguage = language.get();
            Runnable update = new Runnable() {
                @Override
                public void run() {
                    languageService.update(player.getUniqueId(), selectedLanguage);
                    scoreboardService.refresh(player);
                }
            };
            if (Bukkit.isPrimaryThread()) {
                update.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, update);
            }
        } catch (LanguageProtocolException exception) {
            plugin.getLogger().warning(
                    "Payload NetworkLang ignorato: " + exception.getMessage());
        }
    }
}
