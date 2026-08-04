package it.legacynetwork.items.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class LegacyItemsConfiguration {
    private final boolean enabled;
    private final String serverId;
    private final String languageFallback;
    private final boolean placeholderApiEnabled;
    private final int giveDelayTicks;
    private final boolean overwriteExistingSlot;
    private final boolean removeCustomItemsBeforeGive;
    private final boolean reloadRefreshOnlinePlayers;
    private final boolean debug;

    public LegacyItemsConfiguration(boolean enabled,
                                     String serverId,
                                     String languageFallback,
                                     boolean placeholderApiEnabled,
                                     int giveDelayTicks,
                                     boolean overwriteExistingSlot,
                                     boolean removeCustomItemsBeforeGive,
                                     boolean reloadRefreshOnlinePlayers,
                                     boolean debug) {
        this.enabled = enabled;
        this.serverId = serverId;
        this.languageFallback = languageFallback;
        this.placeholderApiEnabled = placeholderApiEnabled;
        this.giveDelayTicks = giveDelayTicks;
        this.overwriteExistingSlot = overwriteExistingSlot;
        this.removeCustomItemsBeforeGive = removeCustomItemsBeforeGive;
        this.reloadRefreshOnlinePlayers = reloadRefreshOnlinePlayers;
        this.debug = debug;
    }

    public static LegacyItemsConfiguration from(FileConfiguration config) {
        return new LegacyItemsConfiguration(
                config.getBoolean("enabled", true),
                config.getString("server.id", "lobby-01"),
                config.getString("language.fallback", "en"),
                config.getBoolean("placeholderapi.enabled", true),
                Math.max(0, config.getInt("inventory.give-delay-ticks", 2)),
                config.getBoolean("inventory.overwrite-existing-slot", true),
                config.getBoolean("inventory.remove-custom-items-before-give", true),
                config.getBoolean("reload.refresh-online-players", true),
                config.getBoolean("debug.enabled", false));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getServerId() {
        return serverId;
    }

    public String getLanguageFallback() {
        return languageFallback;
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    public int getGiveDelayTicks() {
        return giveDelayTicks;
    }

    public boolean isOverwriteExistingSlot() {
        return overwriteExistingSlot;
    }

    public boolean isRemoveCustomItemsBeforeGive() {
        return removeCustomItemsBeforeGive;
    }

    public boolean isReloadRefreshOnlinePlayers() {
        return reloadRefreshOnlinePlayers;
    }

    public boolean isDebug() {
        return debug;
    }
}
