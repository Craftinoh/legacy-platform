package it.legacynetwork.lobby.bossbar;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BossBarConfiguration {
    private final boolean enabled;
    private final int updateTicks;
    private final boolean rotationEnabled;
    private final BossBarRotationMode rotationMode;
    private final int rotationIntervalTicks;
    private final boolean placeholderApiEnabled;
    private final boolean debugPackets;
    private final boolean debugLifecycle;
    private final double witherDistance;
    private final double witherVerticalOffset;
    private final int repositionTicks;
    private final Map<String, BossBarDefinition> bars;

    public BossBarConfiguration(boolean enabled,
                                int updateTicks,
                                boolean rotationEnabled,
                                BossBarRotationMode rotationMode,
                                int rotationIntervalTicks,
                                boolean placeholderApiEnabled,
                                boolean debugPackets,
                                boolean debugLifecycle,
                                double witherDistance,
                                double witherVerticalOffset,
                                int repositionTicks,
                                Map<String, BossBarDefinition> bars) {
        this.enabled = enabled;
        this.updateTicks = updateTicks;
        this.rotationEnabled = rotationEnabled;
        this.rotationMode = rotationMode;
        this.rotationIntervalTicks = rotationIntervalTicks;
        this.placeholderApiEnabled = placeholderApiEnabled;
        this.debugPackets = debugPackets;
        this.debugLifecycle = debugLifecycle;
        this.witherDistance = witherDistance;
        this.witherVerticalOffset = witherVerticalOffset;
        this.repositionTicks = repositionTicks;
        this.bars = Collections.unmodifiableMap(
                new LinkedHashMap<String, BossBarDefinition>(bars));
    }

    public boolean isEnabled() { return enabled; }
    public int getUpdateTicks() { return updateTicks; }
    public boolean isRotationEnabled() { return rotationEnabled; }
    public BossBarRotationMode getRotationMode() { return rotationMode; }
    public int getRotationIntervalTicks() { return rotationIntervalTicks; }
    public boolean isPlaceholderApiEnabled() { return placeholderApiEnabled; }
    public boolean isDebugPackets() { return debugPackets; }
    public boolean isDebugLifecycle() { return debugLifecycle; }
    public double getWitherDistance() { return witherDistance; }
    public double getWitherVerticalOffset() { return witherVerticalOffset; }
    public int getRepositionTicks() { return repositionTicks; }
    public Map<String, BossBarDefinition> getBars() { return bars; }
    public BossBarDefinition getBar(String id) { return bars.get(id); }

    public static BossBarConfiguration load(File file) {
        return load(file, null);
    }

    /**
     * Loads administrator settings and fills only absent bars/languages from
     * the bundled file. Existing custom text and technical settings win.
     */
    public static BossBarConfiguration load(File file,
                                            InputStream bundledDefaults) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean enabled = config.getBoolean("enabled", true);
        int updateTicks = Math.max(1, config.getInt("update.ticks", 5));
        boolean rotationEnabled = config.getBoolean("rotation.enabled", true);
        BossBarRotationMode rotationMode = BossBarRotationMode.fromString(
                config.getString("rotation.mode", "SEQUENTIAL"));
        int rotationIntervalTicks = Math.max(1,
                config.getInt("rotation.interval-ticks", 100));
        boolean placeholderApiEnabled = config.getBoolean(
                "placeholderapi.enabled", true);
        boolean debugPackets = config.getBoolean("debug.packets", false);
        boolean debugLifecycle = config.getBoolean("debug.lifecycle", false);
        double witherDistance = config.getDouble(
                "compatibility.legacy-wither.distance", 35.0);
        double witherVerticalOffset = config.getDouble(
                "compatibility.legacy-wither.vertical-offset", 0.0);
        int repositionTicks = Math.max(1, config.getInt(
                "compatibility.legacy-wither.reposition-ticks", 20));

        Map<String, BossBarDefinition> bars = readBars(config);
        Map<String, BossBarDefinition> defaults = readDefaultBars(bundledDefaults);
        mergeMissingBarsAndLanguages(bars, defaults);

        return new BossBarConfiguration(enabled, updateTicks, rotationEnabled,
                rotationMode, rotationIntervalTicks, placeholderApiEnabled,
                debugPackets, debugLifecycle, witherDistance,
                witherVerticalOffset, repositionTicks, bars);
    }

    private static Map<String, BossBarDefinition> readDefaultBars(
            InputStream bundledDefaults) {
        if (bundledDefaults == null) {
            return Collections.emptyMap();
        }
        try (InputStream input = bundledDefaults;
             InputStreamReader reader = new InputStreamReader(
                     input, StandardCharsets.UTF_8)) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            return readBars(defaults);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private static void mergeMissingBarsAndLanguages(
            Map<String, BossBarDefinition> bars,
            Map<String, BossBarDefinition> defaults) {
        for (Map.Entry<String, BossBarDefinition> entry : defaults.entrySet()) {
            BossBarDefinition current = bars.get(entry.getKey());
            BossBarDefinition fallback = entry.getValue();
            if (current == null) {
                bars.put(entry.getKey(), fallback);
                continue;
            }

            Map<String, String> mergedTexts =
                    new LinkedHashMap<String, String>(fallback.getLanguageTexts());
            mergedTexts.putAll(current.getLanguageTexts());
            bars.put(entry.getKey(), new BossBarDefinition(
                    current.getId(), current.isEnabled(), current.getPriority(),
                    current.getDisplayTicks(), mergedTexts,
                    current.getProgress()));
        }
    }

    private static Map<String, BossBarDefinition> readBars(
            FileConfiguration config) {
        Map<String, BossBarDefinition> bars =
                new LinkedHashMap<String, BossBarDefinition>();
        ConfigurationSection barsSection =
                config.getConfigurationSection("bars");
        if (barsSection == null) {
            return bars;
        }
        for (String barId : barsSection.getKeys(false)) {
            ConfigurationSection barSection =
                    barsSection.getConfigurationSection(barId);
            if (barSection == null) {
                continue;
            }
            boolean barEnabled = barSection.getBoolean("enabled", true);
            int priority = barSection.getInt("priority", 50);
            int displayTicks = Math.max(1,
                    barSection.getInt("display-ticks", 100));

            Map<String, String> languageTexts =
                    new LinkedHashMap<String, String>();
            ConfigurationSection langSection =
                    barSection.getConfigurationSection("languages");
            if (langSection != null) {
                for (String langKey : langSection.getKeys(false)) {
                    ConfigurationSection langText =
                            langSection.getConfigurationSection(langKey);
                    if (langText != null) {
                        languageTexts.put(langKey,
                                langText.getString("text", ""));
                    }
                }
            }

            ConfigurationSection progressSection =
                    barSection.getConfigurationSection("progress");
            BossBarProgress progress = progressSection != null
                    ? loadProgress(progressSection)
                    : new BossBarProgress(BossBarProgressType.STATIC,
                            0, 0, 0, "", "", 1.0, 1.0);

            bars.put(barId, new BossBarDefinition(
                    barId, barEnabled, priority, displayTicks,
                    languageTexts, progress));
        }
        return bars;
    }

    private static BossBarProgress loadProgress(ConfigurationSection section) {
        BossBarProgressType type = BossBarProgressType.fromString(
                section.getString("type", "STATIC"));
        double start = section.getDouble("start", 1.0);
        double end = section.getDouble("end", 0.0);
        int durationTicks = section.getInt("duration-ticks", 100);
        String currentPlaceholder = section.getString("current", "");
        String maximumPlaceholder = section.getString("maximum", "500");
        double staticValue = section.getDouble("value", 1.0);
        double fallback = section.getDouble("fallback", 1.0);
        return new BossBarProgress(type, start, end, durationTicks,
                currentPlaceholder, maximumPlaceholder,
                staticValue, fallback);
    }
}
