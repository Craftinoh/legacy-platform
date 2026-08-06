package it.legacynetwork.menu;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageChangeRequestService;
import it.legacynetwork.language.PlayerLanguageEventService;
import it.legacynetwork.language.PlayerLanguageProvider;
import it.legacynetwork.language.TranslationInstaller;
import it.legacynetwork.menu.command.LanguageCommand;
import it.legacynetwork.menu.command.LegacyMenuCommand;
import it.legacynetwork.menu.lang.FlagTextureService;
import it.legacynetwork.menu.lang.LanguageMenuService;
import it.legacynetwork.menu.listener.MenuProtectionListener;
import it.legacynetwork.menu.loader.MenuFileLoader;
import it.legacynetwork.menu.model.MenuDefinition;
import it.legacynetwork.menu.service.MenuService;
import it.legacynetwork.menu.util.LegacyColorTranslator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class LegacyMenuPlugin extends JavaPlugin implements MenuService {
    private Map<String, MenuDefinition> menus = new HashMap<String, MenuDefinition>();
    private PlayerLanguageProvider languageProvider;
    private PlayerLanguageEventService languageEventService;
    private PlayerLanguageChangeRequestService languageChangeRequestService;
    private FlagTextureService flagTextureService;
    private LanguageMenuService languageMenuService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("menus/server-selector.yml", false);
        saveResource("flag-textures.yml", false);
        int installed = TranslationInstaller.install(getDataFolder(),
                "translations", getLogger(), getClassLoader());
        getLogger().info("Traduzioni installate: " + installed + " file.");

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        loadLanguageServices();
        loadMenus();

        flagTextureService = new FlagTextureService(getDataFolder());
        languageMenuService = new LanguageMenuService(this, flagTextureService);
        if (languageEventService != null) {
            languageEventService.registerListener(languageMenuService);
        }

        getServer().getPluginManager().registerEvents(
                new MenuProtectionListener(this), this);
        getCommand("legacymenu").setExecutor(
                new LegacyMenuCommand(this));
        getCommand("lang").setExecutor(
                new LanguageCommand(this));
        getCommand("language").setExecutor(
                new LanguageCommand(this));

        getLogger().info("LegacyMenu inizializzato. " + menus.size()
                + " menu caricati.");
    }

    void loadLanguageServices() {
        languageProvider = Bukkit.getServicesManager()
                .load(PlayerLanguageProvider.class);
        languageEventService = Bukkit.getServicesManager()
                .load(PlayerLanguageEventService.class);
        languageChangeRequestService = Bukkit.getServicesManager()
                .load(PlayerLanguageChangeRequestService.class);
    }

    public PlayerLanguageProvider getLanguageProvider() {
        if (languageProvider == null) {
            loadLanguageServices();
        }
        return languageProvider;
    }

    public PlayerLanguageEventService getLanguageEventService() {
        if (languageEventService == null) {
            loadLanguageServices();
        }
        return languageEventService;
    }

    public PlayerLanguageChangeRequestService getLanguageChangeRequestService() {
        if (languageChangeRequestService == null) {
            loadLanguageServices();
        }
        return languageChangeRequestService;
    }

    void loadMenus() {
        File menuDir = new File(getDataFolder(), "menus");
        if (!menuDir.exists() && !menuDir.mkdirs()) {
            getLogger().warning("Impossibile creare la cartella menus.");
        }
        Map<String, MenuDefinition> loaded = new HashMap<String, MenuDefinition>();
        File[] files = menuDir.listFiles((directory, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    MenuDefinition definition = MenuFileLoader.load(file);
                    if (definition != null && definition.isEnabled()) {
                        loaded.put(definition.getId(), definition);
                    }
                } catch (RuntimeException exception) {
                    getLogger().warning("Errore caricamento menu " + file.getName()
                            + ": " + exception.getMessage());
                }
            }
        }
        this.menus = loaded;
    }

    public void reload() {
        if (languageEventService != null && languageMenuService != null) {
            languageEventService.unregisterListener(languageMenuService);
        }
        reloadConfig();
        languageProvider = null;
        languageEventService = null;
        languageChangeRequestService = null;
        loadLanguageServices();
        loadMenus();
        if (flagTextureService != null) {
            flagTextureService.reload();
        }
        if (languageEventService != null && languageMenuService != null) {
            languageEventService.registerListener(languageMenuService);
        }
    }

    public void openLanguageMenu(Player player) {
        if (languageMenuService != null) {
            languageMenuService.openMenu(player);
        }
    }

    public LanguageMenuService getLanguageMenuService() {
        return languageMenuService;
    }

    public FlagTextureService getFlagTextureService() {
        return flagTextureService;
    }

    public String getLanguage(Player player) {
        String fallback = getFallbackLanguage();
        PlayerLanguageProvider provider = getLanguageProvider();
        if (provider == null) {
            return fallback;
        }
        Language language = provider.getLanguage(player.getUniqueId());
        if (language == null || language.getCode() == null
                || language.getCode().trim().isEmpty()) {
            return fallback;
        }
        return language.getCode();
    }

    public String getFallbackLanguage() {
        return getConfig().getString("language.fallback", "en");
    }

    public boolean isDebug() {
        return getConfig().getBoolean("debug.enabled", false);
    }

    public MenuDefinition getMenu(String id) {
        return menus.get(id);
    }

    public Map<String, MenuDefinition> getMenus() {
        return menus;
    }

    @Override
    public boolean openMenu(Player player, String menuId) {
        MenuDefinition menu = menus.get(menuId);
        if (menu == null) {
            player.sendMessage(LegacyColorTranslator.translate(
                    "&cMenu non trovato: " + menuId));
            return false;
        }
        String language = getLanguage(player);
        if (isDebug()) {
            getLogger().info("LegacyMenu player=" + player.getName()
                    + " providerAvailable=" + (languageProvider != null)
                    + " language=" + language);
        }
        menu.open(player, language);
        return true;
    }

    @Override
    public void onDisable() {
        if (languageEventService != null && languageMenuService != null) {
            languageEventService.unregisterListener(languageMenuService);
        }
        languageProvider = null;
        languageEventService = null;
        languageChangeRequestService = null;
    }
}
