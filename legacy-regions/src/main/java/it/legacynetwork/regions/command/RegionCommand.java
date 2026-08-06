package it.legacynetwork.regions.command;

import it.legacynetwork.regions.LegacyRegionsPlugin;
import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionFlag;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RegionCommand implements CommandExecutor, TabCompleter {

    private final LegacyRegionsPlugin plugin;

    public RegionCommand(LegacyRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legacyregions.admin")) {
            sender.sendMessage(ChatColor.RED + "Permessi insufficienti.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        try {
            switch (sub) {
                case "create":
                    return handleCreate(sender, args);
                case "redefine":
                    return handleRedefine(sender, args);
                case "delete":
                    return handleDelete(sender, args);
                case "list":
                    return handleList(sender);
                case "info":
                    return handleInfo(sender, args);
                case "flag":
                    return handleFlag(sender, args);
                case "priority":
                    return handlePriority(sender, args);
                case "reload":
                    return handleReload(sender);
                default:
                    sendUsage(sender, label);
                    return true;
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Errore: " + e.getMessage());
            return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo i giocatori possono usare questo comando.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /legacyregion create <id>");
            return true;
        }
        Player player = (Player) sender;
        String id = args[1];

        List<CuboidRegion> snapshot = plugin.getSnapshot();
        for (int i = 0; i < snapshot.size(); i++) {
            if (snapshot.get(i).getId().equalsIgnoreCase(id)) {
                sender.sendMessage(ChatColor.RED + "Una regione con questo ID esiste gia'.");
                return true;
            }
        }

        int[] selection = plugin.getSelectionProvider().getSelection(player);
        if (selection == null) {
            sender.sendMessage(ChatColor.RED + "Devi prima selezionare un'area con WorldEdit.");
            return true;
        }

        World world = player.getWorld();
        CuboidRegion region = new CuboidRegion(
                id,
                world.getName(),
                world.getUID().toString(),
                selection[0], selection[1], selection[2],
                selection[3], selection[4], selection[5],
                0,
                new HashMap<RegionFlag, FlagState>());

        List<CuboidRegion> newSnapshot = new ArrayList<CuboidRegion>(snapshot);
        newSnapshot.add(region);
        plugin.saveAndRebuild(newSnapshot);
        sender.sendMessage(ChatColor.GREEN + "Regione '" + id + "' creata.");
        return true;
    }

    private boolean handleRedefine(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo i giocatori possono usare questo comando.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /legacyregion redefine <id>");
            return true;
        }
        Player player = (Player) sender;
        String id = args[1];

        List<CuboidRegion> snapshot = plugin.getSnapshot();
        CuboidRegion existing = null;
        int existingIndex = -1;
        for (int i = 0; i < snapshot.size(); i++) {
            if (snapshot.get(i).getId().equalsIgnoreCase(id)) {
                existing = snapshot.get(i);
                existingIndex = i;
                break;
            }
        }
        if (existing == null) {
            sender.sendMessage(ChatColor.RED + "Regione '" + id + "' non trovata.");
            return true;
        }

        int[] selection = plugin.getSelectionProvider().getSelection(player);
        if (selection == null) {
            sender.sendMessage(ChatColor.RED + "Devi prima selezionare un'area con WorldEdit.");
            return true;
        }

        World world = player.getWorld();
        CuboidRegion updated = new CuboidRegion(
                existing.getId(),
                world.getName(),
                world.getUID().toString(),
                selection[0], selection[1], selection[2],
                selection[3], selection[4], selection[5],
                existing.getPriority(),
                existing.getFlags());

        List<CuboidRegion> newSnapshot = new ArrayList<CuboidRegion>(snapshot);
        newSnapshot.set(existingIndex, updated);
        plugin.saveAndRebuild(newSnapshot);
        sender.sendMessage(ChatColor.GREEN + "Regione '" + id + "' ridefinita.");
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /legacyregion delete <id>");
            return true;
        }
        String id = args[1];

        List<CuboidRegion> snapshot = plugin.getSnapshot();
        CuboidRegion toRemove = null;
        for (int i = 0; i < snapshot.size(); i++) {
            if (snapshot.get(i).getId().equalsIgnoreCase(id)) {
                toRemove = snapshot.get(i);
                break;
            }
        }
        if (toRemove == null) {
            sender.sendMessage(ChatColor.RED + "Regione '" + id + "' non trovata.");
            return true;
        }

        List<CuboidRegion> newSnapshot = new ArrayList<CuboidRegion>(snapshot);
        newSnapshot.remove(toRemove);
        plugin.saveAndRebuild(newSnapshot);
        sender.sendMessage(ChatColor.GREEN + "Regione '" + id + "' eliminata.");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<CuboidRegion> snapshot = plugin.getSnapshot();
        if (snapshot.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Nessuna regione definita.");
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "Regioni (" + snapshot.size() + "):");
        for (int i = 0; i < snapshot.size(); i++) {
            CuboidRegion region = snapshot.get(i);
            sender.sendMessage(ChatColor.GRAY + " - " + region.getId()
                    + " (priorita': " + region.getPriority() + ", mondo: "
                    + (region.getWorldName() != null ? region.getWorldName() : "tutti") + ")");
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /legacyregion info <id>");
            return true;
        }
        String id = args[1];

        List<CuboidRegion> snapshot = plugin.getSnapshot();
        CuboidRegion region = null;
        for (int i = 0; i < snapshot.size(); i++) {
            if (snapshot.get(i).getId().equalsIgnoreCase(id)) {
                region = snapshot.get(i);
                break;
            }
        }
        if (region == null) {
            sender.sendMessage(ChatColor.RED + "Regione '" + id + "' non trovata.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "=== " + region.getId() + " ===");
        sender.sendMessage(ChatColor.GRAY + "Mondo: " + (region.getWorldName() != null ? region.getWorldName() : "tutti"));
        sender.sendMessage(ChatColor.GRAY + "UUID Mondo: " + (region.getWorldUuid() != null ? region.getWorldUuid() : "N/A"));
        sender.sendMessage(ChatColor.GRAY + "Da: (" + region.getMinX() + ", " + region.getMinY() + ", " + region.getMinZ() + ")");
        sender.sendMessage(ChatColor.GRAY + "A: (" + region.getMaxX() + ", " + region.getMaxY() + ", " + region.getMaxZ() + ")");
        sender.sendMessage(ChatColor.GRAY + "Priorita': " + region.getPriority());
        sender.sendMessage(ChatColor.GRAY + "Flags:");
        Map<RegionFlag, FlagState> flags = region.getFlags();
        if (flags.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  (nessuna)");
        } else {
            for (Map.Entry<RegionFlag, FlagState> entry : flags.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey().getPermissionKey() + ": " + entry.getValue().name());
            }
        }
        return true;
    }

    private boolean handleFlag(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Uso: /legacyregion flag <id> <flag> <allow|deny|inherit>");
            return true;
        }
        String id = args[1];
        String flagName = args[2];
        String stateName = args[3];

        RegionFlag flag = RegionFlag.fromString(flagName);
        if (flag == null) {
            sender.sendMessage(ChatColor.RED + "Flag non valido: " + flagName);
            return true;
        }
        FlagState state = FlagState.fromString(stateName);
        if (state == null) {
            sender.sendMessage(ChatColor.RED + "Stato non valido: " + stateName + ". Usa ALLOW, DENY o INHERIT.");
            return true;
        }

        List<CuboidRegion> snapshot = plugin.getSnapshot();
        CuboidRegion existing = null;
        int existingIndex = -1;
        for (int i = 0; i < snapshot.size(); i++) {
            if (snapshot.get(i).getId().equalsIgnoreCase(id)) {
                existing = snapshot.get(i);
                existingIndex = i;
                break;
            }
        }
        if (existing == null) {
            sender.sendMessage(ChatColor.RED + "Regione '" + id + "' non trovata.");
            return true;
        }

        Map<RegionFlag, FlagState> newFlags = new HashMap<RegionFlag, FlagState>(existing.getFlags());
        if (state == FlagState.INHERIT) {
            newFlags.remove(flag);
        } else {
            newFlags.put(flag, state);
        }

        CuboidRegion updated = new CuboidRegion(
                existing.getId(),
                existing.getWorldName(),
                existing.getWorldUuid(),
                existing.getMinX(), existing.getMinY(), existing.getMinZ(),
                existing.getMaxX(), existing.getMaxY(), existing.getMaxZ(),
                existing.getPriority(),
                newFlags);

        List<CuboidRegion> newSnapshot = new ArrayList<CuboidRegion>(snapshot);
        newSnapshot.set(existingIndex, updated);
        plugin.saveAndRebuild(newSnapshot);
        sender.sendMessage(ChatColor.GREEN + "Flag '" + flag.getPermissionKey() + "' impostato a " + state.name() + " per la regione '" + id + "'.");
        return true;
    }

    private boolean handlePriority(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /legacyregion priority <id> <numero>");
            return true;
        }
        String id = args[1];
        int newPriority;
        try {
            newPriority = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Priorita' non valida: " + args[2]);
            return true;
        }

        List<CuboidRegion> snapshot = plugin.getSnapshot();
        CuboidRegion existing = null;
        int existingIndex = -1;
        for (int i = 0; i < snapshot.size(); i++) {
            if (snapshot.get(i).getId().equalsIgnoreCase(id)) {
                existing = snapshot.get(i);
                existingIndex = i;
                break;
            }
        }
        if (existing == null) {
            sender.sendMessage(ChatColor.RED + "Regione '" + id + "' non trovata.");
            return true;
        }

        CuboidRegion updated = new CuboidRegion(
                existing.getId(),
                existing.getWorldName(),
                existing.getWorldUuid(),
                existing.getMinX(), existing.getMinY(), existing.getMinZ(),
                existing.getMaxX(), existing.getMaxY(), existing.getMaxZ(),
                newPriority,
                existing.getFlags());

        List<CuboidRegion> newSnapshot = new ArrayList<CuboidRegion>(snapshot);
        newSnapshot.set(existingIndex, updated);
        plugin.saveAndRebuild(newSnapshot);
        sender.sendMessage(ChatColor.GREEN + "Priorita' della regione '" + id + "' impostata a " + newPriority + ".");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadRegions();
        sender.sendMessage(ChatColor.GREEN + "Regioni ricaricate.");
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "=== LegacyRegions ===");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " create <id> - Crea una regione dalla selezione WE");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " redefine <id> - Ridefinisce l'area di una regione esistente");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " delete <id> - Elimina una regione");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " list - Elenca tutte le regioni");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " info <id> - Mostra informazioni su una regione");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " flag <id> <flag> <allow|deny|inherit> - Imposta un flag");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " priority <id> <num> - Imposta la priorita'");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " reload - Ricarica le regioni dal file");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<String>();

        if (args.length == 1) {
            String[] subs = {"create", "redefine", "delete", "list", "info", "flag", "priority", "reload"};
            String prefix = args[0].toLowerCase();
            for (int i = 0; i < subs.length; i++) {
                if (subs[i].startsWith(prefix)) {
                    completions.add(subs[i]);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("info") || sub.equals("delete") || sub.equals("redefine")
                    || sub.equals("flag") || sub.equals("priority")) {
                List<CuboidRegion> snapshot = plugin.getSnapshot();
                String prefix = args[1].toLowerCase();
                for (int i = 0; i < snapshot.size(); i++) {
                    String regionId = snapshot.get(i).getId();
                    if (regionId.toLowerCase().startsWith(prefix)) {
                        completions.add(regionId);
                    }
                }
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            String prefix = args[2].toLowerCase();
            for (RegionFlag flag : RegionFlag.values()) {
                String key = flag.getPermissionKey();
                if (key.startsWith(prefix)) {
                    completions.add(key);
                }
            }
            return completions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("flag")) {
            String prefix = args[3].toLowerCase();
            String[] states = {"allow", "deny", "inherit"};
            for (int i = 0; i < states.length; i++) {
                if (states[i].startsWith(prefix)) {
                    completions.add(states[i]);
                }
            }
            return completions;
        }

        return completions;
    }
}
