package it.legacynetwork.language.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
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
import it.legacynetwork.language.velocity.tablist.VelocityTabListService;
import it.legacynetwork.language.velocity.translation.PropertiesTranslationLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private VelocityTabListService tabListService;
    private ScheduledExecutorService tabScheduler;
    private ProxyLanguageService languageService;
    private LanguageSynchronizationService synchronizationService;
    private FileLanguageRepository repository;

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
            repository = new FileLanguageRepository(dataDirectory);
            repository.load().join();

            languageService = new ProxyLanguageService(
                    repository, new LocaleLanguageResolver());
            LegacyChannelIdentifier channel =
                    new LegacyChannelIdentifier("NetworkLang");
            synchronizationService = new LanguageSynchronizationService(
                    channel, new LanguageProtocol(), languageService, logger);

            proxy.getChannelRegistrar().register(channel);
            registerCommand(translations);
            proxy.getEventManager().register(this,
                    new PlayerConnectedListener(languageService));
            proxy.getEventManager().register(this,
                    new PlayerSettingsListener(languageService, synchronizationService));
            proxy.getEventManager().register(this,
                    new ServerPostConnectListener(synchronizationService));
            proxy.getEventManager().register(this,
                    new ProxyShutdownListener(repository));

            saveResource("tablist.yml");
            tabScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "networklang-tab");
                t.setDaemon(true);
                return t;
            });
            tabListService = new VelocityTabListService(
                    proxy, logger, dataDirectory, tabScheduler);
            tabListService.setLanguageResolver(
                    p -> languageService.current(p).language());
            tabListService.load();

            registerTabListeners();

            logger.info("NetworkLanguage inizializzato.");
        } catch (IOException | RuntimeException exception) {
            logger.error("Impossibile inizializzare NetworkLanguage", exception);
            throw new IllegalStateException(
                    "NetworkLanguage initialization failed", exception);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (tabListService != null) {
            tabListService.close();
        }
        if (tabScheduler != null) {
            tabScheduler.shutdown();
            try {
                tabScheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void registerTabListeners() {
        proxy.getEventManager().register(this, new Object() {
            @Subscribe
            public void onLogin(PostLoginEvent event) {
                if (tabListService != null) {
                    tabListService.sendScheduled(event.getPlayer(), true);
                }
            }

            @Subscribe
            public void onServerConnect(ServerPostConnectEvent event) {
                if (tabListService != null) {
                    tabListService.sendScheduled(event.getPlayer(), true);
                }
            }

            @Subscribe
            public void onDisconnect(DisconnectEvent event) {
                if (tabListService != null) {
                    tabListService.clear(event.getPlayer());
                }
            }

            @Subscribe
            public void onSettingsChanged(PlayerSettingsChangedEvent event) {
                if (tabListService != null) {
                    tabListService.sendScheduled(event.getPlayer(), true);
                }
            }
        });
    }

    private void registerCommand(TranslationService translations) {
        CommandMeta langMeta = proxy.getCommandManager()
                .metaBuilder("lang")
                .plugin(this)
                .build();
        proxy.getCommandManager().register(langMeta,
                new LanguageCommand(languageService, synchronizationService,
                        translations, logger, tabListService));

        CommandMeta reloadMeta = proxy.getCommandManager()
                .metaBuilder("networklang")
                .plugin(this)
                .build();
        proxy.getCommandManager().register(reloadMeta,
                new NetworkLangReloadCommand(this));
    }

    public void reloadAll() {
        repository.load().join();
        if (tabListService != null) {
            tabListService.reload();
        }
        logger.info("NetworkLanguage reload completato.");
    }

    public VelocityTabListService getTabListService() {
        return tabListService;
    }

    void saveResource(String resource) throws IOException {
        Path target = dataDirectory.resolve(resource);
        if (Files.exists(target)) {
            return;
        }
        Files.createDirectories(target.getParent());
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream(resource)) {
            if (in == null) {
                return;
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
