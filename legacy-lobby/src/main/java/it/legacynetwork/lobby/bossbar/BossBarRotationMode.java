package it.legacynetwork.lobby.bossbar;

public enum BossBarRotationMode {
    SEQUENTIAL,
    RANDOM;

    public static BossBarRotationMode fromString(String value) {
        if (value == null) {
            return SEQUENTIAL;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SEQUENTIAL;
        }
    }
}
