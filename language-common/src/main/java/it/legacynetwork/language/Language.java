package it.legacynetwork.language;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum Language {
    ITALIAN("it", "Italiano", "it", "ita", "italiano", "italian"),
    ENGLISH("en", "English", "en", "eng", "english", "inglese");

    private final String code;
    private final String displayName;
    private final Set<String> aliases;

    Language(String code, String displayName, String... aliases) {
        this.code = code;
        this.displayName = displayName;
        this.aliases = Collections.unmodifiableSet(
                new LinkedHashSet<String>(Arrays.asList(aliases)));
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public static Optional<Language> findByInput(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        for (Language language : values()) {
            if (language.aliases.contains(normalized)) {
                return Optional.of(language);
            }
        }
        return Optional.empty();
    }
}
