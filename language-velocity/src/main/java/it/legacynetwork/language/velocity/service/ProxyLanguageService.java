package it.legacynetwork.language.velocity.service;

import com.velocitypowered.api.proxy.Player;
import it.legacynetwork.language.Language;
import it.legacynetwork.language.LanguagePreference;
import it.legacynetwork.language.LocaleLanguageResolver;
import it.legacynetwork.language.velocity.repository.FileLanguageRepository;
import it.legacynetwork.language.velocity.repository.PlayerLanguageRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ProxyLanguageService {
    private final FileLanguageRepository fileRepository;
    private final PlayerLanguageRepository databaseRepository;
    private final LocaleLanguageResolver localeResolver;
    private final String proxyId;
    private final ConcurrentMap<UUID, PlayerLanguage> cache = new ConcurrentHashMap<>();

    /** Kept for compatibility with the original file-backed tests and tooling. */
    public ProxyLanguageService(FileLanguageRepository repository,
                                LocaleLanguageResolver localeResolver) {
        this.fileRepository = repository;
        this.databaseRepository = null;
        this.localeResolver = localeResolver;
        this.proxyId = "velocity-1";
    }

    public ProxyLanguageService(PlayerLanguageRepository repository,
                                LocaleLanguageResolver localeResolver,
                                String proxyId) {
        this.fileRepository = null;
        this.databaseRepository = repository;
        this.localeResolver = localeResolver;
        this.proxyId = proxyId == null || proxyId.trim().isEmpty()
                ? "velocity-1" : proxyId;
    }

    public PlayerLanguage initialize(Player player) {
        if (databaseRepository != null) {
            try {
                Optional<PlayerLanguageRepository.LanguageState> stored =
                        databaseRepository.find(player.getUniqueId()).join();
                if (stored.isPresent()) {
                    Language language = Language.findByInput(
                                    stored.get().languageCode)
                            .orElseGet(() -> resolveClientLocale(player));
                    PlayerLanguage state = new PlayerLanguage(
                            language, LanguagePreference.MANUAL);
                    cache.put(player.getUniqueId(), state);
                    return state;
                }
            } catch (RuntimeException ignored) {
                // Fall through to the client locale. A later request can retry storage.
            }

            PlayerLanguage automatic = new PlayerLanguage(
                    resolveClientLocale(player), LanguagePreference.AUTOMATIC);
            cache.put(player.getUniqueId(), automatic);
            databaseRepository.upsertState(player.getUniqueId(),
                    automatic.language().getCode(), clientLocale(player));
            return automatic;
        }

        Optional<FileLanguageRepository.StoredPreference> stored =
                fileRepository.find(player.getUniqueId());
        PlayerLanguage state;
        if (stored.isPresent()) {
            state = new PlayerLanguage(
                    stored.get().language(), stored.get().preference());
        } else {
            state = new PlayerLanguage(
                    resolveClientLocale(player), LanguagePreference.AUTOMATIC);
        }
        cache.put(player.getUniqueId(), state);
        return state;
    }

    public PlayerLanguage current(Player player) {
        PlayerLanguage state = cache.get(player.getUniqueId());
        return state == null ? initialize(player) : state;
    }

    public CompletableFuture<PlayerLanguageRepository.ChangeResult> requestManualChange(
            Player player, Language language) {
        if (databaseRepository != null) {
            return databaseRepository.changeLanguage(
                            UUID.randomUUID(), player.getUniqueId(), language.getCode(),
                            clientLocale(player), proxyId, 5, 3, 60)
                    .thenApply(result -> {
                        if (result.status == PlayerLanguageRepository.ChangeStatus.SUCCESS
                                || result.status == PlayerLanguageRepository.ChangeStatus.ALREADY_SELECTED) {
                            cache.put(player.getUniqueId(), new PlayerLanguage(
                                    language, LanguagePreference.MANUAL));
                        }
                        return result;
                    });
        }

        PlayerLanguage state = new PlayerLanguage(language, LanguagePreference.MANUAL);
        cache.put(player.getUniqueId(), state);
        return fileRepository.save(
                        player.getUniqueId(), state.language(), state.preference())
                .thenApply(ignored -> new PlayerLanguageRepository.ChangeResult(
                        PlayerLanguageRepository.ChangeStatus.SUCCESS,
                        language.getCode(), "changed"));
    }

    public CompletableFuture<Void> setManual(Player player, Language language) {
        return requestManualChange(player, language).thenApply(result -> {
            if (result.status != PlayerLanguageRepository.ChangeStatus.SUCCESS
                    && result.status != PlayerLanguageRepository.ChangeStatus.ALREADY_SELECTED) {
                throw new CompletionException(new IllegalStateException(result.messageCode));
            }
            return null;
        });
    }

    public CompletableFuture<PlayerLanguage> setAutomatic(Player player) {
        Language resolved = resolveClientLocale(player);
        PlayerLanguage state = new PlayerLanguage(resolved, LanguagePreference.AUTOMATIC);
        cache.put(player.getUniqueId(), state);

        if (databaseRepository != null) {
            return databaseRepository.upsertState(player.getUniqueId(),
                            state.language().getCode(), clientLocale(player))
                    .thenApply(ignored -> state);
        }
        return fileRepository.save(
                        player.getUniqueId(), state.language(), state.preference())
                .thenApply(ignored -> state);
    }

    public boolean updateAutomaticLocale(Player player) {
        PlayerLanguage current = current(player);
        if (current.preference().overridesClientLocale()) {
            return false;
        }
        Language resolved = resolveClientLocale(player);
        cache.put(player.getUniqueId(),
                new PlayerLanguage(resolved, LanguagePreference.AUTOMATIC));
        if (databaseRepository != null) {
            databaseRepository.upsertState(player.getUniqueId(),
                    resolved.getCode(), clientLocale(player));
        }
        return resolved != current.language();
    }

    public void remove(Player player) {
        cache.remove(player.getUniqueId());
    }

    private Language resolveClientLocale(Player player) {
        return localeResolver.resolve(clientLocale(player));
    }

    private String clientLocale(Player player) {
        if (player.getPlayerSettings() == null
                || player.getPlayerSettings().getLocale() == null) {
            return "en_us";
        }
        return player.getPlayerSettings().getLocale().toString()
                .toLowerCase().replace('-', '_');
    }

    public record PlayerLanguage(Language language, LanguagePreference preference) {
    }
}
