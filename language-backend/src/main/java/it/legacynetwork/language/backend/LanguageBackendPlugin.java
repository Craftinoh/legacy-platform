package it.legacynetwork.language.backend;

import it.legacynetwork.language.PlayerLanguageChangeListener;
import it.legacynetwork.language.PlayerLanguageEventService;
import it.legacynetwork.language.PlayerLanguageProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LanguageBackendPlugin extends JavaPlugin
        implements PlayerLanguageProvider, PlayerLanguageEventService {

    private final Map<UUID, LanguageState> cache = new ConcurrentHashMap<>();
    private final List<PlayerLanguageChangeListener> listeners =
            new CopyOnWriteArrayList<>();
    private String fallbackLanguage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fallbackLanguage = getConfig().getString("fallback-language", "en");

        getServer().getServicesManager().register(
                PlayerLanguageProvider.class, this, this, ServicePriority.Normal);
        getServer().getServicesManager().register(
                PlayerLanguageEventService.class, this, this, ServicePriority.Normal);

        getServer().getMessenger().registerOutgoingPluginChannel(
                this, "legacy:language");
        getServer().getMessenger().registerIncomingPluginChannel(
                this, "legacy:language",
                new LanguageMessageListener(this));

        getLogger().info("LanguageBackend inizializzato.");
    }

    @Override
    public it.legacynetwork.language.Language getLanguage(UUID playerId) {
        LanguageState state = cache.get(playerId);
        if (state == null) {
            return it.legacynetwork.language.Language.findByInput(fallbackLanguage)
                    .orElse(it.legacynetwork.language.Language.ENGLISH);
        }
        return state.language;
    }

    LanguageState getState(UUID playerId) {
        return cache.get(playerId);
    }

    void updateState(UUID playerId, it.legacynetwork.language.Language language,
                      String locale) {
        cache.put(playerId, new LanguageState(language, locale));
    }

    void removeState(UUID playerId) {
        cache.remove(playerId);
    }

    @Override
    public void onDisable() {
        cache.clear();
        listeners.clear();
    }

    @Override
    public void registerListener(PlayerLanguageChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(PlayerLanguageChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void fireLanguageChanged(UUID playerId,
                                     it.legacynetwork.language.Language previous,
                                     it.legacynetwork.language.Language current) {
        for (PlayerLanguageChangeListener listener : listeners) {
            try {
                listener.onLanguageChanged(playerId, previous, current);
            } catch (Exception ignored) {
            }
        }
    }

    static final class LanguageState {
        final it.legacynetwork.language.Language language;
        final String locale;

        LanguageState(it.legacynetwork.language.Language language, String locale) {
            this.language = language;
            this.locale = locale;
        }
    }
}
