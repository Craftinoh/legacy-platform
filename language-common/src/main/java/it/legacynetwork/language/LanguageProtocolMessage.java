package it.legacynetwork.language;

import java.util.Objects;
import java.util.UUID;

public final class LanguageProtocolMessage {
    private final int protocolVersion;
    private final LanguageProtocolAction action;
    private final UUID playerUuid;
    private final String languageCode;
    private final boolean manualPreference;

    public LanguageProtocolMessage(int protocolVersion,
                                   LanguageProtocolAction action,
                                   UUID playerUuid,
                                   String languageCode,
                                   boolean manualPreference) {
        this.protocolVersion = protocolVersion;
        this.action = Objects.requireNonNull(action, "action");
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.languageCode = Objects.requireNonNull(languageCode, "languageCode");
        this.manualPreference = manualPreference;
    }

    public static LanguageProtocolMessage languageSync(UUID playerUuid,
                                                       Language language,
                                                       boolean manualPreference) {
        return new LanguageProtocolMessage(
                LanguageProtocol.VERSION,
                LanguageProtocolAction.LANGUAGE_SYNC,
                playerUuid,
                language.getCode(),
                manualPreference);
    }

    public static LanguageProtocolMessage languageChangeRequest(UUID playerUuid,
                                                                Language language) {
        return new LanguageProtocolMessage(
                LanguageProtocol.VERSION,
                LanguageProtocolAction.LANGUAGE_CHANGE_REQUEST,
                playerUuid,
                language.getCode(),
                true);
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public LanguageProtocolAction getAction() {
        return action;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public boolean isManualPreference() {
        return manualPreference;
    }
}
