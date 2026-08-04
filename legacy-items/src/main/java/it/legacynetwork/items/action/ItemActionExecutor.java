package it.legacynetwork.items.action;

import it.legacynetwork.items.definition.CustomItemAction;
import it.legacynetwork.items.definition.CustomItemActionType;
import it.legacynetwork.items.language.PlayerLanguageAccessor;
import it.legacynetwork.items.message.MessageService;
import it.legacynetwork.items.placeholder.ItemPlaceholderService;
import it.legacynetwork.items.util.LegacyColorTranslator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.List;

public final class ItemActionExecutor {
    private final JavaPlugin plugin;
    private final ItemPlaceholderService placeholderService;
    private final PlayerLanguageAccessor languageAccessor;
    private final MessageService messageService;
    private boolean menuWarningLogged;

    public ItemActionExecutor(JavaPlugin plugin,
                              ItemPlaceholderService placeholderService,
                              PlayerLanguageAccessor languageAccessor,
                              MessageService messageService) {
        this.plugin = plugin;
        this.placeholderService = placeholderService;
        this.languageAccessor = languageAccessor;
        this.messageService = messageService;
    }

    public void executeActions(Player player, List<CustomItemAction> actions) {
        for (CustomItemAction action : actions) {
            executeAction(player, action);
        }
    }

    private void executeAction(Player player, CustomItemAction action) {
        CustomItemActionType type = action.getType();
        if (type == null) {
            return;
        }
        switch (type) {
            case OPEN_MENU: {
                String menuId = resolve(player, action.getValue());
                if (!openLegacyMenu(player, menuId)) {
                    sendMessage(player, "menu-unavailable");
                }
                break;
            }
            case PLAYER_COMMAND: {
                String command = resolve(player, action.getValue());
                player.performCommand(command);
                break;
            }
            case CONSOLE_COMMAND: {
                String command = resolve(player, action.getValue());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                break;
            }
            case MESSAGE: {
                String langCode = languageAccessor.getLanguageCode(player);
                String msg = action.getTranslatedValue(langCode);
                if (msg != null && !msg.isEmpty()) {
                    player.sendMessage(LegacyColorTranslator.translate(
                            resolve(player, msg)));
                }
                break;
            }
            case CONNECT_SERVER: {
                String server = resolve(player, action.getValue());
                connectToServer(player, server);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Opens a menu through LegacyMenu's public plugin API without dispatching a
     * command and without sharing duplicated API classes across plugin
     * classloaders. LegacyMenu exposes public boolean openMenu(Player, String).
     */
    private boolean openLegacyMenu(Player player, String menuId) {
        Plugin menuPlugin = Bukkit.getPluginManager().getPlugin("LegacyMenu");
        if (menuPlugin == null || !menuPlugin.isEnabled()) {
            logMenuWarningOnce("LegacyMenu non trovato o disabilitato. OPEN_MENU non disponibile.");
            return false;
        }

        try {
            Method openMenu = menuPlugin.getClass().getMethod(
                    "openMenu", Player.class, String.class);
            Object result = openMenu.invoke(menuPlugin, player, menuId);
            return !(result instanceof Boolean) || ((Boolean) result);
        } catch (ReflectiveOperationException | LinkageError exception) {
            logMenuWarningOnce("API LegacyMenu incompatibile: "
                    + exception.getClass().getSimpleName() + ": "
                    + exception.getMessage());
            return false;
        }
    }

    private void logMenuWarningOnce(String message) {
        if (!menuWarningLogged) {
            menuWarningLogged = true;
            plugin.getLogger().warning(message);
        }
    }

    private void connectToServer(Player player, String server) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeUTF("Connect");
            output.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning(
                    "Impossibile connettere " + player.getName() + " a " + server);
        }
    }

    private String resolve(Player player, String text) {
        if (text == null) {
            return "";
        }
        String result = text;
        result = result.replace("{player}", player.getName());
        result = result.replace("{uuid}", player.getUniqueId().toString());
        result = result.replace("{world}", player.getWorld().getName());
        if (placeholderService.isAvailable()) {
            result = placeholderService.apply(player, result);
        }
        return result;
    }

    private void sendMessage(Player player, String key) {
        String msg = messageService.getMessage(key, player);
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }
}
