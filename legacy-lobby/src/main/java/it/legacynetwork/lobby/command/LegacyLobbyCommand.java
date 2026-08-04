package it.legacynetwork.lobby.command;

import it.legacynetwork.lobby.bossbar.LegacyBossBarService;
import it.legacynetwork.lobby.config.LobbyConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class LegacyLobbyCommand implements CommandExecutor, TabCompleter {
    private final LobbyConfiguration configuration;
    private final LegacyBossBarService bossBarService;
    private final Runnable reloadAction;

    public LegacyLobbyCommand(LobbyConfiguration configuration,
                              LegacyBossBarService bossBarService,
                              Runnable reloadAction) {
        this.configuration = configuration;
        this.bossBarService = bossBarService;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender, "legacylobby.admin.reload")) {
                return true;
            }
            reloadAction.run();
            sender.sendMessage(ChatColor.GREEN + "LegacyLobby reload completato.");
            return true;
        }
        if ("bossbar".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sendBossBarUsage(sender);
                return true;
            }
            if ("reload".equalsIgnoreCase(args[1])) {
                if (!hasPermission(sender, "legacylobby.admin.bossbar")) {
                    return true;
                }
                reloadAction.run();
                sender.sendMessage(ChatColor.GREEN + "Bossbar ricaricata.");
                return true;
            }
            if ("preview".equalsIgnoreCase(args[1])) {
                if (!hasPermission(sender, "legacylobby.admin.bossbar")) {
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Solo giocatori.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usa: /legacylobby bossbar preview <id>");
                    return true;
                }
                bossBarService.startPreview((Player) sender, args[2]);
                sender.sendMessage(ChatColor.GREEN + "Preview bossbar: " + args[2]);
                return true;
            }
            if ("stop".equalsIgnoreCase(args[1])) {
                if (!hasPermission(sender, "legacylobby.admin.bossbar")) {
                    return true;
                }
                bossBarService.stopPreview();
                sender.sendMessage(ChatColor.GREEN + "Preview bossbar fermata.");
                return true;
            }
            sendBossBarUsage(sender);
            return true;
        }
        sendUsage(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (String opt : Arrays.asList("reload", "bossbar")) {
                if (opt.startsWith(args[0].toLowerCase())) {
                    completions.add(opt);
                }
            }
            return completions;
        }
        if (args.length == 2 && "bossbar".equalsIgnoreCase(args[0])) {
            List<String> completions = new ArrayList<>();
            for (String opt : Arrays.asList("reload", "preview", "stop")) {
                if (opt.startsWith(args[1].toLowerCase())) {
                    completions.add(opt);
                }
            }
            return completions;
        }
        if (args.length == 3 && "bossbar".equalsIgnoreCase(args[0])
                && "preview".equalsIgnoreCase(args[1])) {
            List<String> completions = new ArrayList<>();
            for (it.legacynetwork.lobby.bossbar.BossBarDefinition bar
                    : bossBarService.getEnabledBars()) {
                if (bar.getId().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(bar.getId());
                }
            }
            return completions;
        }
        return null;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "LegacyLobby comandi:");
        sender.sendMessage(ChatColor.YELLOW + "/legacylobby reload");
        sender.sendMessage(ChatColor.YELLOW + "/legacylobby bossbar reload|preview|stop");
    }

    private void sendBossBarUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Bossbar comandi:");
        sender.sendMessage(ChatColor.YELLOW + "/legacylobby bossbar reload");
        sender.sendMessage(ChatColor.YELLOW + "/legacylobby bossbar preview <id>");
        sender.sendMessage(ChatColor.YELLOW + "/legacylobby bossbar stop");
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "Permesso mancante.");
            return false;
        }
        return true;
    }
}
