package it.legacynetwork.items;

import it.legacynetwork.items.action.ItemActionExecutor;
import it.legacynetwork.items.command.LegacyItemsCommand;
import it.legacynetwork.items.config.LegacyItemsConfiguration;
import it.legacynetwork.items.cooldown.ItemCooldownService;
import it.legacynetwork.items.item.CustomItemFactory;
import it.legacynetwork.items.item.CustomItemGiveService;
import it.legacynetwork.items.item.CustomItemMatcher;
import it.legacynetwork.items.item.CustomItemRegistry;
import it.legacynetwork.items.language.BukkitServicePlayerLanguageProvider;
import it.legacynetwork.items.language.DefaultPlayerLanguageProvider;
import it.legacynetwork.items.language.PlayerLanguageAccessor;
import it.legacynetwork.items.listener.InventoryProtectionListener;
import it.legacynetwork.items.listener.PlayerDropListener;
import it.legacynetwork.items.listener.PlayerInteractListener;
import it.legacynetwork.items.listener.PlayerJoinListener;
import it.legacynetwork.items.listener.PlayerQuitListener;
import it.legacynetwork.items.listener.PlayerRespawnListener;
import it.legacynetwork.items.listener.PlayerWorldChangeListener;
import it.legacynetwork.items.message.MessageService;
import it.legacynetwork.items.placeholder.ItemPlaceholderService;
import it.legacynetwork.items.placeholder.NoopItemPlaceholderService;
import it.legacynetwork.items.placeholder.PlaceholderApiItemService;
import it.legacynetwork.language.TranslationInstaller;
import it.legacynetwork.language.PlayerLanguageChangeListener;
import it.legacynetwork.language.PlayerLanguageEventService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class LegacyItemsPlugin extends JavaPlugin
        implements PlayerLanguageChangeListener {
    private LegacyItemsConfiguration configuration;
    private CustomItemRegistry itemRegistry;
    private CustomItemFactory itemFactory;
    private CustomItemGiveService giveService;
    private CustomItemMatcher itemMatcher;
    private ItemActionExecutor actionExecutor;
    private ItemCooldownService cooldownService;
    private ItemPlaceholderService placeholderService;
    private PlayerLanguageAccessor languageAccessor;
    private MessageService messageService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("items.yml", false);
        int count = TranslationInstaller.install(getDataFolder(),
                "translations", getLogger(), getClassLoader());
        getLogger().info("TranslationInstaller: " + count + " files installed.");

        try {
            configuration = LegacyItemsConfiguration.from(getConfig());
            placeholderService = initPlaceholderAPI();
            languageAccessor = new PlayerLanguageAccessor(
                    new BukkitServicePlayerLanguageProvider(),
                    new DefaultPlayerLanguageProvider(configuration.getLanguageFallback()));
            messageService = new MessageService(this, placeholderService,
                    configuration.getServerId(), configuration.getLanguageFallback());
            messageService.load();
            loadItems();
            itemMatcher = new CustomItemMatcher(itemRegistry);
            actionExecutor = new ItemActionExecutor(this, placeholderService,
                    languageAccessor, messageService);
            cooldownService = new ItemCooldownService();

            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            registerListeners();
            registerLanguageChangeListener();
            getCommand("legacyitems").setExecutor(
                    new LegacyItemsCommand(configuration, itemRegistry, giveService,
                            itemMatcher, cooldownService, languageAccessor, messageService,
                            this));
            getLogger().info("LegacyItems inizializzato.");
            getLogger().info("PlaceholderAPI: "
                    + (placeholderService.isAvailable() ? "integrata" : "non disponibile"));
        } catch (RuntimeException exception) {
            getLogger().severe("Impossibile inizializzare LegacyItems: "
                    + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public void reload() {
        reloadConfig();
        LegacyItemsConfiguration newConfig = LegacyItemsConfiguration.from(getConfig());
        this.configuration = newConfig;

        CustomItemRegistry newRegistry = loadItemsFromFile();
        if (newRegistry == null) {
            getLogger().warning("Reload fallito, configurazione invalida. "
                    + "Mantengo la configurazione precedente.");
            return;
        }
        this.itemRegistry = newRegistry;
        this.itemFactory = new CustomItemFactory(placeholderService, languageAccessor,
                configuration.getServerId());
        this.giveService = new CustomItemGiveService(this, itemRegistry, itemFactory,
                itemMatcher, configuration);
        this.itemMatcher = new CustomItemMatcher(itemRegistry);
        this.actionExecutor = new ItemActionExecutor(this, placeholderService,
                languageAccessor, messageService);
        messageService.load();

        registerLanguageChangeListener();

        if (configuration.isReloadRefreshOnlinePlayers()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                giveService.removeCustomItems(player);
                giveService.giveTriggeredItems(player,
                        it.legacynetwork.items.definition.CustomItemTrigger.JOIN);
            }
        }
        getLogger().info("LegacyItems reload completato.");
    }

    private void registerLanguageChangeListener() {
        PlayerLanguageEventService service = Bukkit.getServicesManager()
                .load(PlayerLanguageEventService.class);
        if (service != null) {
            service.registerListener(this);
            getLogger().info("PlayerLanguageEventService registrato per aggiornamento oggetti.");
        } else {
            getLogger().warning("PlayerLanguageEventService non trovato. "
                    + "Gli oggetti non si aggiorneranno al cambio lingua.");
        }
    }

    private void unregisterLanguageChangeListener() {
        PlayerLanguageEventService service = Bukkit.getServicesManager()
                .load(PlayerLanguageEventService.class);
        if (service != null) {
            service.unregisterListener(this);
        }
    }

    @Override
    public void onLanguageChanged(java.util.UUID playerId,
                                   it.legacynetwork.language.Language previous,
                                   it.legacynetwork.language.Language current) {
        if (giveService == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(this,
                    () -> giveService.rebuildOnLanguageChange(player));
        } else {
            giveService.rebuildOnLanguageChange(player);
        }
    }

    private void loadItems() {
        this.itemRegistry = loadItemsFromFile();
        this.itemFactory = new CustomItemFactory(placeholderService, languageAccessor,
                configuration.getServerId());
        this.giveService = new CustomItemGiveService(this, itemRegistry, itemFactory,
                itemMatcher != null ? itemMatcher : new CustomItemMatcher(itemRegistry),
                configuration);
    }

    private CustomItemRegistry loadItemsFromFile() {
        File itemsFile = new File(getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            saveResource("items.yml", false);
        }
        return CustomItemRegistry.load(itemsFile);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(giveService, configuration), this);
        getServer().getPluginManager().registerEvents(
                new PlayerRespawnListener(giveService), this);
        getServer().getPluginManager().registerEvents(
                new PlayerWorldChangeListener(giveService, itemRegistry, configuration),
                this);
        getServer().getPluginManager().registerEvents(
                new PlayerInteractListener(actionExecutor, cooldownService,
                        itemMatcher, itemRegistry, messageService),
                this);
        getServer().getPluginManager().registerEvents(
                new InventoryProtectionListener(itemMatcher), this);
        getServer().getPluginManager().registerEvents(
                new PlayerDropListener(itemMatcher), this);
        getServer().getPluginManager().registerEvents(
                new PlayerQuitListener(cooldownService, giveService), this);
    }

    private ItemPlaceholderService initPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI rilevata, integrazione attiva.");
            return new PlaceholderApiItemService();
        }
        getLogger().warning(
                "PlaceholderAPI non trovata, i placeholder esterni non saranno risolti.");
        return new NoopItemPlaceholderService();
    }

    @Override
    public void onDisable() {
        unregisterLanguageChangeListener();
        if (giveService != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                giveService.removeCustomItems(player);
            }
        }
        if (cooldownService != null) {
            cooldownService.clear();
        }
    }

    CustomItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    CustomItemGiveService getGiveService() {
        return giveService;
    }

    ItemCooldownService getCooldownService() {
        return cooldownService;
    }

    PlayerLanguageAccessor getLanguageAccessor() {
        return languageAccessor;
    }

    LegacyItemsConfiguration getConfiguration() {
        return configuration;
    }
}
