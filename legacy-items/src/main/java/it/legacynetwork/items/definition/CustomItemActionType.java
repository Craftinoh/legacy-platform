package it.legacynetwork.items.definition;

public enum CustomItemActionType {
    OPEN_MENU,
    PLAYER_COMMAND,
    CONSOLE_COMMAND,
    MESSAGE,
    CONNECT_SERVER;

    public static CustomItemActionType fromString(String value) {
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
