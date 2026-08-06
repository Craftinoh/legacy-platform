package fr.xephi.authme.message.locale;

import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ApterisLanguageSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides which language a message must be rendered in.
 * <p>
 * Resolution order for a known recipient:
 * <ol>
 *   <li>the language currently served by the network {@code PlayerLanguageProvider};</li>
 *   <li>a language explicitly set for that player through {@link #setExplicitLanguage};</li>
 *   <li>the client locale — read live for an online player, otherwise the last one detected;</li>
 *   <li>{@code apteris-language.fallback};</li>
 *   <li>{@code settings.messagesLanguage};</li>
 *   <li>English.</li>
 * </ol>
 * The provider is queried on every request and takes precedence over any remembered client locale,
 * so a language change made through the network {@code /lang} command is picked up by the very next
 * message without a reconnect. Callers must therefore resolve the language at the moment they send
 * a message, never when they schedule one.
 * <p>
 * Messages without a recipient — console output, log lines — always use
 * {@link PluginSettings#MESSAGES_LANGUAGE}, as does every message when the per-player feature is
 * switched off, which reproduces upstream AuthMe behaviour.
 */
public class LocaleResolver implements Reloadable {

    /** Final fallback, mirroring {@code MessagePathHelper.DEFAULT_LANGUAGE}. */
    private static final String DEFAULT_LANGUAGE = "en";
    /** Upper bound of the per-player caches, to keep them from growing unbounded. */
    private static final int MAX_CACHED_ENTRIES = 1000;

    private final Settings settings;
    private final NetworkLanguageProviderHook networkProviderHook;
    private final ClientLocaleProvider clientLocaleProvider;

    /** Last client locale detected per player, used when the player is not (yet) online. */
    private final Map<UUID, String> detectedLocales = new ConcurrentHashMap<>();
    /** Languages explicitly chosen for a player, overriding the client locale. */
    private final Map<UUID, String> explicitLanguages = new ConcurrentHashMap<>();

    @Inject
    LocaleResolver(Settings settings,
                   NetworkLanguageProviderHook networkProviderHook,
                   ClientLocaleProvider clientLocaleProvider) {
        this.settings = settings;
        this.networkProviderHook = networkProviderHook;
        this.clientLocaleProvider = clientLocaleProvider;
    }

    /**
     * Returns the language to use for the given recipient.
     *
     * @param sender the recipient of the message, may be null
     * @return the language code to render the message in
     */
    public String getLanguage(CommandSender sender) {
        if (sender instanceof Player) {
            return getLanguage((Player) sender);
        }
        // Console and other senders have no personal language.
        return getConsoleLanguage();
    }

    /**
     * Returns the language to use for the given player.
     *
     * @param player the player to resolve the language of
     * @return the language code to render the message in
     */
    public String getLanguage(Player player) {
        if (player == null || !isPerPlayerEnabled()) {
            return getConsoleLanguage();
        }
        return resolve(player.getUniqueId(), player);
    }

    /**
     * Returns the language to use for a player that is identified only by its unique id, which is
     * the case before the player has joined (e.g. kick messages built in the pre-login phase).
     *
     * @param playerId the unique id of the player, may be null
     * @return the language code to render the message in
     */
    public String getLanguage(UUID playerId) {
        if (playerId == null || !isPerPlayerEnabled()) {
            return getConsoleLanguage();
        }
        return resolve(playerId, null);
    }

    /**
     * Returns the language used for messages that have no player recipient.
     *
     * @return the configured global message language
     */
    public String getConsoleLanguage() {
        String language = SupportedLanguages.normalize(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE));
        return language == null ? DEFAULT_LANGUAGE : language;
    }

    /**
     * Records a language explicitly chosen for a player. It takes precedence over the client locale
     * but not over the network provider, which remains the authority when it knows the player.
     *
     * @param playerId the unique id of the player
     * @param language the language code, or null to drop the choice
     */
    public void setExplicitLanguage(UUID playerId, String language) {
        if (playerId == null) {
            return;
        }
        String normalized = SupportedLanguages.normalize(language);
        if (normalized == null) {
            explicitLanguages.remove(playerId);
        } else {
            capAndPut(explicitLanguages, playerId, normalized);
        }
    }

    /**
     * Forgets everything remembered about a player. Called when a player leaves so that a
     * reconnecting player is resolved from scratch.
     *
     * @param playerId the unique id of the player
     */
    public void forget(UUID playerId) {
        if (playerId != null) {
            detectedLocales.remove(playerId);
            explicitLanguages.remove(playerId);
        }
    }

    private String resolve(UUID playerId, Player player) {
        // 1. Network provider (optional service), asked again on every message.
        if (settings.getProperty(ApterisLanguageSettings.USE_NETWORK_PROVIDER)) {
            String networkLanguage = SupportedLanguages.normalize(networkProviderHook.getLanguageCode(playerId));
            if (networkLanguage != null) {
                return networkLanguage;
            }
        }

        // 2. Explicit choice known to LegacyAuth.
        String explicit = explicitLanguages.get(playerId);
        if (explicit != null) {
            return explicit;
        }

        // 3. Client locale: read live when possible, otherwise the last one detected.
        if (settings.getProperty(ApterisLanguageSettings.USE_CLIENT_LOCALE)) {
            String clientLanguage = null;
            if (player != null) {
                clientLanguage = SupportedLanguages.normalize(clientLocaleProvider.getLanguageCode(player));
                if (clientLanguage != null) {
                    capAndPut(detectedLocales, playerId, clientLanguage);
                }
            }
            if (clientLanguage == null) {
                clientLanguage = detectedLocales.get(playerId);
            }
            if (clientLanguage != null) {
                return clientLanguage;
            }
        }

        // 4. Configured fallback.
        String fallback = SupportedLanguages.normalize(settings.getProperty(ApterisLanguageSettings.FALLBACK));
        if (fallback != null) {
            return fallback;
        }

        // 5. Global message language, then 6. English.
        return getConsoleLanguage();
    }

    private static void capAndPut(Map<UUID, String> cache, UUID playerId, String language) {
        if (cache.size() >= MAX_CACHED_ENTRIES) {
            cache.clear();
        }
        cache.put(playerId, language);
    }

    private boolean isPerPlayerEnabled() {
        return settings.getProperty(ApterisLanguageSettings.ENABLED)
            && settings.getProperty(ApterisLanguageSettings.PER_PLAYER_LOCALE);
    }

    @Override
    public void reload() {
        detectedLocales.clear();
        explicitLanguages.clear();
    }
}
