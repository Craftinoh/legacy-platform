package fr.xephi.authme.message.locale;

import fr.xephi.authme.initialization.Reloadable;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Reads the locale reported by a player's client.
 * <p>
 * AuthMe is compiled against a modern Spigot API in which {@code Player#getLocale()} exists, but
 * that method was only added in Minecraft 1.12. Calling it directly would therefore compile fine
 * and then fail at runtime with a {@code NoSuchMethodError} on a 1.8.8 server. The lookup is done
 * reflectively instead, trying in order:
 * <ol>
 *   <li>{@code Player#getLocale()} — Minecraft 1.12 and later;</li>
 *   <li>{@code Player.Spigot#getLocale()} — available on Spigot/PandaSpigot 1.8.</li>
 * </ol>
 * When neither is present the provider simply yields no answer and the caller falls back to the
 * configured language.
 */
public class ClientLocaleProvider implements Reloadable {

    /** Resolution state of the reflective lookup, computed once per (re)load. */
    private enum Strategy {
        UNKNOWN, PLAYER_GET_LOCALE, SPIGOT_GET_LOCALE, UNSUPPORTED
    }

    private Strategy strategy = Strategy.UNKNOWN;
    private Method playerGetLocale;
    private Method spigotMethod;
    private Method spigotGetLocale;

    ClientLocaleProvider() {
    }

    /**
     * Returns the language code reported by the player's client, e.g. {@code "it"} for a client
     * whose locale is {@code it_IT}.
     *
     * @param player the player to read the locale of
     * @return the language code, or null when it cannot be determined
     */
    public String getLanguageCode(Player player) {
        if (player == null) {
            return null;
        }

        // The raw locale is normalized by SupportedLanguages, which is the single place that knows
        // that pt_BR must stay distinct from pt.
        return SupportedLanguages.normalize(readRawLocale(player));
    }

    private String readRawLocale(Player player) {
        try {
            switch (resolveStrategy(player)) {
                case PLAYER_GET_LOCALE:
                    return (String) playerGetLocale.invoke(player);
                case SPIGOT_GET_LOCALE:
                    return (String) spigotGetLocale.invoke(spigotMethod.invoke(player));
                default:
                    return null;
            }
        } catch (Exception | NoSuchMethodError e) {
            // Never let locale detection break message sending.
            strategy = Strategy.UNSUPPORTED;
            return null;
        }
    }

    private Strategy resolveStrategy(Player player) {
        if (strategy != Strategy.UNKNOWN) {
            return strategy;
        }

        Class<?> playerClass = player.getClass();
        try {
            playerGetLocale = playerClass.getMethod("getLocale");
            playerGetLocale.setAccessible(true);
            strategy = Strategy.PLAYER_GET_LOCALE;
            return strategy;
        } catch (Exception ignored) {
            // Not a 1.12+ server; try the Spigot 1.8 accessor below.
        }

        try {
            spigotMethod = playerClass.getMethod("spigot");
            spigotMethod.setAccessible(true);
            Object spigot = spigotMethod.invoke(player);
            spigotGetLocale = spigot.getClass().getMethod("getLocale");
            spigotGetLocale.setAccessible(true);
            strategy = Strategy.SPIGOT_GET_LOCALE;
            return strategy;
        } catch (Exception ignored) {
            strategy = Strategy.UNSUPPORTED;
            return strategy;
        }
    }

    @Override
    public void reload() {
        strategy = Strategy.UNKNOWN;
        playerGetLocale = null;
        spigotMethod = null;
        spigotGetLocale = null;
    }
}
