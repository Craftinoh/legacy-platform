package it.legacynetwork.combat.command;

import it.legacynetwork.combat.LegacyCombatPlugin;
import it.legacynetwork.combat.config.CombatConfig;
import it.legacynetwork.combat.fireball.FireballService;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class LegacyCombatCommand implements CommandExecutor, TabCompleter {

    private final LegacyCombatPlugin plugin;

    public LegacyCombatCommand(LegacyCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender)) {
                return true;
            }
            plugin.reload();
            sender.sendMessage(ChatColor.GREEN
                    + "LegacyCombat reload completato.");
            return true;
        }

        if ("debug".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender)) {
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED
                        + "Usa: /legacycombat debug <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED
                        + "Giocatore non trovato.");
                return true;
            }
            sender.sendMessage(ChatColor.YELLOW + "Debug: "
                    + plugin.isDebug());
            sender.sendMessage(ChatColor.YELLOW + "Hit enabled: "
                    + plugin.getHitConfig().isHitEnabled());
            sender.sendMessage(ChatColor.YELLOW + "Fireball enabled: "
                    + plugin.getFireballConfig().isFireballEnabled());
            return true;
        }

        if ("testfireball".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender)) {
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Solo giocatori.");
                return true;
            }
            Player player = (Player) sender;
            CombatConfig config = plugin.getFireballConfig();
            FireballService service = new FireballService(plugin);
            service.launchFireball(player,
                    config.getFireballSpeed(),
                    config.getFireballDamage(),
                    config.getFireballExplosionPower(),
                    config.isFireballBlockDamage(),
                    config.isFireballFire(),
                    config.getFireballShooterImmunityTicks());
            sender.sendMessage(ChatColor.GREEN
                    + "Fireball di test lanciata.");
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
            for (String opt : Arrays.asList("reload", "debug",
                    "testfireball")) {
                if (opt.startsWith(args[0].toLowerCase())) {
                    completions.add(opt);
                }
            }
            return completions;
        }
        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            List<String> completions = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase()
                        .startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
            return completions;
        }
        return null;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "LegacyCombat comandi:");
        sender.sendMessage(ChatColor.YELLOW
                + "/legacycombat reload");
        sender.sendMessage(ChatColor.YELLOW
                + "/legacycombat debug <player>");
        sender.sendMessage(ChatColor.YELLOW
                + "/legacycombat testfireball");
    }

    private boolean hasPermission(CommandSender sender) {
        if (!sender.hasPermission("legacycombat.admin")) {
            sender.sendMessage(ChatColor.RED + "Permesso mancante.");
            return false;
        }
        return true;
    }
}
