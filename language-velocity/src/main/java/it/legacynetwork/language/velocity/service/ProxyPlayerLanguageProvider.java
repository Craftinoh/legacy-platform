package it.legacynetwork.language.velocity.service;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;

import java.util.Optional;
import java.util.UUID;

/**
 * Espone la lingua gia' risolta dal proxy come {@link PlayerLanguageProvider}.
 *
 * <p>Non decide nulla: legge lo stato che {@link ProxyLanguageService} ha gia'
 * calcolato da preferenza manuale, database e locale del client. Gli altri
 * plugin del proxy ottengono cosi' la lingua reale del giocatore senza
 * duplicare la risoluzione.</p>
 */
public final class ProxyPlayerLanguageProvider implements PlayerLanguageProvider {

    private final ProxyServer proxy;
    private final ProxyLanguageService languages;

    public ProxyPlayerLanguageProvider(ProxyServer proxy,
                                       ProxyLanguageService languages) {
        if (proxy == null || languages == null) {
            throw new IllegalArgumentException("Provider lingua incompleto");
        }
        this.proxy = proxy;
        this.languages = languages;
    }

    /**
     * Lingua del giocatore indicato.
     *
     * @return la lingua, oppure {@code null} se il giocatore non e' connesso
     *         o la risoluzione non e' disponibile
     */
    @Override
    public Language getLanguage(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        Optional<Player> player = proxy.getPlayer(playerId);
        if (!player.isPresent()) {
            return null;
        }
        try {
            ProxyLanguageService.PlayerLanguage current =
                    languages.current(player.get());
            return current == null ? null : current.language();
        } catch (RuntimeException unavailable) {
            // Storage momentaneamente irraggiungibile: il chiamante ricadra'
            // sul proprio fallback.
            return null;
        }
    }
}
