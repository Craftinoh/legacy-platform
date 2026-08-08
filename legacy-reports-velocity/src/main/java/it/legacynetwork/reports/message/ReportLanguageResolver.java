package it.legacynetwork.reports.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;

import java.util.UUID;

/**
 * Risoluzione della lingua di chi riceve un messaggio.
 *
 * <p>Interroga il provider reale della rete e ne conserva il risultato cosi'
 * com'e': una lingua supportata non viene mai ridotta a italiano o inglese. Il
 * fallback configurato interviene solo quando la lingua non e' determinabile —
 * console, provider assente o storage irraggiungibile.</p>
 */
public final class ReportLanguageResolver {

    private final PlayerLanguageProvider provider;
    private final Language fallback;

    public ReportLanguageResolver(PlayerLanguageProvider provider,
                                  Language fallback) {
        if (fallback == null) {
            throw new IllegalArgumentException("Lingua di fallback mancante");
        }
        this.provider = provider;
        this.fallback = fallback;
    }

    /**
     * Lingua da usare per il destinatario indicato.
     *
     * @param playerId giocatore, oppure {@code null} per la console
     * @return una lingua, mai nulla
     */
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

    /**
     * Indica se un provider reale e' collegato.
     */
    public boolean hasProvider() {
        return provider != null;
    }
}
