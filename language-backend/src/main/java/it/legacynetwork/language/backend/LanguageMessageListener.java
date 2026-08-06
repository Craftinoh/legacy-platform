package it.legacynetwork.language.backend;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.LanguageProtocol;
import it.legacynetwork.language.LanguageProtocolAction;
import it.legacynetwork.language.LanguageProtocolException;
import it.legacynetwork.language.LanguageProtocolMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class LanguageMessageListener implements PluginMessageListener {
    private final LanguageBackendPlugin plugin;
    private final LanguageProtocol protocol;

    public LanguageMessageListener(LanguageBackendPlugin plugin,
                                   LanguageProtocol protocol) {
        this.plugin = plugin;
        this.protocol = protocol;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player carrier,
                                         byte[] message) {
        if (!LanguageProtocol.CHANNEL.equals(channel)) {
            return;
        }
        try {
            LanguageProtocolMessage decoded = protocol.deserialize(message);
            if (decoded.getAction() != LanguageProtocolAction.LANGUAGE_SYNC) {
                return;
            }
            Language language = Language.findByInput(decoded.getLanguageCode())
                    .orElse(Language.ENGLISH);
            String locale = decoded.getLanguageCode();
            plugin.applySynchronizedState(decoded.getPlayerUuid(), language, locale);
        } catch (LanguageProtocolException exception) {
            plugin.getLogger().warning("Payload lingua non valido ricevuto da Velocity: "
                    + exception.getMessage());
        }
    }
}
