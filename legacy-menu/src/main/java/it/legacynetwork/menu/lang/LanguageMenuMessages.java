package it.legacynetwork.menu.lang;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageMenuMessages {
    private final File dataFolder;
    private final Map<String, YamlConfiguration> cache = new HashMap<String, YamlConfiguration>();

    public LanguageMenuMessages(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public String get(String lang, String key) {
        YamlConfiguration config = cache.get(lang);
        if (config == null) {
            config = loadMessages(lang);
            cache.put(lang, config);
        }
        String value = config.getString(key);
        if (value == null && !"en".equals(lang)) {
            YamlConfiguration enConfig = cache.get("en");
            if (enConfig == null) {
                enConfig = loadMessages("en");
                cache.put("en", enConfig);
            }
            value = enConfig.getString(key);
        }
        return value != null ? value : "missing:" + key;
    }

    public List<String> getList(String lang, String key) {
        YamlConfiguration config = cache.get(lang);
        if (config == null) {
            config = loadMessages(lang);
            cache.put(lang, config);
        }
        List<String> value = config.getStringList(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        if (!"en".equals(lang)) {
            YamlConfiguration enConfig = cache.get("en");
            if (enConfig == null) {
                enConfig = loadMessages("en");
                cache.put("en", enConfig);
            }
            value = enConfig.getStringList(key);
        }
        return value;
    }

    public void clearCache() {
        cache.clear();
    }

    private YamlConfiguration loadMessages(String lang) {
        File file = new File(dataFolder, "messages_" + lang + ".yml");
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        if (!"en".equals(lang)) {
            return loadMessages("en");
        }
        return createDefaultMessages();
    }

    private YamlConfiguration createDefaultMessages() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("menu.title", "&8Select Language");
        config.set("menu.selected", "&a\u2714 &f");
        config.set("menu.click", "&aYou selected %language%");
        config.set("menu.lore.selected", "&7This is your current language");
        config.set("menu.lore.unselected", "&7Click to change language");
        config.set("menu.lore.current", "&7Current language: %language%");
        config.set("menu.lore.change", "&7Click to select this language");
        config.set("menu.page", "&ePage &f{current}&7/&f{total}");
        config.set("menu.close", "&cClose");
        config.set("menu.back", "&7Close");
        config.set("menu.next", "&aNext Page &7\u2192");
        config.set("menu.prev", "&7\u2190 &aPrevious Page");
        return config;
    }
}
