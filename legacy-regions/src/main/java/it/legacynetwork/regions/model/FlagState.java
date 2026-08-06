package it.legacynetwork.regions.model;

import java.util.Locale;

public enum FlagState {
    ALLOW,
    DENY,
    INHERIT;

    public static FlagState fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
