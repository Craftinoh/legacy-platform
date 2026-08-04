package it.legacynetwork.lobby.bossbar;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
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
        this.bars = Collections.unmodifiableMap(bars);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getUpdateTicks() {
        return updateTicks;
    }

    public boolean isRotationEnabled() {
        return rotationEnabled;
    }

    public BossBarRotationMode getRotationMode() {
        return rotationMode;
    }

    public int getRotationIntervalTicks() {
        return rotationIntervalTicks;
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    public boolean isDebugPackets() {
        return debugPackets;
    }

    public boolean isDebugLifecycle() {
        return debugLifecycle;
    }

    public double getWitherDistance() {
        return witherDistance;
    }

    public double getWitherVerticalOffset() {
        return witherVerticalOffset;
    }

    public int getRepositionTicks() {
        return repositionTicks;
    }

    public Map<String, BossBarDefinition> getBars() {
        return bars;
    }

    public BossBarDefinition getBar(String id) {
        return bars.get(id);
    }

    public static BossBarConfiguration load(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean enabled = config.getBoolean("enabled", true);
        int updateTicks = Math.max(1, config.getInt("update.ticks", 5));
        boolean rotationEnabled = config.getBoolean("rotation.enabled", true);
        BossBarRotationMode rotationMode =
                BossBarRotationMode.fromString(config.getString("rotation.mode", "SEQUENTIAL"));
        int rotationIntervalTicks =
                Math.max(1, config.getInt("rotation.interval-ticks", 100));
        boolean placeholderApiEnabled = config.getBoolean("placeholderapi.enabled", true);
        boolean debugPackets = config.getBoolean("debug.packets", false);
        boolean debugLifecycle = config.getBoolean("debug.lifecycle", false);
        double witherDistance = config.getDouble("compatibility.legacy-wither.distance", 35.0);
        double witherVerticalOffset =
                config.getDouble("compatibility.legacy-wither.vertical-offset", 0.0);
        int repositionTicks =
                Math.max(1, config.getInt("compatibility.legacy-wither.reposition-ticks", 20));

        Map<String, BossBarDefinition> bars = new LinkedHashMap<>();
        ConfigurationSection barsSection = config.getConfigurationSection("bars");
        if (barsSection != null) {
            for (String barId : barsSection.getKeys(false)) {
                ConfigurationSection barSection =
                        barsSection.getConfigurationSection(barId);
                if (barSection == null) {
                    continue;
                }
                boolean barEnabled = barSection.getBoolean("enabled", true);
                int priority = barSection.getInt("priority", 50);
                int displayTicks = Math.max(1, barSection.getInt("display-ticks", 100));

                Map<String, String> languageTexts = new LinkedHashMap<>();
                ConfigurationSection langSection =
                        barSection.getConfigurationSection("languages");
                if (langSection != null) {
                    for (String langKey : langSection.getKeys(false)) {
                        ConfigurationSection langText =
                                langSection.getConfigurationSection(langKey);
                        if (langText != null) {
                            String text = langText.getString("text", "");
                            languageTexts.put(langKey, text);
                        }
                    }
                }

                ConfigurationSection progressSection =
                        barSection.getConfigurationSection("progress");
                BossBarProgress progress;
                if (progressSection != null) {
                    progress = loadProgress(progressSection);
                } else {
                    progress = new BossBarProgress(
                            BossBarProgressType.STATIC, 0, 0, 0, "", "", 1.0, 1.0);
                }

                bars.put(barId, new BossBarDefinition(
                        barId, barEnabled, priority, displayTicks, languageTexts, progress));
            }
        }
        return new BossBarConfiguration(enabled, updateTicks, rotationEnabled,
                rotationMode, rotationIntervalTicks, placeholderApiEnabled,
                debugPackets, debugLifecycle, witherDistance, witherVerticalOffset,
                repositionTicks, bars);
    }

    private static BossBarProgress loadProgress(ConfigurationSection section) {
        BossBarProgressType type =
                BossBarProgressType.fromString(section.getString("type", "STATIC"));
        double start = section.getDouble("start", 1.0);
        double end = section.getDouble("end", 0.0);
        int durationTicks = section.getInt("duration-ticks", 100);
        String currentPlaceholder = section.getString("current", "");
        String maximumPlaceholder = section.getString("maximum", "500");
        double staticValue = section.getDouble("value", 1.0);
        double fallback = section.getDouble("fallback", 1.0);
        return new BossBarProgress(type, start, end, durationTicks,
                currentPlaceholder, maximumPlaceholder, staticValue, fallback);
    }
}
