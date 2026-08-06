package it.legacynetwork.regions.model;

public enum FlagState {
    ALLOW,
    DENY,
    INHERIT;

    public static FlagState fromString(String s) {
        if (s == null) {
            return null;
        }
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
