package it.legacynetwork.lobby.bossbar;

public enum BossBarProgressType {
    STATIC,
    COUNTDOWN,
    COUNTUP,
    PLACEHOLDER_RATIO;

    public static BossBarProgressType fromString(String value) {
        if (value == null) {
            return STATIC;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STATIC;
        }
    }
}
