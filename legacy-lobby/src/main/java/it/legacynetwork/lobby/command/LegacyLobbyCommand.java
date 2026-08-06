package it.legacynetwork.lobby.command;

import it.legacynetwork.lobby.bossbar.LegacyBossBarService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class LegacyLobbyCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final LegacyBossBarService bossBarService;
    private final Runnable reloadAction;

    public LegacyLobbyCommand(JavaPlugin plugin,
                              LegacyBossBarService bossBarService,
                              Runnable reloadAction) {
        this.plugin = plugin;
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
        if ("slot".equalsIgnoreCase(args[0])) {
            return handleSlot(sender, args);
        }
        if ("bossbar".equalsIgnoreCase(args[0])) {
            return handleBossBar(sender, args);
        }
        sendUsage(sender);
        return true;
    }

    private boolean handleSlot(CommandSender sender, String[] args) {
        if (args.length != 3 || !"set".equalsIgnoreCase(args[1])) {
            sender.sendMessage(ChatColor.RED
                    + "Usa: /legacylobby slot set <0-8>");
            return true;
        }
        if (!hasPermission(sender, "legacylobby.admin.slot")) {
            return true;
        }

        int zeroBasedSlot;
        try {
            zeroBasedSlot = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED
                    + "Slot non valido. Usa un numero tra 0 e 8.");
            return true;
        }
        if (zeroBasedSlot < 0 || zeroBasedSlot > 8) {
            sender.sendMessage(ChatColor.RED
                    + "Slot non valido. Usa un numero tra 0 e 8.");
            return true;
        }

        try {
            plugin.getConfig().set(
                    "join.selected-slot.slot", zeroBasedSlot + 1);
            plugin.saveConfig();
            reloadAction.run();
            sender.sendMessage(ChatColor.GREEN
                    + "Slot selezionato predefinito impostato a "
                    + zeroBasedSlot + ".");
        } catch (RuntimeException exception) {
            plugin.getLogger().warning(
                    "Impossibile salvare lo slot predefinito: "
                            + exception.getMessage());
            sender.sendMessage(ChatColor.RED
                    + "Salvataggio dello slot non riuscito.");
        }
        return true;
    }

    private boolean handleBossBar(CommandSender sender, String[] args) {
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
                sender.sendMessage(ChatColor.RED
                        + "Usa: /legacylobby bossbar preview <id>");
                return true;
            }
            bossBarService.startPreview((Player) sender, args[2]);
            sender.sendMessage(ChatColor.GREEN
                    + "Preview bossbar: " + args[2]);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0],
                    Arrays.asList("reload", "slot", "bossbar"));
        }
        if (args.length == 2 && "slot".equalsIgnoreCase(args[0])) {
            return filter(args[1], Arrays.asList("set"));
        }
        if (args.length == 3 && "slot".equalsIgnoreCase(args[0])
                && "set".equalsIgnoreCase(args[1])) {
            return filter(args[2], Arrays.asList(
                    "0", "1", "2", "3", "4", "5", "6", "7", "8"));
        }
        if (args.length == 2 && "bossbar".equalsIgnoreCase(args[0])) {
            return filter(args[1],
                    Arrays.asList("reload", "preview", "stop"));
        }
        if (args.length == 3 && "bossbar".equalsIgnoreCase(args[0])
                && "preview".equalsIgnoreCase(args[1])) {
            List<String> ids = new ArrayList<String>();
            for (it.legacynetwork.lobby.bossbar.BossBarDefinition bar
                    : bossBarService.getEnabledBars()) {
                ids.add(bar.getId());
            }
            return filter(args[2], ids);
        }
        return new ArrayList<String>();
    }

    private List<String> filter(String prefix, List<String> options) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(option);
            }
        }
        return result;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "LegacyLobby comandi:");
        sender.sendMessage(ChatColor.YELLOW + "/legacylobby reload");
        sender.sendMessage(ChatColor.YELLOW
                + "/legacylobby slot set <0-8>");
        sender.sendMessage(ChatColor.YELLOW
                + "/legacylobby bossbar reload|preview|stop");
    }

    private void sendBossBarUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Bossbar comandi:");
        sender.sendMessage(ChatColor.YELLOW
                + "/legacylobby bossbar reload");
        sender.sendMessage(ChatColor.YELLOW
                + "/legacylobby bossbar preview <id>");
        sender.sendMessage(ChatColor.YELLOW
                + "/legacylobby bossbar stop");
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "Permesso mancante.");
            return false;
        }
        return true;
    }
}
