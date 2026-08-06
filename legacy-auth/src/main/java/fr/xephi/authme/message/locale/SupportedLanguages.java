package fr.xephi.authme.message.locale;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The languages LegacyAuth ships message files for, and the normalization of raw locale strings
 * onto them.
 * <p>
 * Locales arrive in many shapes — {@code en_US}, {@code EN-us}, {@code it_IT} — and almost all of
 * them collapse onto the plain language part. The exception is Brazilian Portuguese, which is kept
 * distinct from European Portuguese because the two translations differ.
 */
public final class SupportedLanguages {

    /** Brazilian Portuguese, the one region-specific language that is kept separate. */
    private static final String PORTUGUESE_BRAZIL = "pt_br";

    /** The language codes shipped with LegacyAuth, in the order they are documented. */
    private static final Set<String> CODES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
        "en", "it", "es", "fr", "de", "pt", PORTUGUESE_BRAZIL, "nl", "pl", "ro", "hu", "cs", "sk",
        "sl", "hr", "bg", "el", "da", "sv", "no", "fi", "is", "et", "lv", "lt", "ga", "mt", "ru",
        "uk", "tr", "sr")));

    private SupportedLanguages() {
    }

    /**
     * Returns the shipped language codes.
     *
     * @return the supported language codes
     */
    public static Set<String> getCodes() {
        return CODES;
    }

    /**
     * Returns whether a message file is shipped for the given code.
     *
     * @param code the language code, already normalized
     * @return true if the language is shipped with LegacyAuth
     */
    public static boolean isSupported(String code) {
        return code != null && CODES.contains(code);
    }

    /**
     * Normalizes a raw language tag or locale onto a language code.
     * <p>
     * Case, hyphens and underscores are all accepted: {@code en_US}, {@code EN-us} and {@code en}
     * all yield {@code en}. Brazilian Portuguese ({@code pt_BR}) yields {@code pt_br}, every other
     * Portuguese variant yields {@code pt}.
     *
     * @param rawLocale the locale to normalize, may be null
     * @return the normalized language code, or null when nothing usable was given
     */
    public static String normalize(String rawLocale) {
        if (rawLocale == null) {
            return null;
        }
        String trimmed = rawLocale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (trimmed.isEmpty()) {
            return null;
        }

        int separator = trimmed.indexOf('_');
        String language = separator < 0 ? trimmed : trimmed.substring(0, separator);
        if (language.isEmpty()) {
            return null;
        }
        String region = separator < 0 ? "" : trimmed.substring(separator + 1);

        // Brazilian Portuguese has its own translation; all other regions collapse to the language.
        if ("pt".equals(language) && region.startsWith("br")) {
            return PORTUGUESE_BRAZIL;
        }
        return language;
    }
}
