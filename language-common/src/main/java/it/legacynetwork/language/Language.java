package it.legacynetwork.language;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum Language {
    ITALIAN("it", "Italian", "Italiano", 1, "it",
            "ita", "italiano", "italian", "it_ch"),
    ENGLISH("en", "English", "English", 2, "us",
            "eng", "english", "inglese",
            "en_gb", "en_ca", "en_au"),
    SPANISH("es", "Spanish", "Español", 3, "es",
            "spa", "spanish", "español",
            "es_mx", "es_ar"),
    FRENCH("fr", "French", "Français", 4, "fr",
            "fra", "fre", "french", "français",
            "fr_ca", "fr_be", "fr_ch"),
    GERMAN("de", "German", "Deutsch", 5, "de",
            "deu", "ger", "german", "deutsch",
            "de_at", "de_ch"),
    PORTUGUESE("pt", "Portuguese", "Português", 6, "pt",
            "por", "portuguese", "português",
            "pt_pt"),
    PORTUGUESE_BRAZIL("pt_br", "Brazilian Portuguese", "Português (Brasil)", 7, "br",
            "portuguese_br", "português_br"),
    DUTCH("nl", "Dutch", "Nederlands", 8, "nl",
            "nld", "dut", "dutch", "nederlands",
            "nl_be"),
    POLISH("pl", "Polish", "Polski", 9, "pl",
            "pol", "polish", "polski"),
    ROMANIAN("ro", "Romanian", "Română", 10, "ro",
            "ron", "rum", "romanian", "română"),
    HUNGARIAN("hu", "Hungarian", "Magyar", 11, "hu",
            "hun", "hungarian", "magyar"),
    CZECH("cs", "Czech", "Čeština", 12, "cz",
            "ces", "cze", "czech", "čeština"),
    SLOVAK("sk", "Slovak", "Slovenčina", 13, "sk",
            "slk", "slo", "slovak", "slovenčina"),
    SLOVENIAN("sl", "Slovenian", "Slovenščina", 14, "si",
            "slv", "slovenian", "slovenščina"),
    CROATIAN("hr", "Croatian", "Hrvatski", 15, "hr",
            "hrv", "croatian", "hrvatski"),
    BULGARIAN("bg", "Bulgarian", "Български", 16, "bg",
            "bul", "bulgarian", "български"),
    GREEK("el", "Greek", "Ελληνικά", 17, "gr",
            "ell", "gre", "greek", "ελληνικά"),
    DANISH("da", "Danish", "Dansk", 18, "dk",
            "dan", "danish", "dansk"),
    SWEDISH("sv", "Swedish", "Svenska", 19, "se",
            "swe", "swedish", "svenska"),
    NORWEGIAN("no", "Norwegian", "Norsk", 20, "no",
            "nor", "norwegian", "norsk"),
    FINNISH("fi", "Finnish", "Suomi", 21, "fi",
            "fin", "finnish", "suomi"),
    ICELANDIC("is", "Icelandic", "Íslenska", 22, "is",
            "isl", "ice", "icelandic", "íslenska"),
    ESTONIAN("et", "Estonian", "Eesti", 23, "ee",
            "est", "estonian", "eesti"),
    LATVIAN("lv", "Latvian", "Latviešu", 24, "lv",
            "lav", "latvian", "latviešu"),
    LITHUANIAN("lt", "Lithuanian", "Lietuvių", 25, "lt",
            "lit", "lithuanian", "lietuvių"),
    IRISH("ga", "Irish", "Gaeilge", 26, "ie",
            "gle", "irish", "gaeilge"),
    MALTESE("mt", "Maltese", "Malti", 27, "mt",
            "mlt", "maltese", "malti"),
    RUSSIAN("ru", "Russian", "Русский", 28, "ru",
            "rus", "russian", "русский"),
    UKRAINIAN("uk", "Ukrainian", "Українська", 29, "ua",
            "ukr", "ukrainian", "українська"),
    TURKISH("tr", "Turkish", "Türkçe", 30, "tr",
            "tur", "turkish", "türkçe"),
    SERBIAN("sr", "Serbian", "Српски", 31, "rs",
            "srp", "serbian", "српски");

    private final String code;
    private final String englishName;
    private final String nativeName;
    private final Set<String> aliases;
    private final int menuOrder;
    private final String countryCode;

    Language(String code, String englishName, String nativeName,
             int menuOrder, String countryCode, String... extraAliases) {
        this.code = code;
        this.englishName = englishName;
        this.nativeName = nativeName;
        this.menuOrder = menuOrder;
        this.countryCode = countryCode;

        LinkedHashSet<String> set = new LinkedHashSet<String>();
        set.add(code.toLowerCase(Locale.ROOT));
        set.add((code + "_" + countryCode).toLowerCase(Locale.ROOT));
        for (String alias : extraAliases) {
            set.add(alias.toLowerCase(Locale.ROOT));
        }
        this.aliases = Collections.unmodifiableSet(set);
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return nativeName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getNativeName() {
        return nativeName;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public int getMenuOrder() {
        return menuOrder;
    }

    public String getCountryCode() {
        return countryCode;
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
