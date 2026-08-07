package it.legacynetwork.lobby.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScoreboardConfiguration {
    private final boolean enabled;
    private final int updateTicks;
    private final boolean placeholderApiEnabled;
    private final Map<String, LanguageSection> languages;

    public ScoreboardConfiguration(boolean enabled,
                                   int updateTicks,
                                   boolean placeholderApiEnabled,
                                   Map<String, LanguageSection> languages) {
        this.enabled = enabled;
        this.updateTicks = updateTicks;
        this.placeholderApiEnabled = placeholderApiEnabled;
        this.languages = Collections.unmodifiableMap(
                new LinkedHashMap<String, LanguageSection>(languages));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getUpdateTicks() {
        return updateTicks;
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    public LanguageSection getLanguage(String code) {
        LanguageSection section = languages.get(code);
        if (section == null) {
            section = languages.get("en");
        }
        if (section == null && !languages.isEmpty()) {
            section = languages.values().iterator().next();
        }
        return section;
    }

    public boolean hasLanguage(String code) {
        return languages.containsKey(code);
    }

    public Map<String, LanguageSection> getLanguages() {
        return languages;
    }

    public static ScoreboardConfiguration load(File file) {
        return load(file, null);
    }

    /**
     * Loads the administrator file and overlays only missing language sections
     * from the bundled defaults. Existing custom titles and lines always win.
     */
    public static ScoreboardConfiguration load(File file,
                                                InputStream bundledDefaults) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean enabled = config.getBoolean("enabled", true);
        int updateTicks = Math.max(1, config.getInt("update.ticks", 20));
        boolean placeholderApiEnabled = config.getBoolean(
                "placeholderapi.enabled", true);

        Map<String, LanguageSection> languages = readLanguages(config);
        Map<String, LanguageSection> defaults = readDefaults(bundledDefaults);
        for (Map.Entry<String, LanguageSection> entry : defaults.entrySet()) {
            if (!languages.containsKey(entry.getKey())) {
                languages.put(entry.getKey(), entry.getValue());
            }
        }

        if (!languages.containsKey("en")) {
            languages.put("en", defaultEnglish());
        }
        return new ScoreboardConfiguration(enabled, updateTicks,
                placeholderApiEnabled, languages);
    }

    private static Map<String, LanguageSection> readDefaults(
            InputStream bundledDefaults) {
        if (bundledDefaults == null) {
            return Collections.emptyMap();
        }
        try (InputStream input = bundledDefaults;
             InputStreamReader reader = new InputStreamReader(
                     input, StandardCharsets.UTF_8)) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            return readLanguages(defaults);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private static Map<String, LanguageSection> readLanguages(
            FileConfiguration config) {
        Map<String, LanguageSection> languages =
                new LinkedHashMap<String, LanguageSection>();
        ConfigurationSection languagesSection =
                config.getConfigurationSection("languages");
        if (languagesSection == null) {
            return languages;
        }
        for (String langCode : languagesSection.getKeys(false)) {
            ConfigurationSection langSection =
                    languagesSection.getConfigurationSection(langCode);
            if (langSection == null) {
                continue;
            }
            String title = langSection.getString("title", "&6&lAPTERIS");
            List<String> lines = langSection.getStringList("lines");
            if (lines.size() > 15) {
                lines = new ArrayList<String>(lines.subList(0, 15));
            }
            languages.put(langCode,
                    new LanguageSection(title, lines));
        }
        return languages;
    }

    private static LanguageSection defaultEnglish() {
        List<String> defaultLines = new ArrayList<String>();
        defaultLines.add("&7&m----------------");
        defaultLines.add("&fWelcome &a%player_name%");
        defaultLines.add("");
        defaultLines.add("&fOnline: &a{online}");
        defaultLines.add("");
        defaultLines.add("&eplay.apteris.net");
        defaultLines.add("&7&m----------------");
        return new LanguageSection("&6&lAPTERIS", defaultLines);
    }

    public static final class LanguageSection {
        private final String title;
        private final List<String> lines;

        public LanguageSection(String title, List<String> lines) {
            this.title = title;
            this.lines = Collections.unmodifiableList(
                    new ArrayList<String>(lines));
        }

        public String getTitle() {
            return title;
        }

        public List<String> getLines() {
            return lines;
        }
    }
}
