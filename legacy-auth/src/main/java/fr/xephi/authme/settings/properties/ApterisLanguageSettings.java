package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

/**
 * Settings of the Apteris per-player language fork.
 * <p>
 * All properties have safe defaults: when the keys are absent from an existing config.yml,
 * the values below are used and the plugin behaves exactly like upstream AuthMe, i.e. every
 * message is resolved with {@link PluginSettings#MESSAGES_LANGUAGE}.
 */
public final class ApterisLanguageSettings implements SettingsHolder {

    @Comment({
        "Resolve messages with the language of each recipient instead of using",
        "settings.messagesLanguage for everybody.",
        "When false, AuthMe behaves exactly like upstream."
    })
    public static final Property<Boolean> PER_PLAYER_LOCALE =
        newProperty("settings.perPlayerLocale", true);

    @Comment({
        "Master switch of the Apteris language integration.",
        "When false, no language lookup is performed at all."
    })
    public static final Property<Boolean> ENABLED =
        newProperty("apteris-language.enabled", true);

    @Comment({
        "Ask the network PlayerLanguageProvider service (e.g. LegacyLobby) for the",
        "language of a player. The service is optional: when it is not registered,",
        "AuthMe silently falls back to the next source."
    })
    public static final Property<Boolean> USE_NETWORK_PROVIDER =
        newProperty("apteris-language.use-network-provider", true);

    @Comment({
        "Use the locale reported by the client when the network provider has no answer.",
        "Read through 1.8.8-compatible API, so it also works on PandaSpigot/CraftBukkit 1.8.8."
    })
    public static final Property<Boolean> USE_CLIENT_LOCALE =
        newProperty("apteris-language.use-client-locale", true);

    @Comment({
        "Language used when neither the network provider nor the client locale give an answer.",
        "When this language has no message file either, AuthMe falls back to English."
    })
    public static final Property<String> FALLBACK =
        newProperty("apteris-language.fallback", "en");

    private ApterisLanguageSettings() {
    }
}
