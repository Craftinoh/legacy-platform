package it.legacynetwork.menu.lang;

import it.legacynetwork.language.Language;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LanguageMenuMessages {
    private final File dataFolder;
    private volatile YamlConfiguration externalConfiguration =
            new YamlConfiguration();
    private volatile YamlConfiguration bundledConfiguration =
            new YamlConfiguration();
    private volatile Map<String, YamlConfiguration> legacyCatalogs =
            Collections.emptyMap();

    public LanguageMenuMessages(File dataFolder) {
        this.dataFolder = dataFolder;
        clearCache();
    }

    public String get(String languageCode, String key) {
        String language = normalize(languageCode);
        String value = resolveString(language, key);
        if (isBlank(value) && !"en".equals(language)) {
            value = resolveString("en", key);
        }
        if (isBlank(value)) {
            value = defaultString(key);
        }
        return value == null ? "" : value;
    }

    public List<String> getList(String languageCode, String key) {
        String language = normalize(languageCode);
        List<String> value = resolveList(language, key);
        if (value.isEmpty() && !"en".equals(language)) {
            value = resolveList("en", key);
        }
        if (value.isEmpty()) {
            value = defaultList(key);
        }
        return Collections.unmodifiableList(
                new ArrayList<String>(value));
    }

    public synchronized void clearCache() {
        File externalFile = new File(
                dataFolder, "language-menu.yml");
        externalConfiguration = externalFile.isFile()
                ? YamlConfiguration.loadConfiguration(externalFile)
                : new YamlConfiguration();
        bundledConfiguration = loadBundledConfiguration();

        Map<String, YamlConfiguration> catalogs =
                new HashMap<String, YamlConfiguration>();
        File translationsDirectory = new File(
                dataFolder, "translations");
        for (Language language : Language.values()) {
            String code = language.getCode();
            File file = new File(
                    translationsDirectory,
                    "messages_" + code + ".yml");
            if (file.isFile()) {
                catalogs.put(
                        code,
                        YamlConfiguration.loadConfiguration(file));
            }
        }
        legacyCatalogs = Collections.unmodifiableMap(catalogs);
    }

    private String resolveString(String language, String key) {
        String path = "languages." + language + "." + key;

        String value = externalConfiguration.getString(path);
        if (!isBlank(value)) {
            return value;
        }

        YamlConfiguration legacy = legacyCatalogs.get(language);
        if (legacy != null) {
            value = legacy.getString(key);
            if (!isBlank(value)) {
                return value;
            }
        }

        value = bundledConfiguration.getString(path);
        return isBlank(value) ? null : value;
    }

    private List<String> resolveList(String language, String key) {
        String path = "languages." + language + "." + key;

        List<String> value = externalConfiguration.getStringList(path);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        YamlConfiguration legacy = legacyCatalogs.get(language);
        if (legacy != null) {
            value = legacy.getStringList(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        value = bundledConfiguration.getStringList(path);
        return value == null
                ? Collections.<String>emptyList()
                : value;
    }

    private YamlConfiguration loadBundledConfiguration() {
        InputStream input = LanguageMenuMessages.class
                .getClassLoader()
                .getResourceAsStream("language-menu.yml");
        if (input == null) {
            return new YamlConfiguration();
        }
        try (InputStreamReader reader = new InputStreamReader(
                input, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception ignored) {
            return new YamlConfiguration();
        }
    }

    private String defaultString(String key) {
        if ("menu.title".equals(key)) {
            return "&8Choose your language";
        }
        if ("menu.language-name".equals(key)) {
            return "&f{language}";
        }
        if ("menu.language-name-selected".equals(key)) {
            return "&a✔ &f{language}";
        }
        if ("menu.status.selected".equals(key)) {
            return "Selected";
        }
        if ("menu.status.available".equals(key)) {
            return "Available";
        }
        if ("menu.page".equals(key)) {
            return "&ePage &f{current}&7/&f{total}";
        }
        if ("menu.close".equals(key)) {
            return "&cClose";
        }
        if ("menu.back".equals(key)) {
            return "&7Close";
        }
        if ("menu.next".equals(key)) {
            return "&aNext page &7→";
        }
        if ("menu.prev".equals(key)) {
            return "&7← &aPrevious page";
        }
        if ("change.success".equals(key)) {
            return "&aLanguage changed to &f{language}&a.";
        }
        if ("change.already-selected".equals(key)) {
            return "&e{language} is already your current language.";
        }
        if ("change.pending".equals(key)) {
            return "&eYour previous language request is still being processed.";
        }
        if ("change.cooldown".equals(key)) {
            return "&cPlease wait {seconds} seconds before changing language again.";
        }
        if ("change.rate-limited".equals(key)) {
            return "&cYou can change language at most {limit} times every {minutes} minutes.";
        }
        if ("change.unavailable".equals(key)) {
            return "&cThe language service is currently unavailable.";
        }
        if ("change.timeout".equals(key)) {
            return "&cThe language service did not answer in time. Try again.";
        }
        if ("change.error".equals(key)) {
            return "&cThe language could not be changed. Try again later.";
        }
        return "";
    }

    private List<String> defaultList(String key) {
        List<String> lines = new ArrayList<String>();
        if ("menu.lore.selected".equals(key)) {
            lines.add("&8{country} • {code}");
            lines.add("");
            lines.add("&a✔ This is your current language");
            lines.add("&7Menus, messages and interfaces");
            lines.add("&7are shown in this language.");
        } else if ("menu.lore.unselected".equals(key)) {
            lines.add("&8{country} • {code}");
            lines.add("");
            lines.add("&7Use this language for menus,");
            lines.add("&7messages and network interfaces.");
            lines.add("");
            lines.add("&eClick to select");
        } else if ("menu.page-lore".equals(key)) {
            lines.add("&7Choose the language used across");
            lines.add("&7the whole Apteris network.");
            lines.add("");
            lines.add("&8Limit: {limit} changes / {minutes} min");
        }
        return lines;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String language) {
        if (language == null) {
            return "en";
        }
        return language.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_');
    }
}
