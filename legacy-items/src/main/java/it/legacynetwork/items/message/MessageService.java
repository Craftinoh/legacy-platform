package it.legacynetwork.items.message;

import it.legacynetwork.items.placeholder.ItemPlaceholderService;
import it.legacynetwork.items.util.LegacyColorTranslator;
import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MessageService {
    private final JavaPlugin plugin;
    private final ItemPlaceholderService placeholderService;
    private final String serverId;
    private final String fallbackLanguage;
    private final Map<String, Map<String, List<String>>> messagesByLang = new HashMap<>();
    private final Map<String, String> prefixesByLang = new HashMap<>();

    public MessageService(JavaPlugin plugin,
                           ItemPlaceholderService placeholderService,
                           String serverId,
                           String fallbackLanguage) {
        this.plugin = plugin;
        this.placeholderService = placeholderService;
        this.serverId = serverId;
        this.fallbackLanguage = fallbackLanguage != null ? fallbackLanguage : "en";
    }

    public void load() {
        messagesByLang.clear();
        prefixesByLang.clear();
        loadLanguageFile("it", new File(plugin.getDataFolder(), "messages_it.yml"));
        loadLanguageFile("en", new File(plugin.getDataFolder(), "messages_en.yml"));
    }

    private void loadLanguageFile(String langCode, File file) {
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, List<String>> loaded = new LinkedHashMap<>();
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                Object value = yaml.get(key);
                if (value instanceof String) {
                    loaded.put(key, Collections.singletonList((String) value));
                }
                continue;
            }
            boolean enabled = section.getBoolean("enabled", true);
            if (!enabled) {
                continue;
            }
            Object textObj = section.get("text");
            List<String> text;
            if (textObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) textObj;
                text = list;
            } else if (textObj instanceof String) {
                text = Collections.singletonList((String) textObj);
            } else {
                text = Collections.emptyList();
            }
            loaded.put(key, text);
        }
        messagesByLang.put(langCode, loaded);

        List<String> prefixLines = loaded.get("prefix");
        if (prefixLines != null && !prefixLines.isEmpty()) {
            prefixesByLang.put(langCode,
                    LegacyColorTranslator.translate(prefixLines.get(0)));
        }
    }

    private String resolveLanguage(Player player) {
        if (player == null) {
            return fallbackLanguage;
        }
        PlayerLanguageProvider provider = Bukkit.getServicesManager()
                .load(PlayerLanguageProvider.class);
        if (provider != null) {
            return provider.getLanguage(player.getUniqueId()).getCode();
        }
        return fallbackLanguage;
    }

    public String getMessage(String key, Player player) {
        return getMessage(key, player, Collections.<String, String>emptyMap());
    }

    public String getMessage(String key, Player player,
                              Map<String, String> extraPlaceholders) {
        String lang = resolveLanguage(player);
        Map<String, List<String>> langMessages = messagesByLang.get(lang);
        if (langMessages == null) {
            langMessages = messagesByLang.get(fallbackLanguage);
        }
        if (langMessages == null) {
            return "";
        }
        List<String> lines = langMessages.get(key);
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        String raw = lines.get(0);
        String result = raw;
        result = result.replace("{player}",
                player != null ? player.getName() : "???");
        result = result.replace("{uuid}",
                player != null ? player.getUniqueId().toString() : "???");
        result = result.replace("{world}",
                player != null ? player.getWorld().getName() : "???");
        result = result.replace("{server}", serverId);
        String prefix = prefixesByLang.getOrDefault(lang, "");
        result = result.replace("{prefix}", prefix);
        if (extraPlaceholders != null) {
            for (Map.Entry<String, String> entry : extraPlaceholders.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}",
                        entry.getValue());
            }
        }
        if (placeholderService.isAvailable() && player != null) {
            result = placeholderService.apply(player, result);
        }
        return LegacyColorTranslator.translate(result);
    }
}
