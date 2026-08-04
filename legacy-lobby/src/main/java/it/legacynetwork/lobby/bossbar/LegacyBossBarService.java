package it.legacynetwork.lobby.bossbar;

import it.legacynetwork.language.Language;
import it.legacynetwork.lobby.bossbar.packet.BossBarPacketAdapter;
import it.legacynetwork.lobby.bossbar.packet.NmsV1_8R3BossBarPacketAdapter;
import it.legacynetwork.lobby.bossbar.packet.PacketEventsBossBarAdapter;
import it.legacynetwork.lobby.language.BackendLanguageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LegacyBossBarService implements AutoCloseable {
    private static final Logger LOGGER =
            Logger.getLogger(LegacyBossBarService.class.getName());

    private final JavaPlugin plugin;
    private final BackendLanguageService languageService;
    private BossBarTextRenderer textRenderer;
    private BossBarPacketAdapter packetAdapter;
    private final Map<UUID, PlayerBossBarSession> sessions = new ConcurrentHashMap<>();
    private BossBarConfiguration configuration;
    private BukkitTask updateTask;
    private BukkitTask rotationTask;
    private BukkitTask repositionTask;
    private final Random random = new Random();
    private boolean previewActive;
    private String previewBarId;
    private UUID previewPlayerUuid;
    private boolean lifecycleLogged;

    public LegacyBossBarService(JavaPlugin plugin,
                                 BackendLanguageService languageService,
                                 BossBarTextRenderer textRenderer,
                                 BossBarPacketAdapter packetAdapter,
                                 BossBarConfiguration configuration) {
        this.plugin = plugin;
        this.languageService = languageService;
        this.textRenderer = textRenderer;
        this.packetAdapter = packetAdapter;
        this.configuration = configuration;
        applySettings();
    }

    private void applySettings() {
        if (packetAdapter instanceof PacketEventsBossBarAdapter) {
            PacketEventsBossBarAdapter pe = (PacketEventsBossBarAdapter) packetAdapter;
            pe.setDebugPackets(configuration.isDebugPackets());
            pe.setDebugLifecycle(configuration.isDebugLifecycle());
            pe.setDistance(configuration.getWitherDistance());
            pe.setVerticalOffset(configuration.getWitherVerticalOffset());
        } else if (packetAdapter instanceof NmsV1_8R3BossBarPacketAdapter) {
            NmsV1_8R3BossBarPacketAdapter nms = (NmsV1_8R3BossBarPacketAdapter) packetAdapter;
            nms.setDebugPackets(configuration.isDebugPackets());
            nms.setDebugLifecycle(configuration.isDebugLifecycle());
            nms.setDistance(configuration.getWitherDistance());
            nms.setVerticalOffset(configuration.getWitherVerticalOffset());
        }
    }

    public void start() {
        assertMainThread();
        if (!configuration.isEnabled()) {
            logLifecycle("configuration-disabled");
            return;
        }
        cancelTasks();
        updateTask = Bukkit.getScheduler().runTaskTimer(
                plugin, this::updateAll, 0L, configuration.getUpdateTicks());
        if (configuration.isRotationEnabled()) {
            rotationTask = Bukkit.getScheduler().runTaskTimer(
                    plugin, this::rotateAll, 0L,
                    configuration.getRotationIntervalTicks());
        }
        repositionTask = Bukkit.getScheduler().runTaskTimer(
                plugin, this::repositionAll, 10L,
                configuration.getRepositionTicks());
        logLifecycle("configuration-enabled=" + configuration.isEnabled()
                + " definitions=" + getEnabledBars().size());
    }

    public void show(Player player) {
        assertMainThread();
        if (!configuration.isEnabled()) {
            return;
        }
        PlayerBossBarSession session = new PlayerBossBarSession(player, packetAdapter);
        sessions.put(player.getUniqueId(), session);
        logLifecycle("session-created player=" + player.getName());
        rotatePlayer(player);
    }

    public void refresh(Player player) {
        assertMainThread();
        PlayerBossBarSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.isOnline()) {
            return;
        }
        String barId = session.getCurrentBarId();
        if (barId == null) {
            rotatePlayer(player);
            return;
        }
        BossBarDefinition bar = configuration.getBar(barId);
        if (bar == null) {
            rotatePlayer(player);
            return;
        }
        Language language = languageService.get(player.getUniqueId());
        String text = textRenderer.renderText(player, bar, language.getCode());
        double progress = textRenderer.calculateProgress(
                player, bar.getProgress(), session.getTickCount());
        session.updateDisplay(text, progress);
        logLifecycle("refresh player=" + player.getName()
                + " bar=" + barId + " text=" + text
                + " progress=" + String.format("%.2f", progress));
    }

    public void rotatePlayer(Player player) {
        assertMainThread();
        PlayerBossBarSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.isOnline()) {
            return;
        }
        List<BossBarDefinition> enabledBars = getEnabledBars();
        if (enabledBars.isEmpty()) {
            session.destroy();
            logLifecycle("no-enabled-definitions player=" + player.getName());
            return;
        }
        BossBarDefinition next;
        if (previewActive && player.getUniqueId().equals(previewPlayerUuid)
                && previewBarId != null) {
            next = configuration.getBar(previewBarId);
            if (next == null) {
                next = enabledBars.get(0);
            }
        } else {
            next = pickNextBar(session, enabledBars);
        }
        if (next == null) {
            session.destroy();
            return;
        }
        session.reset(next.getId(), next.getDisplayTicks());
        logLifecycle("selected player=" + player.getName() + " bar=" + next.getId());
        refresh(player);
    }

    public void startPreview(Player player, String barId) {
        BossBarDefinition bar = configuration.getBar(barId);
        if (bar == null || !bar.isEnabled()) {
            player.sendMessage("§cBossbar non trovata o disabilitata: " + barId);
            return;
        }
        previewActive = true;
        previewBarId = barId;
        previewPlayerUuid = player.getUniqueId();
        PlayerBossBarSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            session = new PlayerBossBarSession(player, packetAdapter);
            sessions.put(player.getUniqueId(), session);
        }
        session.reset(barId, Math.max(bar.getDisplayTicks(), 100));
        logLifecycle("preview-start player=" + player.getName() + " bar=" + barId);
        refresh(player);
    }

    public void stopPreview() {
        if (previewActive && previewPlayerUuid != null) {
            PlayerBossBarSession session = sessions.get(previewPlayerUuid);
            if (session != null) {
                session.destroy();
            }
        }
        previewActive = false;
        previewBarId = null;
        previewPlayerUuid = null;
        logLifecycle("preview-stop");
    }

    private BossBarDefinition pickNextBar(PlayerBossBarSession session,
                                           List<BossBarDefinition> enabledBars) {
        if (enabledBars.isEmpty()) {
            return null;
        }
        if (enabledBars.size() == 1) {
            return enabledBars.get(0);
        }
        switch (configuration.getRotationMode()) {
            case RANDOM:
                String currentId = session.getCurrentBarId();
                List<BossBarDefinition> choices = new ArrayList<>(enabledBars);
                if (currentId != null && choices.size() > 1) {
                    choices.removeIf(b -> b.getId().equals(currentId));
                }
                return choices.get(random.nextInt(choices.size()));
            case SEQUENTIAL:
            default:
                int currentIndex = -1;
                String curId = session.getCurrentBarId();
                if (curId != null) {
                    for (int i = 0; i < enabledBars.size(); i++) {
                        if (enabledBars.get(i).getId().equals(curId)) {
                            currentIndex = i;
                            break;
                        }
                    }
                }
                int nextIndex = (currentIndex + 1) % enabledBars.size();
                return enabledBars.get(nextIndex);
        }
    }

    public List<BossBarDefinition> getEnabledBars() {
        List<BossBarDefinition> bars = new ArrayList<>();
        for (BossBarDefinition bar : configuration.getBars().values()) {
            if (bar.isEnabled()) {
                bars.add(bar);
            }
        }
        bars.sort(Comparator.comparingInt(BossBarDefinition::getPriority));
        return bars;
    }

    private void updateAll() {
        assertMainThread();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerBossBarSession session = sessions.get(player.getUniqueId());
            if (session == null) {
                continue;
            }
            session.incrementTickCount();
            session.decrementDisplayTicks();
            if (session.getDisplayTicksRemaining() <= 0
                    && configuration.isRotationEnabled()) {
                rotatePlayer(player);
            }
            refresh(player);
        }
    }

    private void rotateAll() {
        assertMainThread();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerBossBarSession session = sessions.get(player.getUniqueId());
            if (session == null) {
                continue;
            }
            rotatePlayer(player);
        }
    }

    private void repositionAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerBossBarSession session = sessions.get(player.getUniqueId());
            if (session == null || !session.isOnline() || !session.isSpawned()) {
                continue;
            }
            com.github.retrooper.packetevents.protocol.world.Location loc =
                    packetAdapter.calculatePosition(player);
            if (loc != null) {
                packetAdapter.updatePosition(player, session.getEntityId(),
                        loc.getX(), loc.getY(), loc.getZ());
            }
        }
    }

    public void remove(Player player) {
        assertMainThread();
        PlayerBossBarSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.destroy();
        }
    }

    public void reload(BossBarTextRenderer newRenderer,
                       BossBarConfiguration newConfiguration) {
        assertMainThread();
        for (PlayerBossBarSession session : sessions.values()) {
            session.destroy();
        }
        sessions.clear();
        this.textRenderer = newRenderer;
        this.configuration = newConfiguration;
        applySettings();
        this.previewActive = false;
        this.previewBarId = null;
        this.previewPlayerUuid = null;
        this.lifecycleLogged = false;
        cancelTasks();
        start();
        for (Player player : Bukkit.getOnlinePlayers()) {
            show(player);
        }
    }

    private void cancelTasks() {
        if (updateTask != null) { updateTask.cancel(); updateTask = null; }
        if (rotationTask != null) { rotationTask.cancel(); rotationTask = null; }
        if (repositionTask != null) { repositionTask.cancel(); repositionTask = null; }
    }

    @Override
    public void close() {
        assertMainThread();
        cancelTasks();
        for (PlayerBossBarSession session : sessions.values()) {
            session.destroy();
        }
        sessions.clear();
    }

    private void logLifecycle(String msg) {
        if (configuration.isDebugLifecycle()) {
            LOGGER.info("BossBar " + msg);
        }
    }

    private void assertMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "Le bossbar devono essere gestite nel main thread");
        }
    }
}
