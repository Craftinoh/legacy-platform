package it.legacynetwork.menu;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;
import it.legacynetwork.menu.command.LegacyMenuCommand;
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
    private Map<String, MenuDefinition> menus = new HashMap<>();
    private PlayerLanguageProvider languageProvider;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("menus/server-selector.yml", false);
        loadLanguageProvider();
        loadMenus();

        getServer().getPluginManager().registerEvents(
                new MenuProtectionListener(this), this);
        getCommand("legacymenu").setExecutor(
                new LegacyMenuCommand(this));

        getLogger().info("LegacyMenu inizializzato. " + menus.size() + " menu caricati.");
    }

    void loadLanguageProvider() {
        languageProvider = Bukkit.getServicesManager()
                .load(PlayerLanguageProvider.class);
    }

    void loadMenus() {
        File menuDir = new File(getDataFolder(), "menus");
        if (!menuDir.exists()) {
            menuDir.mkdirs();
        }
        Map<String, MenuDefinition> loaded = new HashMap<>();
        File[] files = menuDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    MenuDefinition def = MenuFileLoader.load(file);
                    if (def != null && def.isEnabled()) {
                        loaded.put(def.getId(), def);
                    }
                } catch (Exception e) {
                    getLogger().warning("Errore caricamento menu: " + file.getName());
                }
            }
        }
        this.menus = loaded;
    }

    public void reload() {
        reloadConfig();
        loadLanguageProvider();
        loadMenus();
    }

    public String getLanguage(Player player) {
        if (languageProvider != null) {
            return languageProvider.getLanguage(player.getUniqueId()).getCode();
        }
        return "en";
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
        String lang = getLanguage(player);
        if (lang == null) {
            lang = getFallbackLanguage();
        }
        menu.open(player, lang);
        return true;
    }

    @Override
    public void onDisable() {
    }
}
