package it.legacynetwork.language;

public enum LanguagePreference {
    AUTOMATIC,
    MANUAL;

    public boolean overridesClientLocale() {
        return this == MANUAL;
    }
}
