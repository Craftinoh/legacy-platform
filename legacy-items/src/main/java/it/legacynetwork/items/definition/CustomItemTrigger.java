package it.legacynetwork.items.definition;

public enum CustomItemTrigger {
    JOIN,
    RESPAWN,
    WORLD_CHANGE;

    public static CustomItemTrigger fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
