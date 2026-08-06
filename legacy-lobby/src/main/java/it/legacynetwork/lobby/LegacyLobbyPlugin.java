package it.legacynetwork.lobby;

import it.legacynetwork.language.LanguageProtocol;
import it.legacynetwork.language.PlayerLanguageEventService;
import it.legacynetwork.language.PlayerLanguageProvider;
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
import it.legacynetwork.lobby.language.BukkitPlayerLanguageEventService;
import it.legacynetwork.lobby.language.LanguagePluginMessageListener;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class LegacyLobbyPlugin extends JavaPlugin {
    private BackendLanguageService languageService;
    private LobbyScoreboardService scoreboardService;
    private LegacyBossBarService bossBarService;
    private BukkitPlayerLanguageEventService eventService;
    private PlaceholderService placeholderService;
    private BossBarPacketAdapter packetAdapter;
    private MessageService messageService;
    private LobbyConfiguration configuration;
    private ScoreboardConfiguration scoreboardConfiguration;
    private BossBarConfiguration bossBarConfiguration;
    private VoidTeleportService voidTeleportService;
    private String channel;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("scoreboard.yml", false);
        saveResource("messages_it.yml", false);
        saveResource("messages_en.yml", false);
        saveResource("bossbar.yml", false);

        try {
            configuration = LobbyConfiguration.from(getConfig());
            placeholderService = initPlaceholderAPI();
            packetAdapter = initBossBarAdapter();
            loadConfigurations();
            messageService = new MessageService(
                    new File(getDataFolder(), configuration.getMessagesItalianFile()),
                    placeholderService);
            messageService.load();

            languageService = new BackendLanguageService();
            getServer().getServicesManager().register(
                    PlayerLanguageProvider.class, languageService, this,
                    org.bukkit.plugin.ServicePriority.Normal);

            eventService = new BukkitPlayerLanguageEventService();
            getServer().getServicesManager().register(
                    PlayerLanguageEventService.class, eventService, this,
                    org.bukkit.plugin.ServicePriority.Normal);

            LobbyScoreboardRenderer scoreboardRenderer = new LobbyScoreboardRenderer(
                    scoreboardConfiguration, placeholderService,
                    configuration.getServerId(), "play.apteris.net");
            scoreboardService = new LobbyScoreboardService(
                    this, languageService, scoreboardRenderer, scoreboardConfiguration);

            BossBarTextRenderer bossBarTextRenderer = new BossBarTextRenderer(
                    bossBarConfiguration, placeholderService, configuration.getServerId());
            bossBarService = new LegacyBossBarService(
                    this, languageService, bossBarTextRenderer,
                    packetAdapter, bossBarConfiguration);

            channel = configuration.getLanguageChannel();
            LanguagePluginMessageListener messageListener =
                    new LanguagePluginMessageListener(
                            this, channel, new LanguageProtocol(),
                            languageService, scoreboardService,
                            bossBarService, eventService);
            getServer().getMessenger().registerIncomingPluginChannel(
                    this, channel, messageListener);
            getServer().getMessenger().registerOutgoingPluginChannel(this, channel);

            getServer().getPluginManager().registerEvents(
                    new LobbyJoinListener(this, configuration, languageService,
                            messageService, scoreboardService, bossBarService),
                    this);
            getServer().getPluginManager().registerEvents(
                    new LobbyQuitListener(languageService, scoreboardService, bossBarService),
                    this);

            getCommand("legacylobby").setExecutor(
                    new LegacyLobbyCommand(configuration, bossBarService, this::reloadAll));

            scoreboardService.start();
            bossBarService.start();

            voidTeleportService = new VoidTeleportService(this);
            voidTeleportService.configure(
                    configuration.isVoidTeleportEnabled(),
                    configuration.getVoidTeleportBelowY(),
                    configuration.getVoidTeleportTarget(),
                    configuration.getVoidTeleportFallback(),
                    configuration.getVoidTeleportCheckTicks());

            getLogger().info("LegacyLobby inizializzato.");
            getLogger().info("PlaceholderAPI: "
                    + (placeholderService.isAvailable() ? "integrata" : "non disponibile"));
            getLogger().info("Bossbar adapter: "
                    + (packetAdapter instanceof NmsV1_8R3BossBarPacketAdapter
                    ? "NMS v1_8_R3" : "non disponibile"));
        } catch (RuntimeException exception) {
            getLogger().severe(
                    "Impossibile inizializzare LegacyLobby: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void loadConfigurations() {
        File dataFolder = getDataFolder();
        File scoreboardFile = new File(dataFolder, configuration.getScoreboardConfigFile());
        scoreboardConfiguration = ScoreboardConfiguration.load(scoreboardFile);
        File bossbarFile = new File(dataFolder, configuration.getBossbarConfigFile());
        bossBarConfiguration = BossBarConfiguration.load(bossbarFile);
    }

    private void reloadAll() {
        reloadConfig();
        configuration = LobbyConfiguration.from(getConfig());
        loadConfigurations();

        MessageService newMessageService = new MessageService(
                new File(getDataFolder(), configuration.getMessagesItalianFile()),
                placeholderService);
        newMessageService.load();
        this.messageService = newMessageService;

        LobbyScoreboardRenderer newScoreboardRenderer = new LobbyScoreboardRenderer(
                scoreboardConfiguration, placeholderService,
                configuration.getServerId(), "play.apteris.net");
        scoreboardService.reload(newScoreboardRenderer, scoreboardConfiguration);

        BossBarTextRenderer newBossBarTextRenderer = new BossBarTextRenderer(
                bossBarConfiguration, placeholderService, configuration.getServerId());
        bossBarService.reload(newBossBarTextRenderer, bossBarConfiguration);

        if (voidTeleportService != null) {
            voidTeleportService.configure(
                    configuration.isVoidTeleportEnabled(),
                    configuration.getVoidTeleportBelowY(),
                    configuration.getVoidTeleportTarget(),
                    configuration.getVoidTeleportFallback(),
                    configuration.getVoidTeleportCheckTicks());
        }

        getLogger().info("LegacyLobby reload completato.");
    }

    private PlaceholderService initPlaceholderAPI() {
        org.bukkit.plugin.Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi != null && papi.isEnabled()) {
            getLogger().info("PlaceholderAPI rilevata, integrazione attiva.");
            return new PlaceholderApiService();
        }
        getLogger().warning("PlaceholderAPI non trovata o disabilitata, "
                + "i placeholder esterni non saranno risolti.");
        return new NoopPlaceholderService();
    }

    private BossBarPacketAdapter initBossBarAdapter() {
        if (!NmsV1_8R3BossBarPacketAdapter.validateServerVersion()) {
            getLogger().warning("Server non v1_8_R3: bossbar legacy disabilitata.");
            return new NoopBossBarPacketAdapter();
        }
        try {
            getLogger().info("Server v1_8_R3 rilevato, bossbar NMS attiva.");
            return new NmsV1_8R3BossBarPacketAdapter();
        } catch (LinkageError | RuntimeException exception) {
            getLogger().warning("Impossibile inizializzare la bossbar NMS: "
                    + exception.getMessage());
            return new NoopBossBarPacketAdapter();
        }
    }

    @Override
    public void onDisable() {
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
        if (channel != null) {
            getServer().getMessenger().unregisterIncomingPluginChannel(this, channel);
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, channel);
        }
    }
}
