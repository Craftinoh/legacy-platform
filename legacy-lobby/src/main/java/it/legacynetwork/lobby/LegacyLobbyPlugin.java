package it.legacynetwork.lobby;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageChangeListener;
import it.legacynetwork.language.PlayerLanguageEventService;
import it.legacynetwork.language.PlayerLanguageProvider;
import it.legacynetwork.language.TranslationInstaller;
import it.legacynetwork.lobby.bossbar.BossBarConfiguration;
import it.legacynetwork.lobby.bossbar.BossBarTextRenderer;
import it.legacynetwork.lobby.bossbar.LegacyBossBarService;
import it.legacynetwork.lobby.bossbar.packet.BossBarPacketAdapter;
import it.legacynetwork.lobby.bossbar.packet.NmsV1_8R3BossBarPacketAdapter;
import it.legacynetwork.lobby.bossbar.packet.NoopBossBarPacketAdapter;
import it.legacynetwork.lobby.command.LegacyLobbyCommand;
import it.legacynetwork.lobby.config.LobbyConfiguration;
import it.legacynetwork.lobby.config.ScoreboardConfiguration;
import it.legacynetwork.lobby.language.BackendLanguageService;
import it.legacynetwork.lobby.listener.LobbyJoinListener;
import it.legacynetwork.lobby.listener.LobbyQuitListener;
import it.legacynetwork.lobby.listener.VoidTeleportService;
import it.legacynetwork.lobby.message.MessageService;
import it.legacynetwork.lobby.placeholder.NoopPlaceholderService;
import it.legacynetwork.lobby.placeholder.PlaceholderApiService;
import it.legacynetwork.lobby.placeholder.PlaceholderService;
import it.legacynetwork.lobby.scoreboard.LobbyScoreboardRenderer;
import it.legacynetwork.lobby.scoreboard.LobbyScoreboardService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;
import java.util.function.Supplier;

public final class LegacyLobbyPlugin extends JavaPlugin
        implements PlayerLanguageChangeListener {
    private BackendLanguageService languageService;
    private PlayerLanguageProvider languageProvider;
    private PlayerLanguageEventService languageEventService;
    private LobbyScoreboardService scoreboardService;
    private LegacyBossBarService bossBarService;
    private PlaceholderService placeholderService;
    private BossBarPacketAdapter packetAdapter;
    private MessageService messageService;
    private LobbyConfiguration configuration;
    private ScoreboardConfiguration scoreboardConfiguration;
    private BossBarConfiguration bossBarConfiguration;
    private VoidTeleportService voidTeleportService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("scoreboard.yml", false);
        saveResource("bossbar.yml", false);
        int installed = TranslationInstaller.install(getDataFolder(),
                "translations", getLogger(), getClassLoader());
        getLogger().info("Traduzioni installate: " + installed + " file.");

        try {
            configuration = LobbyConfiguration.from(getConfig());
            placeholderService = initPlaceholderAPI();
            packetAdapter = initBossBarAdapter();
            loadConfigurations();

            languageProvider = Bukkit.getServicesManager()
                    .load(PlayerLanguageProvider.class);
            languageEventService = Bukkit.getServicesManager()
                    .load(PlayerLanguageEventService.class);
            if (languageProvider == null || languageEventService == null) {
                throw new IllegalStateException(
                        "LanguageBackend services non disponibili");
            }

            languageService = new BackendLanguageService(
                    languageProvider, configuration.getLanguageFallback());
            messageService = createMessageService();

            LobbyScoreboardRenderer scoreboardRenderer =
                    new LobbyScoreboardRenderer(
                            scoreboardConfiguration, placeholderService,
                            configuration.getServerId(), "play.apteris.net");
            scoreboardService = new LobbyScoreboardService(
                    this, languageService, scoreboardRenderer,
                    scoreboardConfiguration);

            BossBarTextRenderer bossBarTextRenderer = new BossBarTextRenderer(
                    bossBarConfiguration, placeholderService,
                    configuration.getServerId());
            bossBarService = new LegacyBossBarService(
                    this, languageService, bossBarTextRenderer,
                    packetAdapter, bossBarConfiguration);

            languageEventService.registerListener(this);

            getServer().getPluginManager().registerEvents(
                    new LobbyJoinListener(
                            this,
                            new Supplier<LobbyConfiguration>() {
                                @Override
                                public LobbyConfiguration get() {
                                    return configuration;
                                }
                            },
                            languageService,
                            new Supplier<MessageService>() {
                                @Override
                                public MessageService get() {
                                    return messageService;
                                }
                            },
                            scoreboardService,
                            bossBarService),
                    this);
            getServer().getPluginManager().registerEvents(
                    new LobbyQuitListener(languageService,
                            scoreboardService, bossBarService),
                    this);

            getCommand("legacylobby").setExecutor(
                    new LegacyLobbyCommand(this, bossBarService,
                            this::reloadAll));

            scoreboardService.start();
            bossBarService.start();

            voidTeleportService = new VoidTeleportService(this);
            configureVoidTeleport();

            getLogger().info("LegacyLobby inizializzato con LanguageBackend.");
            getLogger().info("PlaceholderAPI: "
                    + (placeholderService.isAvailable()
                    ? "integrata" : "non disponibile"));
            getLogger().info("Bossbar adapter: "
                    + (packetAdapter instanceof NmsV1_8R3BossBarPacketAdapter
                    ? "NMS v1_8_R3" : "non disponibile"));
        } catch (RuntimeException exception) {
            getLogger().severe(
                    "Impossibile inizializzare LegacyLobby: "
                            + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private MessageService createMessageService() {
        MessageService service = new MessageService(
                new File(getDataFolder(), "translations"),
                placeholderService,
                languageProvider,
                configuration.getLanguageFallback());
        service.load();
        return service;
    }

    private void loadConfigurations() {
        File dataFolder = getDataFolder();
        File scoreboardFile = new File(dataFolder,
                configuration.getScoreboardConfigFile());
        scoreboardConfiguration = ScoreboardConfiguration.load(scoreboardFile);
        File bossbarFile = new File(dataFolder,
                configuration.getBossbarConfigFile());
        bossBarConfiguration = BossBarConfiguration.load(bossbarFile);
    }

    private void reloadAll() {
        reloadConfig();
        configuration = LobbyConfiguration.from(getConfig());
        loadConfigurations();
        languageService = new BackendLanguageService(
                languageProvider, configuration.getLanguageFallback());
        messageService = createMessageService();

        LobbyScoreboardRenderer newScoreboardRenderer =
                new LobbyScoreboardRenderer(
                        scoreboardConfiguration, placeholderService,
                        configuration.getServerId(), "play.apteris.net");
        scoreboardService.reload(newScoreboardRenderer,
                scoreboardConfiguration);

        BossBarTextRenderer newBossBarTextRenderer = new BossBarTextRenderer(
                bossBarConfiguration, placeholderService,
                configuration.getServerId());
        bossBarService.reload(newBossBarTextRenderer,
                bossBarConfiguration);

        configureVoidTeleport();
        getLogger().info("LegacyLobby reload completato.");
    }

    @Override
    public void onLanguageChanged(final UUID playerId,
                                  Language previousLanguage,
                                  Language newLanguage) {
        Runnable refresh = new Runnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    return;
                }
                if (scoreboardService != null) {
                    scoreboardService.refresh(player);
                }
                if (bossBarService != null) {
                    bossBarService.refresh(player);
                }
                if (messageService != null) {
                    messageService.send(player, "language-change");
                }
            }
        };
        if (Bukkit.isPrimaryThread()) {
            refresh.run();
        } else {
            Bukkit.getScheduler().runTask(this, refresh);
        }
    }

    private void configureVoidTeleport() {
        if (voidTeleportService == null) {
            return;
        }
        voidTeleportService.configure(
                configuration.isVoidTeleportEnabled(),
                configuration.isAuthmeIntegration(),
                configuration.getVoidTeleportBelowY(),
                configuration.getVoidTeleportTarget(),
                configuration.getVoidTeleportFallback(),
                configuration.getVoidTeleportCheckTicks());
    }

    private PlaceholderService initPlaceholderAPI() {
        org.bukkit.plugin.Plugin papi =
                Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi != null && papi.isEnabled()) {
            getLogger().info(
                    "PlaceholderAPI rilevata, integrazione attiva.");
            return new PlaceholderApiService();
        }
        getLogger().warning("PlaceholderAPI non trovata o disabilitata, "
                + "i placeholder esterni non saranno risolti.");
        return new NoopPlaceholderService();
    }

    private BossBarPacketAdapter initBossBarAdapter() {
        if (!NmsV1_8R3BossBarPacketAdapter.validateServerVersion()) {
            getLogger().warning(
                    "Server non v1_8_R3: bossbar legacy disabilitata.");
            return new NoopBossBarPacketAdapter();
        }
        try {
            getLogger().info(
                    "Server v1_8_R3 rilevato, bossbar NMS attiva.");
            return new NmsV1_8R3BossBarPacketAdapter();
        } catch (LinkageError | RuntimeException exception) {
            getLogger().warning("Impossibile inizializzare la bossbar NMS: "
                    + exception.getMessage());
            return new NoopBossBarPacketAdapter();
        }
    }

    @Override
    public void onDisable() {
        if (languageEventService != null) {
            languageEventService.unregisterListener(this);
        }
        if (voidTeleportService != null) {
            voidTeleportService.stop();
        }
        if (bossBarService != null) {
            bossBarService.close();
        }
        if (scoreboardService != null) {
            scoreboardService.close();
        }
        if (languageService != null) {
            languageService.clear();
        }
        languageProvider = null;
        languageEventService = null;
    }
}
