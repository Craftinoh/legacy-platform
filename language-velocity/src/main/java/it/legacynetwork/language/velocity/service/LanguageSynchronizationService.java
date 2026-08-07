package it.legacynetwork.language.velocity.service;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import it.legacynetwork.language.Language;
import it.legacynetwork.language.LanguageChangeResult;
import it.legacynetwork.language.LanguageProtocol;
import it.legacynetwork.language.LanguageProtocolException;
import it.legacynetwork.language.LanguageProtocolMessage;
import org.slf4j.Logger;

public final class LanguageSynchronizationService {
    private final LegacyChannelIdentifier channel;
    private final LanguageProtocol protocol;
    private final ProxyLanguageService languageService;
    private final Logger logger;

    public LanguageSynchronizationService(
            LegacyChannelIdentifier channel,
            LanguageProtocol protocol,
            ProxyLanguageService languageService,
            Logger logger) {
        this.channel = channel;
        this.protocol = protocol;
        this.languageService = languageService;
        this.logger = logger;
    }

    public boolean synchronize(Player player) {
        return player.getCurrentServer().map(serverConnection -> {
            ProxyLanguageService.PlayerLanguage state =
                    languageService.current(player);
            try {
                byte[] payload = protocol.serialize(
                        LanguageProtocolMessage.languageSync(
                                player.getUniqueId(),
                                state.language(),
                                state.preference()
                                        .overridesClientLocale()));
                return serverConnection.sendPluginMessage(
                        channel, payload);
            } catch (LanguageProtocolException exception) {
                logger.error(
                        "Impossibile sincronizzare la lingua di {}",
                        player.getUsername(), exception);
                return false;
            }
        }).orElse(false);
    }

    public boolean sendChangeResult(Player player,
                                    Language requestedLanguage,
                                    LanguageChangeResult result) {
        return player.getCurrentServer().map(serverConnection -> {
            try {
                byte[] payload = protocol.serialize(
                        LanguageProtocolMessage.languageChangeResult(
                                player.getUniqueId(),
                                requestedLanguage,
                                result));
                return serverConnection.sendPluginMessage(
                        channel, payload);
            } catch (LanguageProtocolException exception) {
                logger.error(
                        "Impossibile inviare l'esito del cambio lingua di {}",
                        player.getUsername(), exception);
                return false;
            }
        }).orElse(false);
    }
}
