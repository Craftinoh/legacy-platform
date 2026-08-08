package it.legacynetwork.screenshare.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;

import java.util.UUID;

/**
 * Risoluzione della lingua di chi riceve un messaggio.
 *
 * <p>Il valore restituito dal provider viene conservato cosi' com'e'. Il
 * fallback interviene solo quando la lingua non e' determinabile: console,
 * provider assente o storage irraggiungibile.</p>
 */
public final class ScreenshareLanguageResolver {

    private final PlayerLanguageProvider provider;
    private final Language fallback;

    public ScreenshareLanguageResolver(PlayerLanguageProvider provider,
                                       Language fallback) {
        if (fallback == null) {
            throw new IllegalArgumentException("Lingua di fallback mancante");
        }
        this.provider = provider;
        this.fallback = fallback;
    }

    public Language resolve(UUID playerId) {
        if (playerId == null || provider == null) {
            return fallback;
        }
        try {
            Language resolved = provider.getLanguage(playerId);
            return resolved == null ? fallback : resolved;
        } catch (RuntimeException unavailable) {
            // Storage irraggiungibile: meglio il fallback di un errore in chat.
            return fallback;
        }
    }

    public Language getFallback() {
        return fallback;
    }

    public boolean hasProvider() {
        return provider != null;
    }
}
