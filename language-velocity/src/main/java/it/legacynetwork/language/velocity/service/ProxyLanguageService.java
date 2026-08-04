package it.legacynetwork.language.velocity.service;

import com.velocitypowered.api.proxy.Player;
import it.legacynetwork.language.Language;
import it.legacynetwork.language.LanguagePreference;
import it.legacynetwork.language.LocaleLanguageResolver;
import it.legacynetwork.language.velocity.repository.FileLanguageRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ProxyLanguageService {
    private final FileLanguageRepository repository;
    private final LocaleLanguageResolver localeResolver;
    private final ConcurrentMap<UUID, PlayerLanguage> cache = new ConcurrentHashMap<>();

    public ProxyLanguageService(FileLanguageRepository repository,
                                LocaleLanguageResolver localeResolver) {
        this.repository = repository;
        this.localeResolver = localeResolver;
    }

    public PlayerLanguage initialize(Player player) {
        Optional<FileLanguageRepository.StoredPreference> stored =
                repository.find(player.getUniqueId());
        PlayerLanguage state;
        if (stored.isPresent()) {
            state = new PlayerLanguage(
                    stored.get().language(), stored.get().preference());
        } else {
            String locale = player.getPlayerSettings().getLocale().toString();
            state = new PlayerLanguage(
                    localeResolver.resolve(locale), LanguagePreference.AUTOMATIC);
        }
        cache.put(player.getUniqueId(), state);
        return state;
    }

    public PlayerLanguage current(Player player) {
        PlayerLanguage state = cache.get(player.getUniqueId());
        return state == null ? initialize(player) : state;
    }

    public CompletableFuture<Void> setManual(Player player, Language language) {
        PlayerLanguage state = new PlayerLanguage(language, LanguagePreference.MANUAL);
        cache.put(player.getUniqueId(), state);
        return repository.save(
                player.getUniqueId(), state.language(), state.preference());
    }

    public boolean updateAutomaticLocale(Player player) {
        PlayerLanguage current = current(player);
        if (current.preference().overridesClientLocale()) {
            return false;
        }
        Language resolved =
                localeResolver.resolve(player.getPlayerSettings().getLocale().toString());
        cache.put(player.getUniqueId(),
                new PlayerLanguage(resolved, LanguagePreference.AUTOMATIC));
        return resolved != current.language();
    }

    public void remove(Player player) {
        cache.remove(player.getUniqueId());
    }

    public record PlayerLanguage(Language language, LanguagePreference preference) {
    }
}
