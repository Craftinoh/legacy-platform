package it.legacynetwork.items.command;

import it.legacynetwork.items.config.LegacyItemsConfiguration;
import it.legacynetwork.items.cooldown.ItemCooldownService;
import it.legacynetwork.items.definition.CustomItemDefinition;
import it.legacynetwork.items.definition.CustomItemTrigger;
import it.legacynetwork.items.item.CustomItemGiveService;
import it.legacynetwork.items.item.CustomItemMatcher;
import it.legacynetwork.items.item.CustomItemRegistry;
import it.legacynetwork.items.language.PlayerLanguageAccessor;
import it.legacynetwork.items.message.MessageService;
import it.legacynetwork.items.LegacyItemsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LegacyItemsCommand implements CommandExecutor, TabCompleter {
    private final LegacyItemsConfiguration config;
    private final CustomItemRegistry itemRegistry;
    private final CustomItemGiveService giveService;
    private final CustomItemMatcher itemMatcher;
    private final ItemCooldownService cooldownService;
    private final PlayerLanguageAccessor languageAccessor;
    private final MessageService messageService;
    private final LegacyItemsPlugin plugin;

    public LegacyItemsCommand(LegacyItemsConfiguration config,
                               CustomItemRegistry itemRegistry,
                               CustomItemGiveService giveService,
                               CustomItemMatcher itemMatcher,
                               ItemCooldownService cooldownService,
                               PlayerLanguageAccessor languageAccessor,
                               MessageService messageService,
                               LegacyItemsPlugin plugin) {
        this.config = config;
        this.itemRegistry = itemRegistry;
        this.giveService = giveService;
        this.itemMatcher = itemMatcher;
        this.cooldownService = cooldownService;
        this.languageAccessor = languageAccessor;
        this.messageService = messageService;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                              String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                break;
            case "reload":
                if (!hasPerm(sender, "legacyitems.admin.reload")) {
                    return true;
                }
                plugin.reload();
                sendMsg(sender, "reload-success", null);
                break;
            case "list":
                if (!hasPerm(sender, "legacyitems.admin.list")) {
                    return true;
                }
                listItems(sender);
                break;
            case "give":
                if (!hasPerm(sender, "legacyitems.admin.give")) {
                    return true;
                }
                handleGive(sender, args);
                break;
            case "remove":
                if (!hasPerm(sender, "legacyitems.admin.remove")) {
                    return true;
                }
                handleRemove(sender, args);
                break;
            case "giveall":
                if (!hasPerm(sender, "legacyitems.admin.give")) {
                    return true;
                }
                handleGiveAll(sender, args);
                break;
            case "removeall":
                if (!hasPerm(sender, "legacyitems.admin.remove")) {
                    return true;
                }
                handleRemoveAll(sender, args);
                break;
            case "debug":
                if (!hasPerm(sender, "legacyitems.admin.debug")) {
                    return true;
                }
                handleDebug(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = Arrays.asList(
                    "help", "reload", "list", "give", "remove",
                    "giveall", "removeall", "debug");
            List<String> result = new ArrayList<>();
            for (String opt : opts) {
                if (opt.startsWith(args[0].toLowerCase())) {
                    result.add(opt);
                }
            }
            return result;
        }
        if (args.length == 2 && ("give".equalsIgnoreCase(args[0])
                || "remove".equalsIgnoreCase(args[0])
                || "giveall".equalsIgnoreCase(args[0])
                || "removeall".equalsIgnoreCase(args[0])
                || "debug".equalsIgnoreCase(args[0]))) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase()
                        .startsWith(args[1].toLowerCase())) {
                    players.add(p.getName());
                }
            }
            return players;
        }
        if (args.length == 3 && ("give".equalsIgnoreCase(args[0])
                || "remove".equalsIgnoreCase(args[0]))) {
            List<String> items = new ArrayList<>();
            for (CustomItemDefinition def : itemRegistry.getAll()) {
                if (def.getId().toLowerCase()
                        .startsWith(args[2].toLowerCase())) {
                    items.add(def.getId());
                }
            }
            return items;
        }
        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "LegacyItems comandi:");
        sender.sendMessage(ChatColor.YELLOW + "/legacyitems reload");
        sender.sendMessage(ChatColor.YELLOW + "/legacyitems list");
        sender.sendMessage(ChatColor.YELLOW + "/legacyitems give <player> <item>");
        sender.sendMessage(ChatColor.YELLOW + "/legacyitems remove <player> <item>");
        sender.sendMessage(ChatColor.YELLOW + "/legacyitems giveall <player>");
        sender.sendMessage(ChatColor.YELLOW + "/legacyitems removeall <player>");
        sender.sendMessage(ChatColor.YELLOW + "/legacyitems debug <player>");
    }

    private void listItems(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Oggetti configurati:");
        for (CustomItemDefinition def : itemRegistry.getAll()) {
            sender.sendMessage(ChatColor.YELLOW + "  " + def.getId()
                    + " - " + def.getMaterial()
                    + " slot:" + def.getSlot()
                    + " " + (def.isEnabled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")
                    + ChatColor.YELLOW + " triggers:" + def.getTriggers().size());
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMsg(sender, "player-not-found", null);
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMsg(sender, "player-not-found", null);
            return;
        }
        CustomItemDefinition def = itemRegistry.get(args[2]);
        if (def == null) {
            sendMsg(sender, "item-not-found", null);
            return;
        }
        giveService.giveItemNow(target, def);
        Map<String, String> extra = new HashMap<>();
        extra.put("player", target.getName());
        extra.put("item", def.getId());
        sendMsg(sender, "item-given", extra);
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMsg(sender, "player-not-found", null);
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMsg(sender, "player-not-found", null);
            return;
        }
        CustomItemDefinition def = itemRegistry.get(args[2]);
        if (def == null) {
            sendMsg(sender, "item-not-found", null);
            return;
        }
        giveService.removeCustomItems(target);
        Map<String, String> extra = new HashMap<>();
        extra.put("player", target.getName());
        extra.put("item", def.getId());
        sendMsg(sender, "item-removed", extra);
    }

    private void handleGiveAll(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMsg(sender, "player-not-found", null);
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMsg(sender, "player-not-found", null);
            return;
        }
        giveService.giveTriggeredItems(target, CustomItemTrigger.JOIN);
        sender.sendMessage(ChatColor.GREEN + "Oggetti assegnati a " + target.getName());
    }

    private void handleRemoveAll(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMsg(sender, "player-not-found", null);
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMsg(sender, "player-not-found", null);
            return;
        }
        giveService.removeCustomItems(target);
        sender.sendMessage(ChatColor.GREEN + "Oggetti rimossi da " + target.getName());
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMsg(sender, "player-not-found", null);
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMsg(sender, "player-not-found", null);
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "Debug: " + target.getName());
        sender.sendMessage(ChatColor.YELLOW + "UUID: " + target.getUniqueId());
        sender.sendMessage(ChatColor.YELLOW + "World: " + target.getWorld().getName());
        sender.sendMessage(ChatColor.YELLOW + "Language: "
                + languageAccessor.getLanguageCode(target));
        sender.sendMessage(ChatColor.YELLOW + "PAPI: "
                + (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null));
        boolean menuAvailable =
                Bukkit.getServicesManager().load(
                        it.legacynetwork.items.api.MenuService.class) != null;
        sender.sendMessage(ChatColor.YELLOW + "MenuService: " + menuAvailable);
        Map<Integer, String> slots = giveService.getSlotCache(target);
        sender.sendMessage(ChatColor.YELLOW + "Custom items: "
                + (slots != null ? slots.size() : 0));
        if (slots != null) {
            for (Map.Entry<Integer, String> entry : slots.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  slot " + (entry.getKey() + 1)
                        + " -> " + entry.getValue());
            }
        }
    }

    private void sendMsg(CommandSender sender, String key,
                          Map<String, String> extra) {
        if (extra == null) {
            extra = new HashMap<>();
        }
        String msg = messageService.getMessage(key,
                sender instanceof Player ? (Player) sender : null, extra);
        if (msg != null && !msg.isEmpty()) {
            sender.sendMessage(msg);
        }
    }

    private boolean hasPerm(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(ChatColor.RED + "Permesso mancante.");
            return false;
        }
        return true;
    }
}
