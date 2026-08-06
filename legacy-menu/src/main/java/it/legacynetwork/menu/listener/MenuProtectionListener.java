package it.legacynetwork.menu.listener;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import it.legacynetwork.menu.LegacyMenuPlugin;
import it.legacynetwork.menu.lang.LanguageMenuHolder;
import it.legacynetwork.menu.model.MenuDefinition;
import it.legacynetwork.menu.model.MenuInventoryHolder;
import it.legacynetwork.menu.model.MenuItem;
import it.legacynetwork.menu.model.MenuItemAction;
import it.legacynetwork.menu.util.LegacyColorTranslator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

public class MenuProtectionListener implements Listener {
    private final LegacyMenuPlugin plugin;

    public MenuProtectionListener(LegacyMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInv = event.getView().getTopInventory();

        if (topInv.getHolder() instanceof LanguageMenuHolder) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                LanguageMenuHolder holder = (LanguageMenuHolder) topInv.getHolder();
                plugin.getLanguageMenuService().handleClick(player,
                        event.getSlot(), holder.getPage());
            }
            return;
        }

        if (!(topInv.getHolder() instanceof MenuInventoryHolder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked != topInv) {
            return;
        }

        int slot = event.getSlot();
        MenuInventoryHolder holder = (MenuInventoryHolder) topInv.getHolder();
        MenuDefinition menu = plugin.getMenu(holder.getMenuId());
        if (menu == null) {
            return;
        }

        Map<Integer, MenuItem> items = menu.getItems();
        if (items == null) {
            return;
        }

        MenuItem item = items.get(slot + 1);
        if (item == null) {
            return;
        }

        String lang = plugin.getLanguage(player);
        if (lang == null) {
            lang = plugin.getFallbackLanguage();
        }

        String clickType;
        if (event.isLeftClick()) {
            clickType = "LEFT";
        } else if (event.isRightClick()) {
            clickType = "RIGHT";
        } else {
            return;
        }

        executeActions(player, lang, item.getActions(), clickType);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv.getHolder() instanceof MenuInventoryHolder
                || topInv.getHolder() instanceof LanguageMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void executeActions(Player player, String lang,
                                Map<String, List<MenuItemAction>> actionMap,
                                String clickType) {
        if (actionMap == null || actionMap.isEmpty()) {
            return;
        }
        List<MenuItemAction> actions = actionMap.get(clickType);
        if (actions == null) {
            actions = actionMap.get("CLICK");
        }
        if (actions == null) {
            return;
        }
        for (MenuItemAction action : actions) {
            executeAction(player, lang, action);
        }
    }

    public void executeAction(Player player, String lang, MenuItemAction action) {
        String value = resolvePlaceholders(player, action.getValue());
        switch (action.getType().toUpperCase()) {
            case "CONNECT_SERVER":
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(value);
                player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                break;
            case "PLAYER_COMMAND":
                player.performCommand(value);
                break;
            case "CONSOLE_COMMAND":
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value);
                break;
            case "MESSAGE":
                player.sendMessage(LegacyColorTranslator.translate(value));
                break;
            case "OPEN_MENU":
                plugin.openMenu(player, value);
                break;
            case "CLOSE":
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        player.closeInventory();
                    }
                });
                break;
            case "SOUND":
                try {
                    Sound sound = Sound.valueOf(value.toUpperCase());
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException ignored) {
                    if (plugin.isDebug()) {
                        plugin.getLogger().warning("Invalid sound: " + value);
                    }
                }
                break;
            default:
                if (plugin.isDebug()) {
                    plugin.getLogger().warning("Unknown action type: "
                            + action.getType());
                }
                break;
        }
    }

    private String resolvePlaceholders(Player player, String text) {
        if (text == null) {
            return "";
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                return me.clip.placeholderapi.PlaceholderAPI
                        .setPlaceholders(player, text);
            } catch (Exception ignored) {
            }
        }
        return text;
    }
}
