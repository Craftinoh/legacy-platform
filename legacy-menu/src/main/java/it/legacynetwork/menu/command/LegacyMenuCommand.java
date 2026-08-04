package it.legacynetwork.menu.command;

import it.legacynetwork.menu.LegacyMenuPlugin;
import it.legacynetwork.menu.model.MenuDefinition;
import it.legacynetwork.menu.util.LegacyColorTranslator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LegacyMenuCommand implements CommandExecutor, TabCompleter {
    private static final String PERM_ADMIN = "legacymenu.admin";

    private final LegacyMenuPlugin plugin;

    public LegacyMenuCommand(LegacyMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "open":
                handleOpen(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "list":
                handleList(sender);
                break;
            case "debug":
                handleDebug(sender, args);
                break;
            default:
                sendUsage(sender);
                break;
        }
        return true;
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!checkPermission(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(LegacyColorTranslator.translate(
                    "&cUsa: /legacymenu open <menu> [player]"));
            return;
        }
        String menuId = args[1];
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(LegacyColorTranslator.translate(
                        "&cGiocatore non trovato: " + args[2]));
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(LegacyColorTranslator.translate(
                        "&cSpecifica un giocatore."));
                return;
            }
            target = (Player) sender;
        }
        boolean success = plugin.openMenu(target, menuId);
        if (!success && sender != target) {
            sender.sendMessage(LegacyColorTranslator.translate(
                    "&cMenu non trovato: " + menuId));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!checkPermission(sender)) {
            return;
        }
        plugin.reload();
        sender.sendMessage(LegacyColorTranslator.translate(
                "&aLegacyMenu ricaricato. &7(" + plugin.getMenus().size()
                + " menu caricati)"));
    }

    private void handleList(CommandSender sender) {
        if (!checkPermission(sender)) {
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "Menu disponibili:");
        for (String menuId : plugin.getMenus().keySet()) {
            sender.sendMessage(ChatColor.YELLOW + " - " + menuId);
        }
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (!checkPermission(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(LegacyColorTranslator.translate(
                    "&cUsa: /legacymenu debug <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(LegacyColorTranslator.translate(
                    "&cGiocatore non trovato: " + args[1]));
            return;
        }
        String lang = plugin.getLanguage(target);
        sender.sendMessage(ChatColor.GOLD + "Debug per " + target.getName() + ":");
        sender.sendMessage(ChatColor.YELLOW + "  Lingua: " + lang);
        sender.sendMessage(ChatColor.YELLOW + "  Menu caricati: "
                + plugin.getMenus().size());
        sender.sendMessage(ChatColor.YELLOW + "  PlaceholderAPI: "
                + Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"));
        sender.sendMessage(ChatColor.YELLOW + "  Debug mode: "
                + plugin.isDebug());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> completions = new ArrayList<String>();
            for (String opt : Arrays.asList("open", "reload", "list", "debug")) {
                if (opt.startsWith(args[0].toLowerCase())) {
                    completions.add(opt);
                }
            }
            return completions;
        }
        if (args.length == 2 && "open".equalsIgnoreCase(args[0])) {
            List<String> completions = new ArrayList<String>();
            for (String menuId : plugin.getMenus().keySet()) {
                if (menuId.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(menuId);
                }
            }
            return completions;
        }
        if (args.length == 3 && "open".equalsIgnoreCase(args[0])) {
            return null;
        }
        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            return null;
        }
        return Collections.emptyList();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "LegacyMenu comandi:");
        sender.sendMessage(ChatColor.YELLOW
                + "/legacymenu open <menu> [player]");
        sender.sendMessage(ChatColor.YELLOW + "/legacymenu reload");
        sender.sendMessage(ChatColor.YELLOW + "/legacymenu list");
        sender.sendMessage(ChatColor.YELLOW + "/legacymenu debug <player>");
    }

    private boolean checkPermission(CommandSender sender) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage(ChatColor.RED + "Permesso mancante.");
            return false;
        }
        return true;
    }
}
