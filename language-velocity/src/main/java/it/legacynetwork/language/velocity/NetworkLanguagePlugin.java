package it.legacynetwork.language.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import it.legacynetwork.language.LanguageProtocol;
import it.legacynetwork.language.LocaleLanguageResolver;
import it.legacynetwork.language.TranslationService;
import it.legacynetwork.language.velocity.command.LanguageCommand;
import it.legacynetwork.language.velocity.listener.PlayerConnectedListener;
import it.legacynetwork.language.velocity.listener.PlayerSettingsListener;
import it.legacynetwork.language.velocity.listener.ProxyShutdownListener;
import it.legacynetwork.language.velocity.listener.ServerPostConnectListener;
import it.legacynetwork.language.velocity.repository.FileLanguageRepository;
import it.legacynetwork.language.velocity.service.LanguageSynchronizationService;
import it.legacynetwork.language.velocity.service.ProxyLanguageService;
import it.legacynetwork.language.velocity.translation.PropertiesTranslationLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = "networklanguage",
        name = "NetworkLanguage",
        version = "0.1.0-SNAPSHOT",
        authors = {"LegacyNetwork"}
)
public final class NetworkLanguagePlugin {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public NetworkLanguagePlugin(ProxyServer proxy,
                                 Logger logger,
                                 @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        try {
            TranslationService translations = new PropertiesTranslationLoader(
                    getClass().getClassLoader()).load();
            FileLanguageRepository repository =
                    new FileLanguageRepository(dataDirectory);
            repository.load().join();

            ProxyLanguageService languageService = new ProxyLanguageService(
                    repository, new LocaleLanguageResolver());
            LegacyChannelIdentifier channel =
                    new LegacyChannelIdentifier("NetworkLang");
            LanguageSynchronizationService synchronizationService =
                    new LanguageSynchronizationService(
                            channel,
                            new LanguageProtocol(),
                            languageService,
                            logger);

            proxy.getChannelRegistrar().register(channel);
            registerCommand(languageService, synchronizationService, translations);
            proxy.getEventManager().register(this,
                    new PlayerConnectedListener(languageService));
            proxy.getEventManager().register(this,
                    new PlayerSettingsListener(languageService, synchronizationService));
            proxy.getEventManager().register(this,
                    new ServerPostConnectListener(synchronizationService));
            proxy.getEventManager().register(this,
                    new ProxyShutdownListener(repository));
            logger.info("NetworkLanguage inizializzato.");
        } catch (IOException | RuntimeException exception) {
            logger.error("Impossibile inizializzare NetworkLanguage", exception);
            throw new IllegalStateException("NetworkLanguage initialization failed", exception);
        }
    }

    private void registerCommand(ProxyLanguageService languageService,
                                 LanguageSynchronizationService synchronizationService,
                                 TranslationService translations) {
        CommandMeta commandMeta = proxy.getCommandManager()
                .metaBuilder("lang")
                .plugin(this)
                .build();
        proxy.getCommandManager().register(commandMeta,
                new LanguageCommand(
                        languageService,
                        synchronizationService,
                        translations,
                        logger));
    }
}
