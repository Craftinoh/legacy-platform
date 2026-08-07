package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.chicken.ChickenService;
import it.legacynetwork.chickenwars.config.ChickenWarsConfig;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.shop.ShopService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Servizi condivisi tra tutte le partite.
 *
 * <p>Raccogliere le dipendenze in un unico oggetto mantiene compatti i
 * costruttori e consente di sostituire la configurazione a ogni reload senza
 * ricreare le partite in corso.</p>
 */
public final class GameServices {

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final ChickenService chickens;
    private final ShopService shop;

    private volatile ChickenWarsConfig config;

    public GameServices(JavaPlugin plugin, MessageService messages,
                        ChickenService chickens, ShopService shop,
                        ChickenWarsConfig config) {
        if (plugin == null || messages == null || chickens == null
                || shop == null || config == null) {
            throw new IllegalArgumentException("Servizi ChickenWars incompleti");
        }
        this.plugin = plugin;
        this.messages = messages;
        this.chickens = chickens;
        this.shop = shop;
        this.config = config;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public MessageService getMessages() {
        return messages;
    }

    public ChickenService getChickens() {
        return chickens;
    }

    public ShopService getShop() {
        return shop;
    }

    public ChickenWarsConfig getConfig() {
        return config;
    }

    /**
     * Sostituisce la configurazione dopo un reload riuscito.
     */
    public void setConfig(ChickenWarsConfig config) {
        if (config != null) {
            this.config = config;
        }
    }
}
