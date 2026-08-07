package it.legacynetwork.regions.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;
import it.legacynetwork.language.TranslationInstaller;
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
import java.util.LinkedHashMap;
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
    private volatile Map<String, YamlConfiguration> catalogs =
            Collections.emptyMap();
    private volatile PlayerLanguageProvider languageProvider;

    public RegionMessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean reload() {
        try {
            String translationsDirectory = plugin.getConfig().getString(
                    "language.translations-directory", "translations");
            File directory = new File(plugin.getDataFolder(),
                    translationsDirectory);
            Map<String, YamlConfiguration> loadedCatalogs =
                    new LinkedHashMap<String, YamlConfiguration>();

            for (String language : TranslationInstaller.ALL_LANGUAGES) {
                File file = new File(directory,
                        "messages_" + language + ".yml");
                if (file.isFile()) {
                    loadedCatalogs.put(language, loadStrict(file));
                }
            }

            // Backward compatibility with the original two root files.
            loadLegacyIfMissing(loadedCatalogs, "en",
                    plugin.getConfig().getString(
                            "language.english-file", "messages_en.yml"));
            loadLegacyIfMissing(loadedCatalogs, "it",
                    plugin.getConfig().getString(
                            "language.italian-file", "messages_it.yml"));

            if (!loadedCatalogs.containsKey("en")) {
                throw new IOException(
                        "Catalogo inglese translations/messages_en.yml mancante");
            }

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
            this.catalogs = Collections.unmodifiableMap(loadedCatalogs);
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

    private void loadLegacyIfMissing(
            Map<String, YamlConfiguration> loadedCatalogs,
            String language,
            String configuredPath)
            throws IOException, InvalidConfigurationException {
        if (loadedCatalogs.containsKey(language)
                || configuredPath == null
                || configuredPath.trim().isEmpty()) {
            return;
        }
        File legacy = new File(plugin.getDataFolder(), configuredPath);
        if (legacy.isFile()) {
            loadedCatalogs.put(language, loadStrict(legacy));
        }
    }

    private YamlConfiguration loadStrict(File file)
            throws IOException, InvalidConfigurationException {
        if (!file.isFile()) {
            throw new IOException("File non trovato: " + file.getPath());
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
        String path = "denied." + flag.getPermissionKey();
        YamlConfiguration selected = messagesFor(language);
        String message = selected.getString(path);
        if (isBlank(message)) {
            message = selected.getString("denied.generic");
        }

        if (isBlank(message)) {
            YamlConfiguration fallback = messagesFor(fallbackLanguage);
            message = fallback.getString(path);
            if (isBlank(message)) {
                message = fallback.getString("denied.generic");
            }
        }

        if (isBlank(message)) {
            YamlConfiguration english = messagesFor("en");
            message = english.getString(path);
            if (isBlank(message)) {
                message = english.getString("denied.generic");
            }
        }
        return message;
    }

    private YamlConfiguration messagesFor(String language) {
        YamlConfiguration catalog = catalogs.get(normalizeLanguage(language));
        if (catalog == null) {
            catalog = catalogs.get(fallbackLanguage);
        }
        if (catalog == null) {
            catalog = catalogs.get("en");
        }
        return catalog != null ? catalog : new YamlConfiguration();
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
        catalogs = Collections.emptyMap();
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return "en";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_');
        return Language.findByInput(normalized)
                .map(Language::getCode)
                .orElse("en");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
