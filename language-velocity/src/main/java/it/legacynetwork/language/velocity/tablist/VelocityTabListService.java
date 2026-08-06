package it.legacynetwork.language.velocity.tablist;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import it.legacynetwork.language.Language;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class VelocityTabListService {
    private static final int MIN_PROTOCOL = 47;
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final ScheduledExecutorService scheduler;
    private final LegacyComponentSerializer serializer;
    private TabListConfiguration configuration;
    private final Map<UUID, String> lastSentHeader = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSentFooter = new ConcurrentHashMap<>();
    private Function<Player, Language> languageResolver;
    private Object localizedPrefixProvider;

    public VelocityTabListService(ProxyServer proxy,
                                   Logger logger,
                                   Path dataDirectory,
                                   ScheduledExecutorService scheduler) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.scheduler = scheduler;
        this.serializer = LegacyComponentSerializer.legacyAmpersand();
    }

    public void setLanguageResolver(Function<Player, Language> resolver) {
        this.languageResolver = resolver;
    }

    public void setLocalizedPrefixProvider(Object provider) {
        this.localizedPrefixProvider = provider;
    }

    public void load() {
        File tabFile = new File(dataDirectory.toFile(), "tablist.yml");
        this.configuration = TabListConfigurationLoader.load(tabFile);
        logDebug("Tab configuration-enabled=" + configuration.isEnabled());

        if (!configuration.isEnabled()) {
            clearAll();
            return;
        }
        startUpdateTask();
    }

    private void startUpdateTask() {
        int intervalMs = Math.max(100, configuration.getUpdateTicks() * 50);
        scheduler.scheduleAtFixedRate(this::updateAll,
                intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void reload() {
        load();
        lastSentHeader.clear();
        lastSentFooter.clear();
        for (Player player : proxy.getAllPlayers()) {
            sendScheduled(player, true);
        }
    }

    public void close() {
        clearAll();
        lastSentHeader.clear();
        lastSentFooter.clear();
    }

    public void clear(Player player) {
        if (!isSupported(player)) {
            return;
        }
        try {
            player.clearPlayerListHeaderAndFooter();
            lastSentHeader.remove(player.getUniqueId());
            lastSentFooter.remove(player.getUniqueId());
        } catch (Exception e) {
            logDebug("clear-failed player=" + player.getUsername());
        }
    }

    private void clearAll() {
        for (Player player : proxy.getAllPlayers()) {
            clear(player);
        }
    }

    public void sendScheduled(Player player, boolean force) {
        if (!configuration.isEnabled()) {
            return;
        }
        if (!isSupported(player)) {
            logDebug("unsupported-protocol player=" + player.getUsername()
                    + " protocol=" + getProtocol(player));
            return;
        }
        scheduler.schedule(() -> sendTab(player, force),
                configuration.getSendDelayMillis(), TimeUnit.MILLISECONDS);
    }

    public void sendImmediately(Player player, boolean force) {
        if (!configuration.isEnabled()) {
            return;
        }
        if (!isSupported(player)) {
            return;
        }
        sendTab(player, force);
    }

    private void sendTab(Player player, boolean force) {
        if (!player.isActive()) {
            return;
        }
        Language language = getLanguageCode(player);
        TabListLanguageSection langSection =
                configuration.getLanguage(language.getCode());
        if (langSection == null) {
            logDebug("missing-language-section player=" + player.getUsername());
            return;
        }

        String playerName = player.getUsername();
        String serverName = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse(configuration.getFallbackServerName());
        String online = String.valueOf(proxy.getPlayerCount());
        String ping = String.valueOf(player.getPing());
        String langCode = language.getCode();
        String langName = language.getDisplayName();
        String prefix = resolvePrefixForPlayer(player, langCode);

        String renderedHeader = renderLines(langSection.getHeader(), playerName,
                serverName, online, ping, langCode, langName,
                player.getUniqueId(), prefix);
        String renderedFooter = renderLines(langSection.getFooter(), playerName,
                serverName, online, ping, langCode, langName,
                player.getUniqueId(), prefix);

        if (!force
                && renderedHeader.equals(lastSentHeader.get(player.getUniqueId()))
                && renderedFooter.equals(lastSentFooter.get(player.getUniqueId()))) {
            logDebug("cache-hit player=" + player.getUsername());
            return;
        }

        try {
            Component headerComp = serializer.deserialize(renderedHeader);
            Component footerComp = serializer.deserialize(renderedFooter);
            player.sendPlayerListHeaderAndFooter(headerComp, footerComp);
            lastSentHeader.put(player.getUniqueId(), renderedHeader);
            lastSentFooter.put(player.getUniqueId(), renderedFooter);
            logDebug("send-completed player=" + player.getUsername()
                    + " header=" + renderedHeader);
        } catch (Exception e) {
            logDebug("send-failed player=" + player.getUsername()
                    + " error=" + e.getMessage());
        }
    }

    private String renderLines(List<String> lines, String playerName,
                                 String serverName, String online,
                                 String ping, String langCode, String langName,
                                 UUID playerId, String prefix) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            String line = lines.get(i);
            line = line.replace("{player}", playerName);
            line = line.replace("{uuid}", playerId.toString());
            line = line.replace("{server}", serverName);
            line = line.replace("{online}", online);
            line = line.replace("{ping}", ping);
            line = line.replace("{language}", langName);
            line = line.replace("{language_code}", langCode);
            line = line.replace("{prefix}", prefix);
            line = line.replace("{suffix}", "");
            builder.append(line);
        }
        return builder.toString();
    }

    private String resolvePrefixForPlayer(Player player, String langCode) {
        if (localizedPrefixProvider == null) return "";
        try {
            String locale = player.getPlayerSettings().getLocale()
                    .toString().toLowerCase().replace('-', '_');
            java.lang.reflect.Method getPrefix = localizedPrefixProvider.getClass()
                    .getMethod("getPrefix",
                            UUID.class, String.class, String.class);
            java.util.concurrent.CompletableFuture<String> future =
                    (java.util.concurrent.CompletableFuture<String>)
                            getPrefix.invoke(localizedPrefixProvider,
                                    player.getUniqueId(), langCode, locale);
            return future.get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return "";
        }
    }

    private void updateAll() {
        if (!configuration.isEnabled()) {
            return;
        }
        for (Player player : proxy.getAllPlayers()) {
            if (!isSupported(player) || !player.isActive()) {
                continue;
            }
            sendTab(player, false);
        }
    }

    private boolean isSupported(Player player) {
        int protocol = getProtocol(player);
        return protocol >= MIN_PROTOCOL;
    }

    private int getProtocol(Player player) {
        try {
            return player.getProtocolVersion().getProtocol();
        } catch (Exception e) {
            return -1;
        }
    }

    private Language getLanguageCode(Player player) {
        if (languageResolver != null) {
            Language resolved = languageResolver.apply(player);
            if (resolved != null) {
                return resolved;
            }
        }
        try {
            return Language.findByInput(
                    player.getEffectiveLocale().toString())
                    .orElseGet(() -> Language.findByInput(
                            configuration.getFallbackLanguage())
                            .orElse(Language.ENGLISH));
        } catch (Exception e) {
            return Language.ENGLISH;
        }
    }

    private void logDebug(String msg) {
        if (configuration != null && configuration.isDebug()) {
            logger.info("Tab " + msg);
        }
    }
}
