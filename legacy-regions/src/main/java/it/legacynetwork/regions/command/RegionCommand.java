package it.legacynetwork.regions.command;

import it.legacynetwork.regions.LegacyRegionsPlugin;
import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionFlag;
import it.legacynetwork.regions.model.WorldRegionFlags;
import it.legacynetwork.regions.selection.RegionSelection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RegionCommand implements CommandExecutor, TabCompleter {

    private final LegacyRegionsPlugin plugin;

    public RegionCommand(LegacyRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                              String label, String[] args) {
        if (!sender.hasPermission("legacyregions.admin")) {
            sender.sendMessage(ChatColor.RED + "Permessi insufficienti.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        try {
            if ("create".equals(subcommand)) return create(sender, args);
            if ("redefine".equals(subcommand)) return redefine(sender, args);
            if ("delete".equals(subcommand)) return delete(sender, args);
            if ("list".equals(subcommand)) return list(sender);
            if ("info".equals(subcommand)) return info(sender, args);
            if ("flag".equals(subcommand)) return flag(sender, args);
            if ("priority".equals(subcommand)) return priority(sender, args);
            if ("reload".equals(subcommand)) return reload(sender);
            if ("worldflag".equals(subcommand)) return worldFlag(sender, args);
            if ("worldinfo".equals(subcommand)) return worldInfo(sender, args);
            if ("worldclear".equals(subcommand)) return worldClear(sender, args);
            if ("worldlist".equals(subcommand)) return worldList(sender);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
            return true;
        }

        sendUsage(sender, label);
        return true;
    }

    private boolean worldFlag(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage(ChatColor.RED
                    + "Uso: /legacyregion worldflag <world> <flag|*> <allow|deny|inherit>");
            return true;
        }
        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "Mondo non trovato: " + worldName);
            return true;
        }
        String flagInput = args[2];
        FlagState state = FlagState.fromString(args[3]);
        if (state == null) {
            sender.sendMessage(ChatColor.RED + "Stato non valido: " + args[3]);
            return true;
        }

        String realName = world.getName();
        String uuid = world.getUID().toString();

        List<WorldRegionFlags> current = new ArrayList<WorldRegionFlags>(
                plugin.getWorldFlagsSnapshot());
        WorldRegionFlags existing = null;
        int existingIndex = -1;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getNormalizedName()
                    .equals(WorldRegionFlags.normalizeName(realName))) {
                existing = current.get(i);
                existingIndex = i;
                break;
            }
        }

        Map<RegionFlag, FlagState> flags;
        if (existing != null) {
            flags = new EnumMap<RegionFlag, FlagState>(existing.getFlags());
        } else {
            flags = new EnumMap<RegionFlag, FlagState>(RegionFlag.class);
        }

        if ("*".equals(flagInput)) {
            for (RegionFlag flag : RegionFlag.values()) {
                if (state == FlagState.INHERIT) {
                    flags.remove(flag);
                } else {
                    flags.put(flag, state);
                }
            }
        } else {
            RegionFlag flag = RegionFlag.fromString(flagInput);
            if (flag == null) {
                sender.sendMessage(ChatColor.RED + "Flag non valido: " + flagInput);
                return true;
            }
            if (state == FlagState.INHERIT) {
                flags.remove(flag);
            } else {
                flags.put(flag, state);
            }
        }

        WorldRegionFlags updated = new WorldRegionFlags(realName, uuid, flags);

        if (updated.getFlags().isEmpty() && existing != null) {
            current.remove(existingIndex);
        } else if (existing != null) {
            current.set(existingIndex, updated);
        } else if (!updated.getFlags().isEmpty()) {
            current.add(updated);
        }

        if (plugin.saveWorldFlagsAndRebuild(current)) {
            sender.sendMessage(ChatColor.GREEN + "World flag aggiornato per " + realName);
        } else {
            sender.sendMessage(ChatColor.RED
                    + "Salvataggio fallito: nessuna modifica applicata.");
        }
        return true;
    }

    private boolean worldInfo(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED
                    + "Uso: /legacyregion worldinfo <world>");
            return true;
        }
        String worldName = args[1];
        WorldRegionFlags wf = plugin.getResolver().getWorldFlags(worldName);
        if (wf == null) {
            sender.sendMessage(ChatColor.YELLOW + "Nessun flag configurato per "
                    + worldName + ".");
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "World flags: " + wf.getWorldName());
        sender.sendMessage(ChatColor.GRAY + "UUID: " + wf.getWorldUuid());
        if (wf.getFlags().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Nessun flag esplicito.");
        } else {
            for (Map.Entry<RegionFlag, FlagState> entry : wf.getFlags().entrySet()) {
                sender.sendMessage(ChatColor.GRAY
                        + entry.getKey().getPermissionKey()
                        + ": " + entry.getValue().name());
            }
        }
        return true;
    }

    private boolean worldClear(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED
                    + "Uso: /legacyregion worldclear <world>");
            return true;
        }
        String worldName = args[1];
        List<WorldRegionFlags> current = new ArrayList<WorldRegionFlags>(
                plugin.getWorldFlagsSnapshot());
        String normalized = WorldRegionFlags.normalizeName(worldName);
        boolean found = false;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getNormalizedName().equals(normalized)) {
                current.remove(i);
                found = true;
                break;
            }
        }
        if (!found) {
            sender.sendMessage(ChatColor.RED + "Nessun flag configurato per "
                    + worldName + ".");
            return true;
        }
        if (plugin.saveWorldFlagsAndRebuild(current)) {
            sender.sendMessage(ChatColor.GREEN + "World flag rimossi per "
                    + worldName + ".");
        } else {
            sender.sendMessage(ChatColor.RED
                    + "Rimozione fallita: nessuna modifica applicata.");
        }
        return true;
    }

    private boolean worldList(CommandSender sender) {
        List<WorldRegionFlags> wf = plugin.getWorldFlagsSnapshot();
        if (wf.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW
                    + "Nessun world flag configurato.");
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "World flags: " + wf.size());
        for (WorldRegionFlags entry : wf) {
            sender.sendMessage(ChatColor.GRAY + "- " + entry.getWorldName()
                    + " (" + entry.getFlags().size() + " flag)");
        }
        return true;
    }

    private boolean create(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /legacyregion create <id>");
            return true;
        }
        String id = CuboidRegion.normalizeId(args[1]);
        if (findIndex(id) >= 0) {
            sender.sendMessage(ChatColor.RED + "La regione esiste gia'.");
            return true;
        }
        RegionSelection selection = requireSelection(player);
        CuboidRegion region = fromSelection(id, selection, 0,
                new HashMap<RegionFlag, FlagState>());

        List<CuboidRegion> updated =
                new ArrayList<CuboidRegion>(plugin.getSnapshot());
        updated.add(region);
        sendSaveResult(sender, plugin.saveAndRebuild(updated),
                "Regione '" + id + "' creata.");
        return true;
    }

    private boolean redefine(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /legacyregion redefine <id>");
            return true;
        }
        String id = CuboidRegion.normalizeId(args[1]);
        int index = findIndex(id);
        if (index < 0) {
            sender.sendMessage(ChatColor.RED + "Regione non trovata.");
            return true;
        }
        CuboidRegion current = plugin.getSnapshot().get(index);
        RegionSelection selection = requireSelection(player);
        CuboidRegion replacement = fromSelection(
                current.getId(), selection, current.getPriority(),
                current.getFlags());

        List<CuboidRegion> updated =
                new ArrayList<CuboidRegion>(plugin.getSnapshot());
        updated.set(index, replacement);
        sendSaveResult(sender, plugin.saveAndRebuild(updated),
                "Regione '" + id + "' ridefinita.");
        return true;
    }

    private boolean delete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /legacyregion delete <id>");
            return true;
        }
        String id = CuboidRegion.normalizeId(args[1]);
        int index = findIndex(id);
        if (index < 0) {
            sender.sendMessage(ChatColor.RED + "Regione non trovata.");
            return true;
        }
        List<CuboidRegion> updated =
                new ArrayList<CuboidRegion>(plugin.getSnapshot());
        updated.remove(index);
        sendSaveResult(sender, plugin.saveAndRebuild(updated),
                "Regione '" + id + "' eliminata.");
        return true;
    }

    private boolean list(CommandSender sender) {
        List<CuboidRegion> regions = plugin.getSnapshot();
        if (regions.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Nessuna regione definita.");
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "Regioni: " + regions.size());
        for (CuboidRegion region : regions) {
            sender.sendMessage(ChatColor.GRAY + "- " + region.getId()
                    + " | " + region.getWorldName()
                    + " | priorita' " + region.getPriority());
        }
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /legacyregion info <id>");
            return true;
        }
        String id = CuboidRegion.normalizeId(args[1]);
        int index = findIndex(id);
        if (index < 0) {
            sender.sendMessage(ChatColor.RED + "Regione non trovata.");
            return true;
        }
        CuboidRegion region = plugin.getSnapshot().get(index);
        sender.sendMessage(ChatColor.GREEN + region.getId());
        sender.sendMessage(ChatColor.GRAY + "Mondo: " + region.getWorldName()
                + " (" + valueOrDash(region.getWorldUuid()) + ")");
        sender.sendMessage(ChatColor.GRAY + "Min: " + region.getMinX() + ", "
                + region.getMinY() + ", " + region.getMinZ());
        sender.sendMessage(ChatColor.GRAY + "Max: " + region.getMaxX() + ", "
                + region.getMaxY() + ", " + region.getMaxZ());
        sender.sendMessage(ChatColor.GRAY + "Priorita': " + region.getPriority());
        if (region.getFlags().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Flag: nessuno");
        } else {
            for (Map.Entry<RegionFlag, FlagState> entry
                    : region.getFlags().entrySet()) {
                sender.sendMessage(ChatColor.GRAY + entry.getKey().getPermissionKey()
                        + ": " + entry.getValue().name());
            }
        }
        return true;
    }

    private boolean flag(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage(ChatColor.RED
                    + "Uso: /legacyregion flag <id> <flag> <allow|deny|inherit>");
            return true;
        }
        String id = CuboidRegion.normalizeId(args[1]);
        int index = findIndex(id);
        if (index < 0) {
            sender.sendMessage(ChatColor.RED + "Regione non trovata.");
            return true;
        }
        RegionFlag regionFlag = RegionFlag.fromString(args[2]);
        FlagState state = FlagState.fromString(args[3]);
        if (regionFlag == null) {
            sender.sendMessage(ChatColor.RED + "Flag non valido.");
            return true;
        }
        if (state == null) {
            sender.sendMessage(ChatColor.RED + "Stato non valido.");
            return true;
        }

        CuboidRegion current = plugin.getSnapshot().get(index);
        Map<RegionFlag, FlagState> flags =
                new HashMap<RegionFlag, FlagState>(current.getFlags());
        if (state == FlagState.INHERIT) {
            flags.remove(regionFlag);
        } else {
            flags.put(regionFlag, state);
        }
        CuboidRegion replacement = copy(current, current.getPriority(), flags);
        List<CuboidRegion> updated =
                new ArrayList<CuboidRegion>(plugin.getSnapshot());
        updated.set(index, replacement);
        sendSaveResult(sender, plugin.saveAndRebuild(updated),
                "Flag " + regionFlag.getPermissionKey() + " = " + state.name() + ".");
        return true;
    }

    private boolean priority(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED
                    + "Uso: /legacyregion priority <id> <numero>");
            return true;
        }
        String id = CuboidRegion.normalizeId(args[1]);
        int index = findIndex(id);
        if (index < 0) {
            sender.sendMessage(ChatColor.RED + "Regione non trovata.");
            return true;
        }
        int priority;
        try {
            priority = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED + "Priorita' non valida.");
            return true;
        }

        CuboidRegion current = plugin.getSnapshot().get(index);
        CuboidRegion replacement = copy(current, priority, current.getFlags());
        List<CuboidRegion> updated =
                new ArrayList<CuboidRegion>(plugin.getSnapshot());
        updated.set(index, replacement);
        sendSaveResult(sender, plugin.saveAndRebuild(updated),
                "Priorita' di '" + id + "' = " + priority + ".");
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (plugin.reloadRegions()) {
            sender.sendMessage(ChatColor.GREEN + "Regioni e world flag ricaricati.");
        } else {
            sender.sendMessage(ChatColor.RED
                    + "Reload fallito: configurazione precedente mantenuta.");
        }
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            throw new IllegalArgumentException(
                    "Questo comando richiede un giocatore.");
        }
        return (Player) sender;
    }

    private RegionSelection requireSelection(Player player) {
        if (!plugin.getSelectionProvider().isAvailable()) {
            throw new IllegalArgumentException(
                    "WorldEdit/FAWE non disponibile.");
        }
        RegionSelection selection =
                plugin.getSelectionProvider().getSelection(player);
        if (selection == null) {
            throw new IllegalArgumentException(
                    "Selezione WorldEdit incompleta o non valida.");
        }
        return selection;
    }

    private CuboidRegion fromSelection(String id, RegionSelection selection,
                                        int priority,
                                        Map<RegionFlag, FlagState> flags) {
        return new CuboidRegion(id,
                selection.getWorldName(), selection.getWorldUuid(),
                selection.getMinX(), selection.getMinY(), selection.getMinZ(),
                selection.getMaxX(), selection.getMaxY(), selection.getMaxZ(),
                priority, flags);
    }

    private CuboidRegion copy(CuboidRegion region, int priority,
                               Map<RegionFlag, FlagState> flags) {
        return new CuboidRegion(region.getId(),
                region.getWorldName(), region.getWorldUuid(),
                region.getMinX(), region.getMinY(), region.getMinZ(),
                region.getMaxX(), region.getMaxY(), region.getMaxZ(),
                priority, flags);
    }

    private int findIndex(String id) {
        List<CuboidRegion> regions = plugin.getSnapshot();
        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void sendSaveResult(CommandSender sender, boolean success,
                                 String successMessage) {
        if (success) {
            sender.sendMessage(ChatColor.GREEN + successMessage);
        } else {
            sender.sendMessage(ChatColor.RED
                    + "Salvataggio fallito: nessuna modifica applicata.");
        }
    }

    private String valueOrDash(String value) {
        return value == null ? "-" : value;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label
                + " create|redefine|delete|list|info|flag|priority|reload|worldflag|worldinfo|worldclear|worldlist");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (!sender.hasPermission("legacyregions.admin")) {
            return new ArrayList<String>();
        }
        if (args.length == 1) {
            return filter(args[0], Arrays.asList(
                    "create", "redefine", "delete", "list",
                    "info", "flag", "priority", "reload",
                    "worldflag", "worldinfo", "worldclear", "worldlist"));
        }
        if (args.length == 2 && needsRegion(args[0])) {
            List<String> ids = new ArrayList<String>();
            for (CuboidRegion region : plugin.getSnapshot()) {
                ids.add(region.getId());
            }
            return filter(args[1], ids);
        }
        if (args.length == 2 && needsWorldName(args[0])) {
            List<String> worlds = new ArrayList<String>();
            for (World w : Bukkit.getWorlds()) {
                worlds.add(w.getName());
            }
            return filter(args[1], worlds);
        }
        if (args.length == 3 && "worldflag".equalsIgnoreCase(args[0])) {
            List<String> result = new ArrayList<String>();
            result.add("*");
            for (RegionFlag flag : RegionFlag.values()) {
                result.add(flag.getPermissionKey());
            }
            return filter(args[2], result);
        }
        if (args.length == 4 && "worldflag".equalsIgnoreCase(args[0])) {
            return filter(args[3], Arrays.asList("allow", "deny", "inherit"));
        }
        if (args.length == 3 && "flag".equalsIgnoreCase(args[0])) {
            List<String> flags = new ArrayList<String>();
            for (RegionFlag flag : RegionFlag.values()) {
                flags.add(flag.getPermissionKey());
            }
            return filter(args[2], flags);
        }
        if (args.length == 4 && "flag".equalsIgnoreCase(args[0])) {
            return filter(args[3], Arrays.asList("allow", "deny", "inherit"));
        }
        return new ArrayList<String>();
    }

    private boolean needsRegion(String subcommand) {
        return "redefine".equalsIgnoreCase(subcommand)
                || "delete".equalsIgnoreCase(subcommand)
                || "info".equalsIgnoreCase(subcommand)
                || "flag".equalsIgnoreCase(subcommand)
                || "priority".equalsIgnoreCase(subcommand);
    }

    private boolean needsWorldName(String subcommand) {
        return "worldflag".equalsIgnoreCase(subcommand)
                || "worldinfo".equalsIgnoreCase(subcommand)
                || "worldclear".equalsIgnoreCase(subcommand);
    }

    private List<String> filter(String prefix, List<String> values) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(value);
            }
        }
        return result;
    }
}
