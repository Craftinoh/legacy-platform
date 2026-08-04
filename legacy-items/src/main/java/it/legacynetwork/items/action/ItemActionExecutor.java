package it.legacynetwork.items.action;

import it.legacynetwork.items.api.MenuService;
import it.legacynetwork.items.definition.CustomItemAction;
import it.legacynetwork.items.definition.CustomItemActionType;
import it.legacynetwork.items.language.PlayerLanguageAccessor;
import it.legacynetwork.items.message.MessageService;
import it.legacynetwork.items.placeholder.ItemPlaceholderService;
import it.legacynetwork.items.util.LegacyColorTranslator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
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
                MenuService menuService = Bukkit.getServicesManager()
                        .load(MenuService.class);
                if (menuService != null) {
                    menuService.openMenu(player, menuId);
                } else {
                    if (!menuWarningLogged) {
                        menuWarningLogged = true;
                        plugin.getLogger().warning(
                                "LegacyMenus non trovato. OPEN_MENU non disponibile.");
                    }
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
