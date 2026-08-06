package it.legacynetwork.regions.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;
import it.legacynetwork.regions.model.RegionDecision;
import it.legacynetwork.regions.model.RegionFlag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RegionMessageService {

    private final JavaPlugin plugin;
    private final Map<UUID, Map<RegionFlag, Long>> cooldowns =
            new HashMap<UUID, Map<RegionFlag, Long>>();

    private volatile boolean enabled;
    private volatile long cooldownMillis;
    private volatile String fallbackLanguage = "en";
    private volatile Map<RegionFlag, Boolean> enabledActions =
            Collections.emptyMap();
    private volatile YamlConfiguration italianMessages =
            new YamlConfiguration();
    private volatile YamlConfiguration englishMessages =
            new YamlConfiguration();
    private volatile PlayerLanguageProvider languageProvider;

    public RegionMessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean reload() {
        String italianFileName = plugin.getConfig().getString(
                "language.italian-file", "messages_it.yml");
        String englishFileName = plugin.getConfig().getString(
                "language.english-file", "messages_en.yml");

        try {
            YamlConfiguration newItalian = loadStrict(
                    new File(plugin.getDataFolder(), italianFileName));
            YamlConfiguration newEnglish = loadStrict(
                    new File(plugin.getDataFolder(), englishFileName));

            EnumMap<RegionFlag, Boolean> newEnabledActions =
                    new EnumMap<RegionFlag, Boolean>(RegionFlag.class);
            ConfigurationSection actions = plugin.getConfig()
                    .getConfigurationSection("messages.actions");
            for (RegionFlag flag : RegionFlag.values()) {
                boolean actionEnabled = actions != null
                        && actions.getBoolean(flag.getPermissionKey(), false);
                newEnabledActions.put(flag, actionEnabled);
            }

            this.enabled = plugin.getConfig().getBoolean(
                    "messages.enabled", true);
            this.cooldownMillis = Math.max(0L, plugin.getConfig().getLong(
                    "messages.cooldown-millis", 3000L));
            this.fallbackLanguage = normalizeLanguage(
                    plugin.getConfig().getString("language.fallback", "en"));
            this.enabledActions = Collections.unmodifiableMap(newEnabledActions);
            this.italianMessages = newItalian;
            this.englishMessages = newEnglish;
            this.languageProvider = null;
            this.cooldowns.clear();
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning(
                    "Impossibile caricare i messaggi LegacyRegions: "
                            + exception.getMessage());
            return false;
        } catch (InvalidConfigurationException exception) {
            plugin.getLogger().warning(
                    "File messaggi LegacyRegions non valido: "
                            + exception.getMessage());
            return false;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning(
                    "Configurazione messaggi LegacyRegions non valida: "
                            + exception.getMessage());
            return false;
        }
    }

    private YamlConfiguration loadStrict(File file)
            throws IOException, InvalidConfigurationException {
        if (!file.isFile()) {
            throw new IOException("File non trovato: " + file.getName());
        }
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.load(file);
        return configuration;
    }

    public void sendDenied(Player player, RegionFlag requestedFlag,
                           RegionDecision decision) {
        if (player == null || requestedFlag == null || !enabled
                || !Boolean.TRUE.equals(enabledActions.get(requestedFlag))) {
            return;
        }

        String language = resolveLanguage(player);
        String message = findMessage(language, requestedFlag);
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        if (isCoolingDown(player.getUniqueId(), requestedFlag)) {
            return;
        }

        String regionId = decision == null
                || decision.getDecidingRegion() == null
                ? "global" : decision.getDecidingRegion();
        String rendered = message
                .replace("{player}", player.getName())
                .replace("{region}", regionId)
                .replace("{flag}", requestedFlag.getPermissionKey());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', rendered));
    }

    private String findMessage(String language, RegionFlag flag) {
        YamlConfiguration selected = messagesFor(language);
        YamlConfiguration fallback = messagesFor(fallbackLanguage);
        String path = "denied." + flag.getPermissionKey();

        String message = selected.getString(path);
        if (isBlank(message)) {
            message = selected.getString("denied.generic");
        }
        if (isBlank(message) && selected != fallback) {
            message = fallback.getString(path);
        }
        if (isBlank(message) && selected != fallback) {
            message = fallback.getString("denied.generic");
        }
        return message;
    }

    private YamlConfiguration messagesFor(String language) {
        return "it".equalsIgnoreCase(language)
                ? italianMessages : englishMessages;
    }

    private String resolveLanguage(Player player) {
        PlayerLanguageProvider provider = resolveLanguageProvider();
        if (provider == null) {
            return fallbackLanguage;
        }
        try {
            Language language = provider.getLanguage(player.getUniqueId());
            if (language == null) {
                return fallbackLanguage;
            }
            return normalizeLanguage(language.getCode());
        } catch (RuntimeException exception) {
            return fallbackLanguage;
        } catch (LinkageError error) {
            languageProvider = null;
            return fallbackLanguage;
        }
    }

    private PlayerLanguageProvider resolveLanguageProvider() {
        PlayerLanguageProvider provider = languageProvider;
        if (provider != null) {
            return provider;
        }
        try {
            provider = Bukkit.getServicesManager().load(
                    PlayerLanguageProvider.class);
            languageProvider = provider;
            return provider;
        } catch (RuntimeException exception) {
            return null;
        } catch (LinkageError error) {
            return null;
        }
    }

    private boolean isCoolingDown(UUID playerId, RegionFlag flag) {
        long now = System.currentTimeMillis();
        Map<RegionFlag, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            playerCooldowns = new EnumMap<RegionFlag, Long>(RegionFlag.class);
            cooldowns.put(playerId, playerCooldowns);
        }
        Long lastMessage = playerCooldowns.get(flag);
        if (lastMessage != null && now - lastMessage < cooldownMillis) {
            return true;
        }
        playerCooldowns.put(flag, now);
        return false;
    }

    public void clearPlayer(UUID playerId) {
        cooldowns.remove(playerId);
    }

    public void close() {
        cooldowns.clear();
        languageProvider = null;
        enabledActions = Collections.emptyMap();
    }

    private String normalizeLanguage(String language) {
        if (language == null) {
            return "en";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        return "it".equals(normalized) ? "it" : "en";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
