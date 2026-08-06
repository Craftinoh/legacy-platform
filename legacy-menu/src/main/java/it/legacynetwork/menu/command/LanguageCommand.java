package it.legacynetwork.menu.command;

import it.legacynetwork.menu.LegacyMenuPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LanguageCommand implements CommandExecutor {
    private final LegacyMenuPlugin plugin;

    public LanguageCommand(LegacyMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        Player player = (Player) sender;
        plugin.openLanguageMenu(player);
        return true;
    }
}
