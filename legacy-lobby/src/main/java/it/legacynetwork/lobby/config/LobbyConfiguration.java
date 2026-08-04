package it.legacynetwork.lobby.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class LobbyConfiguration {
    private final String serverId;
    private final String languageChannel;
    private final boolean scoreboardEnabled;
    private final long scoreboardUpdateTicks;
    private final String website;
    private final boolean welcomeEnabled;

    public LobbyConfiguration(String serverId,
                              String languageChannel,
                              boolean scoreboardEnabled,
                              long scoreboardUpdateTicks,
                              String website,
                              boolean welcomeEnabled) {
        this.serverId = serverId;
        this.languageChannel = languageChannel;
        this.scoreboardEnabled = scoreboardEnabled;
        this.scoreboardUpdateTicks = scoreboardUpdateTicks;
        this.website = website;
        this.welcomeEnabled = welcomeEnabled;
    }

    public static LobbyConfiguration from(FileConfiguration configuration) {
        return new LobbyConfiguration(
                configuration.getString("server.id", "lobby-01"),
                configuration.getString("language.channel", "NetworkLang"),
                configuration.getBoolean("scoreboard.enabled", true),
                Math.max(1L, configuration.getLong("scoreboard.update-ticks", 60L)),
                configuration.getString("scoreboard.website", "example.net"),
                configuration.getBoolean("messages.welcome-enabled", true));
    }

    public String getServerId() {
        return serverId;
    }

    public String getLanguageChannel() {
        return languageChannel;
    }

    public boolean isScoreboardEnabled() {
        return scoreboardEnabled;
    }

    public long getScoreboardUpdateTicks() {
        return scoreboardUpdateTicks;
    }

    public String getWebsite() {
        return website;
    }

    public boolean isWelcomeEnabled() {
        return welcomeEnabled;
    }
}
