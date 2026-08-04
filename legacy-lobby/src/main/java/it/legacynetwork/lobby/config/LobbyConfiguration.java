package it.legacynetwork.lobby.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class LobbyConfiguration {
    private final String serverId;
    private final String languageChannel;
    private final String languageFallback;
    private final String scoreboardConfigFile;
    private final String bossbarConfigFile;
    private final String messagesItalianFile;
    private final String messagesEnglishFile;
    private final boolean joinSlotEnabled;
    private final int joinSlot;
    private final int joinSlotDelayTicks;
    private final boolean joinSlotForce;

    public LobbyConfiguration(String serverId,
                               String languageChannel,
                               String languageFallback,
                               String scoreboardConfigFile,
                               String bossbarConfigFile,
                               String messagesItalianFile,
                               String messagesEnglishFile,
                               boolean joinSlotEnabled,
                               int joinSlot,
                               int joinSlotDelayTicks,
                               boolean joinSlotForce) {
        this.serverId = serverId;
        this.languageChannel = languageChannel;
        this.languageFallback = languageFallback;
        this.scoreboardConfigFile = scoreboardConfigFile;
        this.bossbarConfigFile = bossbarConfigFile;
        this.messagesItalianFile = messagesItalianFile;
        this.messagesEnglishFile = messagesEnglishFile;
        this.joinSlotEnabled = joinSlotEnabled;
        this.joinSlot = joinSlot;
        this.joinSlotDelayTicks = joinSlotDelayTicks;
        this.joinSlotForce = joinSlotForce;
    }

    public static LobbyConfiguration from(FileConfiguration configuration) {
        int slot = configuration.getInt("join.selected-slot.slot", 1);
        if (slot < 1 || slot > 9) {
            slot = 1;
        }
        int delayTicks = configuration.getInt("join.selected-slot.delay-ticks", 2);
        if (delayTicks < 0) {
            delayTicks = 0;
        }
        return new LobbyConfiguration(
                configuration.getString("server.id", "lobby-01"),
                configuration.getString("language.channel", "NetworkLang"),
                configuration.getString("language.fallback", "en"),
                configuration.getString("scoreboard.config-file", "scoreboard.yml"),
                configuration.getString("bossbar.config-file", "bossbar.yml"),
                configuration.getString("messages.italian-file", "messages_it.yml"),
                configuration.getString("messages.english-file", "messages_en.yml"),
                configuration.getBoolean("join.selected-slot.enabled", true),
                slot,
                delayTicks,
                configuration.getBoolean("join.selected-slot.force", true));
    }

    public String getServerId() {
        return serverId;
    }

    public String getLanguageChannel() {
        return languageChannel;
    }

    public String getLanguageFallback() {
        return languageFallback;
    }

    public String getScoreboardConfigFile() {
        return scoreboardConfigFile;
    }

    public String getBossbarConfigFile() {
        return bossbarConfigFile;
    }

    public String getMessagesItalianFile() {
        return messagesItalianFile;
    }

    public String getMessagesEnglishFile() {
        return messagesEnglishFile;
    }

    public boolean isJoinSlotEnabled() {
        return joinSlotEnabled;
    }

    public int getJoinSlot() {
        return joinSlot;
    }

    public int getJoinSlotDelayTicks() {
        return joinSlotDelayTicks;
    }

    public boolean isJoinSlotForce() {
        return joinSlotForce;
    }
}
