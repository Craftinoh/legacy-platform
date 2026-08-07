package it.legacynetwork.language.backend;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.LanguageChangeResult;
import it.legacynetwork.language.LanguageProtocol;
import it.legacynetwork.language.LanguageProtocolException;
import it.legacynetwork.language.LanguageProtocolMessage;
import it.legacynetwork.language.PlayerLanguageChangeListener;
import it.legacynetwork.language.PlayerLanguageChangeRequestService;
import it.legacynetwork.language.PlayerLanguageChangeResultListener;
import it.legacynetwork.language.PlayerLanguageEventService;
import it.legacynetwork.language.PlayerLanguageProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LanguageBackendPlugin extends JavaPlugin
        implements PlayerLanguageProvider, PlayerLanguageEventService,
        PlayerLanguageChangeRequestService {

    private final Map<UUID, LanguageState> cache =
            new ConcurrentHashMap<UUID, LanguageState>();
    private final List<PlayerLanguageChangeListener> listeners =
            new CopyOnWriteArrayList<PlayerLanguageChangeListener>();
    private final List<PlayerLanguageChangeResultListener> resultListeners =
            new CopyOnWriteArrayList<PlayerLanguageChangeResultListener>();
    private final LanguageProtocol protocol = new LanguageProtocol();
    private String fallbackLanguage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fallbackLanguage = getConfig().getString(
                "fallback-language", "en");

        getServer().getServicesManager().register(
                PlayerLanguageProvider.class,
                this,
                this,
                ServicePriority.Normal);
        getServer().getServicesManager().register(
                PlayerLanguageEventService.class,
                this,
                this,
                ServicePriority.Normal);
        getServer().getServicesManager().register(
                PlayerLanguageChangeRequestService.class,
                this,
                this,
                ServicePriority.Normal);

        getServer().getMessenger().registerOutgoingPluginChannel(
                this, LanguageProtocol.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(
                this,
                LanguageProtocol.CHANNEL,
                new LanguageMessageListener(this, protocol));

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                removeState(event.getPlayer().getUniqueId());
            }
        }, this);

        getLogger().info("LanguageBackend inizializzato sul canale "
                + LanguageProtocol.CHANNEL + ".");
    }

    @Override
    public Language getLanguage(UUID playerId) {
        LanguageState state = cache.get(playerId);
        if (state == null) {
            return Language.findByInput(fallbackLanguage)
                    .orElse(Language.ENGLISH);
        }
        return state.language;
    }

    LanguageState getState(UUID playerId) {
        return cache.get(playerId);
    }

    void applySynchronizedState(UUID playerId,
                                Language language,
                                String locale) {
        Language previous = getLanguage(playerId);
        cache.put(playerId, new LanguageState(language, locale));
        if (previous != language) {
            fireLanguageChanged(playerId, previous, language);
        }
    }

    void removeState(UUID playerId) {
        cache.remove(playerId);
    }

    @Override
    public boolean requestLanguageChange(UUID playerId,
                                         Language language) {
        if (playerId == null || language == null) {
            return false;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return false;
        }
        try {
            byte[] payload = protocol.serialize(
                    LanguageProtocolMessage.languageChangeRequest(
                            playerId, language));
            player.sendPluginMessage(
                    this, LanguageProtocol.CHANNEL, payload);
            return true;
        } catch (LanguageProtocolException exception) {
            getLogger().warning(
                    "Impossibile inviare il cambio lingua per "
                            + player.getName() + ": "
                            + exception.getMessage());
            return false;
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        cache.clear();
        listeners.clear();
        resultListeners.clear();
    }

    @Override
    public void registerListener(PlayerLanguageChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(PlayerLanguageChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void fireLanguageChanged(UUID playerId,
                                    Language previous,
                                    Language current) {
        for (PlayerLanguageChangeListener listener : listeners) {
            try {
                listener.onLanguageChanged(
                        playerId, previous, current);
            } catch (RuntimeException exception) {
                getLogger().warning(
                        "Listener cambio lingua fallito: "
                                + exception.getMessage());
            }
        }
    }

    @Override
    public void registerResultListener(
            PlayerLanguageChangeResultListener listener) {
        if (listener != null) {
            resultListeners.add(listener);
        }
    }

    @Override
    public void unregisterResultListener(
            PlayerLanguageChangeResultListener listener) {
        resultListeners.remove(listener);
    }

    @Override
    public void fireLanguageChangeResult(UUID playerId,
                                         Language requestedLanguage,
                                         LanguageChangeResult result) {
        for (PlayerLanguageChangeResultListener listener : resultListeners) {
            try {
                listener.onLanguageChangeResult(
                        playerId, requestedLanguage, result);
            } catch (RuntimeException exception) {
                getLogger().warning(
                        "Listener risultato cambio lingua fallito: "
                                + exception.getMessage());
            }
        }
    }

    static final class LanguageState {
        final Language language;
        final String locale;

        LanguageState(Language language, String locale) {
            this.language = language;
            this.locale = locale;
        }
    }
}
