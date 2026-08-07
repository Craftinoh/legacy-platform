package it.legacynetwork.chickenwars.command;

import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.arena.GeneratorDefinition;
import it.legacynetwork.chickenwars.arena.TeamDefinition;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.model.ArenaState;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import it.legacynetwork.chickenwars.model.TeamColor;
import it.legacynetwork.chickenwars.setup.SetupService;
import it.legacynetwork.chickenwars.world.WorldService;
import it.legacynetwork.chickenwars.world.WorldTemplate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Sottocomandi amministrativi: creazione, configurazione e controllo arene.
 *
 * <p>Le posizioni vengono lette dal punto in cui si trova l'amministratore, come
 * previsto dalla procedura di setup.</p>
 */
public final class AdminCommand {

    private static final List<String> SUBCOMMANDS = Collections.unmodifiableList(
            Arrays.asList("help", "create", "edit", "exit", "delete", "enable",
                    "disable", "save", "validate", "info", "tp", "list", "world",
                    "setworld", "setlobby", "setspectator", "setpos1", "setpos2",
                    "setbuildlimit", "setminplayers", "team", "generator",
                    "start", "stop", "reload"));

    private static final List<String> TEAM_ACTIONS = Collections.unmodifiableList(
            Arrays.asList("add", "remove", "setspawn", "setnest", "setchicken",
                    "setshop", "setupgrade"));

    private static final List<String> GENERATOR_ACTIONS = Collections.unmodifiableList(
            Arrays.asList("add", "remove", "setlevel"));

    private static final List<String> WORLD_ACTIONS = Collections.unmodifiableList(
            Arrays.asList("list", "load", "unload", "tp"));

    private static final List<String> TEMPLATES = Collections.unmodifiableList(
            Arrays.asList("void", "flat", "normal", "here"));

    private final ArenaManager arenas;
    private final GameServices services;
    private final SetupService setup;
    private final WorldService worlds;
    private final HelpService help;
    private final Runnable reloadAction;

    public AdminCommand(ArenaManager arenas, GameServices services,
                        SetupService setup, WorldService worlds,
                        HelpService help, Runnable reloadAction) {
        this.arenas = arenas;
        this.services = services;
        this.setup = setup;
        this.worlds = worlds;
        this.help = help;
        this.reloadAction = reloadAction;
    }

    /**
     * Esegue un sottocomando amministrativo.
     *
     * @param args argomenti successivi a {@code /cw admin}
     * @return sempre {@code true}: gli errori sono riportati al mittente
     */
    public boolean execute(CommandSender sender, String[] args) {
        MessageService messages = services.getMessages();
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        if ("reload".equals(action)) {
            reloadAction.run();
            return true;
        }
        if ("list".equals(action)) {
            sendArenaList(sender);
            return true;
        }
        if ("create".equals(action)) {
            handleCreate(sender, args);
            return true;
        }
        if ("exit".equals(action)) {
            handleExit(sender);
            return true;
        }
        if ("world".equals(action)) {
            handleWorld(sender, args);
            return true;
        }
        if ("help".equals(action)) {
            help.send(sender, args.length >= 2 ? args[1] : "admin");
            return true;
        }

        if (args.length < 2) {
            messages.send(sender, "command.usage",
                    "{usage}", "/cw admin " + action + " <arena>");
            return true;
        }

        ArenaDefinition arena = arenas.getDefinition(args[1]);
        if (arena == null) {
            messages.send(sender, "arena.not-found", "{arena}", args[1]);
            return true;
        }

        if ("edit".equals(action)) {
            handleEdit(sender, arena);
        } else if ("delete".equals(action)) {
            handleDelete(sender, arena);
        } else if ("enable".equals(action)) {
            handleEnable(sender, arena);
        } else if ("disable".equals(action)) {
            handleDisable(sender, arena);
        } else if ("save".equals(action)) {
            saveAndReport(sender, arena, "admin.saved");
        } else if ("validate".equals(action)) {
            handleValidate(sender, arena);
        } else if ("info".equals(action)) {
            handleInfo(sender, arena);
        } else if ("tp".equals(action)) {
            handleTeleport(sender, arena);
        } else if ("start".equals(action)) {
            handleStart(sender, arena);
        } else if ("stop".equals(action)) {
            handleStop(sender, arena);
        } else if ("setlobby".equals(action)) {
            handleSetLobby(sender, arena);
        } else if ("setspectator".equals(action)) {
            handleSetSpectator(sender, arena);
        } else if ("setpos1".equals(action)) {
            handleSetCorner(sender, arena, true);
        } else if ("setpos2".equals(action)) {
            handleSetCorner(sender, arena, false);
        } else if ("setworld".equals(action)) {
            handleSetWorld(sender, arena, args);
        } else if ("setbuildlimit".equals(action)) {
            handleSetBuildLimit(sender, arena, args);
        } else if ("setminplayers".equals(action)) {
            handleSetMinPlayers(sender, arena, args);
        } else if ("team".equals(action)) {
            handleTeam(sender, arena, args);
        } else if ("generator".equals(action)) {
            handleGenerator(sender, arena, args);
        } else {
            sendHelp(sender);
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Gestione arena
    // ------------------------------------------------------------------

    /**
     * Crea l'arena, prepara il relativo mondo e apre subito l'editor.
     *
     * <p>Il mondo viene creato se assente e caricato se gia' presente su disco,
     * quindi l'amministratore viene teletrasportato al suo interno.</p>
     */
    private void handleCreate(CommandSender sender, String[] args) {
        MessageService messages = services.getMessages();
        if (args.length < 2) {
            messages.send(sender, "command.usage", "{usage}",
                    "/cw admin create <arena> [void|flat|normal|here|<mondo>]");
            return;
        }

        String worldArgument = args.length >= 3 ? args[2] : null;
        String adoptedWorld = resolveAdoptedWorld(sender, worldArgument);
        WorldTemplate template = null;

        if (adoptedWorld == null) {
            template = resolveTemplate(worldArgument);
            if (template == null) {
                // Non e' un template, non e' "here" e non e' un mondo esistente.
                messages.send(sender, "world.unknown-target",
                        "{value}", String.valueOf(worldArgument));
                return;
            }
        }

        ArenaDefinition created = arenas.create(args[1]);
        if (created == null) {
            messages.send(sender, "admin.already-exists", "{arena}", args[1]);
            return;
        }

        World world;
        String resultKey;
        if (adoptedWorld != null) {
            world = worlds.adopt(adoptedWorld);
            resultKey = "world.adopted";
        } else {
            String worldName = worlds.worldNameFor(created.getId());
            boolean existed = worlds.folderExists(worldName);
            world = worlds.loadOrCreate(worldName, template);
            resultKey = existed ? "world.loaded" : "world.created";
        }

        if (world == null) {
            messages.send(sender, "world.create-failed",
                    "{world}", adoptedWorld == null
                            ? worlds.worldNameFor(created.getId()) : adoptedWorld);
            arenas.delete(created.getId());
            return;
        }

        created.setWorld(world.getName());
        arenas.save(created);

        messages.send(sender, "admin.created", "{arena}", created.getId());
        messages.send(sender, resultKey,
                "{world}", world.getName(),
                "{template}", worlds.getTemplate(world.getName()).name());

        if (!(sender instanceof Player)) {
            messages.send(sender, "world.created-console",
                    "{arena}", created.getId());
            return;
        }

        // L'editor viene aperto prima del teletrasporto, cosi' all'uscita
        // l'amministratore torna esattamente da dove era partito.
        Player player = (Player) sender;
        setup.enter(player, created);
        if (!player.getWorld().getName().equals(world.getName())) {
            worlds.teleport(player, world.getName());
        }
    }

    /**
     * Associa un'arena esistente a un mondo gia' presente sul server.
     */
    private void handleSetWorld(CommandSender sender, ArenaDefinition arena,
                                String[] args) {
        MessageService messages = services.getMessages();
        String requested = args.length >= 3 ? args[2] : "here";

        String worldName = resolveAdoptedWorld(sender, requested);
        if (worldName == null) {
            messages.send(sender, "world.not-found", "{world}", requested);
            return;
        }

        World world = worlds.adopt(worldName);
        if (world == null) {
            messages.send(sender, "world.create-failed", "{world}", worldName);
            return;
        }

        String previous = arena.getWorld();
        arena.setWorld(world.getName());
        arenas.save(arena);
        arenas.rebuildGame(arena.getId());

        messages.send(sender, "world.arena-bound",
                "{arena}", arena.getId(),
                "{world}", world.getName());

        // Le posizioni salvate contengono il nome del vecchio mondo: vanno
        // rifatte, altrimenti l'arena non potra' mai essere abilitata.
        if (previous != null && !previous.equalsIgnoreCase(world.getName())
                && arena.hasAnyPosition()) {
            messages.send(sender, "world.positions-outdated",
                    "{arena}", arena.getId());
        }
    }

    /**
     * Interpreta l'argomento mondo di {@code create} e {@code setworld}.
     *
     * @return il nome del mondo da adottare, oppure {@code null} se l'argomento
     *         non indica un mondo esistente
     */
    private String resolveAdoptedWorld(CommandSender sender, String argument) {
        if (argument == null) {
            return null;
        }
        String value = argument.trim();
        if (value.isEmpty()) {
            return null;
        }

        if ("here".equalsIgnoreCase(value) || "qui".equalsIgnoreCase(value)) {
            if (!(sender instanceof Player)) {
                services.getMessages().send(sender, "command.players-only");
                return null;
            }
            return ((Player) sender).getWorld().getName();
        }

        // Un template ha sempre la precedenza sul nome di un mondo omonimo.
        if (resolveTemplate(value) != null) {
            return null;
        }
        return worlds.isLoaded(value) || worlds.folderExists(value) ? value : null;
    }

    /**
     * @return il template richiesto, il predefinito se assente, {@code null}
     *         se il testo non corrisponde a una generazione creabile
     */
    private WorldTemplate resolveTemplate(String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            return worlds.getDefaultTemplate();
        }
        WorldTemplate template = WorldTemplate.fromString(argument);
        return template != null && template.isCreatable() ? template : null;
    }

    /**
     * Sottocomandi di gestione mondi.
     */
    private void handleWorld(CommandSender sender, String[] args) {
        MessageService messages = services.getMessages();
        if (args.length < 2) {
            messages.send(sender, "command.usage",
                    "{usage}", "/cw admin world <list|load|unload|tp> [mondo]");
            return;
        }

        String worldAction = args[1].toLowerCase(Locale.ROOT);

        if ("list".equals(worldAction)) {
            sendWorldList(sender);
            return;
        }

        if (args.length < 3) {
            messages.send(sender, "command.usage",
                    "{usage}", "/cw admin world " + worldAction + " <mondo>");
            return;
        }
        String worldName = args[2];

        if ("load".equals(worldAction)) {
            if (!worlds.folderExists(worldName)) {
                messages.send(sender, "world.not-found", "{world}", worldName);
                return;
            }
            World world = worlds.loadOrCreate(worldName, null);
            messages.send(sender, world == null
                            ? "world.create-failed" : "world.loaded",
                    "{world}", worldName,
                    "{template}", worlds.getTemplate(worldName).name());
            return;
        }

        if ("unload".equals(worldAction)) {
            if (!worlds.isLoaded(worldName)) {
                messages.send(sender, "world.not-loaded", "{world}", worldName);
                return;
            }
            messages.send(sender, worlds.unload(worldName, true)
                            ? "world.unloaded" : "world.unload-failed",
                    "{world}", worldName);
            return;
        }

        if ("tp".equals(worldAction)) {
            if (!(sender instanceof Player)) {
                messages.send(sender, "command.players-only");
                return;
            }
            if (!worlds.isLoaded(worldName)) {
                messages.send(sender, "world.not-loaded", "{world}", worldName);
                return;
            }
            worlds.teleport((Player) sender, worldName);
            messages.send(sender, "world.teleported", "{world}", worldName);
            return;
        }

        messages.send(sender, "command.usage",
                "{usage}", "/cw admin world <list|load|unload|tp> [mondo]");
    }

    private void sendWorldList(CommandSender sender) {
        MessageService messages = services.getMessages();
        List<String> registered = worlds.getRegisteredWorlds();
        if (registered.isEmpty()) {
            messages.send(sender, "world.none");
            return;
        }
        messages.send(sender, "world.list-header");
        for (String worldName : registered) {
            boolean loaded = worlds.isLoaded(worldName);
            messages.sendRaw(sender, "world.list-entry",
                    "{world}", worldName,
                    "{template}", worlds.getTemplate(worldName).name(),
                    "{state}", messages.get(sender,
                            loaded ? "world.state-loaded" : "world.state-unloaded"),
                    "{players}", String.valueOf(worlds.countPlayers(worldName)));
        }
    }

    /**
     * Apre l'editor guidato con gli strumenti nella barra rapida.
     */
    private void handleEdit(CommandSender sender, ArenaDefinition arena) {
        MessageService messages = services.getMessages();
        if (!(sender instanceof Player)) {
            messages.send(sender, "command.players-only");
            return;
        }
        Player player = (Player) sender;
        if (arenas.getGameOf(player) != null) {
            messages.send(sender, "setup.leave-game-first");
            return;
        }
        Game game = arenas.getGame(arena.getId());
        if (game != null && game.getPlayerCount() > 0) {
            messages.send(sender, "setup.arena-busy", "{arena}", arena.getId());
            return;
        }
        setup.enter(player, arena);
    }

    private void handleExit(CommandSender sender) {
        MessageService messages = services.getMessages();
        if (!(sender instanceof Player)) {
            messages.send(sender, "command.players-only");
            return;
        }
        Player player = (Player) sender;
        if (!setup.isEditing(player)) {
            messages.send(sender, "setup.not-editing");
            return;
        }
        setup.exit(player, true);
    }

    /**
     * Elimina la configurazione dell'arena e scarica il relativo mondo.
     *
     * <p>La cartella del mondo resta su disco: cancellarla e' una scelta
     * dell'amministratore, non un effetto collaterale del comando.</p>
     */
    private void handleDelete(CommandSender sender, ArenaDefinition arena) {
        MessageService messages = services.getMessages();
        String worldName = arena.getWorld();
        if (!arenas.delete(arena.getId())) {
            messages.send(sender, "admin.delete-failed", "{arena}", arena.getId());
            return;
        }
        messages.send(sender, "admin.deleted", "{arena}", arena.getId());

        if (worldName != null && worlds.isLoaded(worldName)) {
            worlds.unload(worldName, true);
            messages.send(sender, "world.unloaded-kept", "{world}", worldName);
        }
    }

    private void handleEnable(CommandSender sender, ArenaDefinition arena) {
        MessageService messages = services.getMessages();
        List<String> missing = arena.findMissing();
        if (!missing.isEmpty()) {
            messages.send(sender, "admin.cannot-enable");
            reportMissing(sender, missing);
            return;
        }
        arena.setEnabled(true);
        arenas.save(arena);
        Game game = arenas.rebuildGame(arena.getId());
        if (game != null) {
            game.setState(ArenaState.WAITING);
        }
        messages.send(sender, "admin.enabled", "{arena}", arena.getId());
    }

    private void handleDisable(CommandSender sender, ArenaDefinition arena) {
        arena.setEnabled(false);
        arenas.save(arena);
        Game game = arenas.getGame(arena.getId());
        if (game != null) {
            game.restart();
            game.setState(ArenaState.DISABLED);
        }
        services.getMessages().send(sender, "admin.disabled",
                "{arena}", arena.getId());
    }

    private void handleValidate(CommandSender sender, ArenaDefinition arena) {
        MessageService messages = services.getMessages();
        List<String> missing = arena.findMissing();
        if (missing.isEmpty()) {
            messages.send(sender, "admin.validate-ok", "{arena}", arena.getId());
            return;
        }
        messages.send(sender, "admin.validate-failed", "{arena}", arena.getId());
        reportMissing(sender, missing);
    }

    private void reportMissing(CommandSender sender, List<String> missing) {
        for (String entry : missing) {
            sender.sendMessage(ChatColor.RED + " ✘ " + ChatColor.GRAY + entry);
        }
    }

    private void handleInfo(CommandSender sender, ArenaDefinition arena) {
        Game game = arenas.getGame(arena.getId());
        sender.sendMessage(ChatColor.GOLD + "--- " + arena.getId() + " ---");
        sender.sendMessage(ChatColor.GRAY + "Mondo: " + ChatColor.WHITE
                + String.valueOf(arena.getWorld()));
        sender.sendMessage(ChatColor.GRAY + "Stato: " + ChatColor.WHITE
                + (game == null ? "assente" : game.getState().name()));
        sender.sendMessage(ChatColor.GRAY + "Abilitata: " + ChatColor.WHITE
                + arena.isEnabled());
        sender.sendMessage(ChatColor.GRAY + "Squadre: " + ChatColor.WHITE
                + arena.getTeams().size());
        sender.sendMessage(ChatColor.GRAY + "Generatori: " + ChatColor.WHITE
                + arena.getGenerators().size());
        sender.sendMessage(ChatColor.GRAY + "Giocatori: " + ChatColor.WHITE
                + (game == null ? 0 : game.getPlayerCount())
                + "/" + arena.getMaximumPlayers());
        sender.sendMessage(ChatColor.GRAY + "Minimo: " + ChatColor.WHITE
                + arena.getMinimumPlayers());
    }

    private void handleTeleport(CommandSender sender, ArenaDefinition arena) {
        MessageService messages = services.getMessages();
        if (!(sender instanceof Player)) {
            messages.send(sender, "command.players-only");
            return;
        }
        SimpleLocation target = arena.getLobby() != null
                ? arena.getLobby() : arena.getSpectator();
        if (target == null || target.toLocation() == null) {
            messages.send(sender, "admin.no-teleport-target");
            return;
        }
        ((Player) sender).teleport(target.toLocation());
        messages.send(sender, "admin.teleported", "{arena}", arena.getId());
    }

    private void handleStart(CommandSender sender, ArenaDefinition arena) {
        MessageService messages = services.getMessages();
        Game game = arenas.getGame(arena.getId());
        if (game == null || !game.start()) {
            messages.send(sender, "admin.start-failed", "{arena}", arena.getId());
            return;
        }
        messages.send(sender, "admin.started", "{arena}", arena.getId());
    }

    private void handleStop(CommandSender sender, ArenaDefinition arena) {
        Game game = arenas.getGame(arena.getId());
        if (game != null) {
            game.end(null);
            game.restart();
        }
        services.getMessages().send(sender, "admin.stopped",
                "{arena}", arena.getId());
    }

    // ------------------------------------------------------------------
    // Posizioni
    // ------------------------------------------------------------------

    private void handleSetLobby(CommandSender sender, ArenaDefinition arena) {
        SimpleLocation location = senderLocation(sender);
        if (location == null) {
            return;
        }
        arena.setLobby(location);
        applyWorld(arena, location);
        saveAndReport(sender, arena, "admin.lobby-set");
    }

    private void handleSetSpectator(CommandSender sender, ArenaDefinition arena) {
        SimpleLocation location = senderLocation(sender);
        if (location == null) {
            return;
        }
        arena.setSpectator(location);
        applyWorld(arena, location);
        saveAndReport(sender, arena, "admin.spectator-set");
    }

    private void handleSetCorner(CommandSender sender, ArenaDefinition arena,
                                 boolean first) {
        SimpleLocation location = senderLocation(sender);
        if (location == null) {
            return;
        }
        if (first) {
            arena.setPos1(location);
        } else {
            arena.setPos2(location);
        }
        applyWorld(arena, location);
        saveAndReport(sender, arena, first ? "admin.pos1-set" : "admin.pos2-set");
    }

    private void handleSetBuildLimit(CommandSender sender, ArenaDefinition arena,
                                     String[] args) {
        MessageService messages = services.getMessages();
        if (args.length < 3) {
            messages.send(sender, "command.usage",
                    "{usage}", "/cw admin setbuildlimit <arena> <y>");
            return;
        }
        Integer limit = parseInteger(args[2]);
        if (limit == null) {
            messages.send(sender, "command.invalid-number", "{value}", args[2]);
            return;
        }
        arena.setMaximumBuildY(limit);
        saveAndReport(sender, arena, "admin.build-limit-set");
    }

    private void handleSetMinPlayers(CommandSender sender, ArenaDefinition arena,
                                     String[] args) {
        MessageService messages = services.getMessages();
        if (args.length < 3) {
            messages.send(sender, "command.usage",
                    "{usage}", "/cw admin setminplayers <arena> <numero>");
            return;
        }
        Integer minimum = parseInteger(args[2]);
        if (minimum == null) {
            messages.send(sender, "command.invalid-number", "{value}", args[2]);
            return;
        }
        arena.setMinimumPlayers(minimum);
        saveAndReport(sender, arena, "admin.min-players-set");
    }

    // ------------------------------------------------------------------
    // Squadre
    // ------------------------------------------------------------------

    private void handleTeam(CommandSender sender, ArenaDefinition arena,
                            String[] args) {
        MessageService messages = services.getMessages();
        if (args.length < 4) {
            messages.send(sender, "command.usage",
                    "{usage}", "/cw admin team <arena> <azione> <squadra> [colore]");
            return;
        }

        String teamAction = args[2].toLowerCase(Locale.ROOT);
        String teamId = args[3].toLowerCase(Locale.ROOT);

        if ("add".equals(teamAction)) {
            if (args.length < 5) {
                messages.send(sender, "command.usage",
                        "{usage}", "/cw admin team <arena> add <squadra> <colore>");
                return;
            }
            TeamColor color = TeamColor.fromString(args[4]);
            if (color == null) {
                messages.send(sender, "admin.invalid-color", "{color}", args[4]);
                return;
            }
            if (arena.getTeam(teamId) != null) {
                messages.send(sender, "admin.team-exists", "{team}", teamId);
                return;
            }
            arena.addTeam(new TeamDefinition(teamId, color.getItalianName(), color,
                    arena.getPlayersPerTeam()));
            saveAndReport(sender, arena, "admin.team-added");
            return;
        }

        if ("remove".equals(teamAction)) {
            if (arena.removeTeam(teamId) == null) {
                messages.send(sender, "admin.team-not-found", "{team}", teamId);
                return;
            }
            saveAndReport(sender, arena, "admin.team-removed");
            return;
        }

        TeamDefinition team = arena.getTeam(teamId);
        if (team == null) {
            messages.send(sender, "admin.team-not-found", "{team}", teamId);
            return;
        }
        SimpleLocation location = senderLocation(sender);
        if (location == null) {
            return;
        }

        if ("setspawn".equals(teamAction)) {
            team.setSpawn(location);
        } else if ("setnest".equals(teamAction)) {
            team.setNest(location);
        } else if ("setchicken".equals(teamAction)) {
            team.setChicken(location);
        } else if ("setshop".equals(teamAction)) {
            team.setShop(location);
        } else if ("setupgrade".equals(teamAction)) {
            team.setUpgrades(location);
        } else {
            messages.send(sender, "command.usage",
                    "{usage}", "/cw admin team <arena> <azione> <squadra>");
            return;
        }

        applyWorld(arena, location);
        saveAndReport(sender, arena, "admin.team-location-set");
    }

    // ------------------------------------------------------------------
    // Generatori
    // ------------------------------------------------------------------

    private void handleGenerator(CommandSender sender, ArenaDefinition arena,
                                 String[] args) {
        MessageService messages = services.getMessages();
        if (args.length < 4) {
            messages.send(sender, "command.usage",
                    "{usage}", "/cw admin generator <arena> <azione> <tipo|id> [team]");
            return;
        }

        String generatorAction = args[2].toLowerCase(Locale.ROOT);

        if ("add".equals(generatorAction)) {
            ResourceType type = ResourceType.fromString(args[3]);
            if (type == null) {
                messages.send(sender, "admin.invalid-resource", "{type}", args[3]);
                return;
            }
            SimpleLocation location = senderLocation(sender);
            if (location == null) {
                return;
            }
            String teamId = args.length >= 5 ? args[4].toLowerCase(Locale.ROOT) : null;
            if (teamId != null && arena.getTeam(teamId) == null) {
                messages.send(sender, "admin.team-not-found", "{team}", teamId);
                return;
            }
            String generatorId = arena.nextGeneratorId(
                    type.name().toLowerCase(Locale.ROOT));
            arena.addGenerator(new GeneratorDefinition(generatorId, type,
                    location.centered(), teamId, 1, teamId == null));
            applyWorld(arena, location);
            saveAndReport(sender, arena, "admin.generator-added",
                    "{id}", generatorId);
            return;
        }

        if ("remove".equals(generatorAction)) {
            if (arena.removeGenerator(args[3]) == null) {
                messages.send(sender, "admin.generator-not-found", "{id}", args[3]);
                return;
            }
            saveAndReport(sender, arena, "admin.generator-removed", "{id}", args[3]);
            return;
        }

        if ("setlevel".equals(generatorAction)) {
            if (args.length < 5) {
                messages.send(sender, "command.usage", "{usage}",
                        "/cw admin generator <arena> setlevel <id> <livello>");
                return;
            }
            Integer level = parseInteger(args[4]);
            if (level == null) {
                messages.send(sender, "command.invalid-number", "{value}", args[4]);
                return;
            }
            GeneratorDefinition existing = arena.removeGenerator(args[3]);
            if (existing == null) {
                messages.send(sender, "admin.generator-not-found", "{id}", args[3]);
                return;
            }
            arena.addGenerator(existing.withLevel(level));
            saveAndReport(sender, arena, "admin.generator-level-set",
                    "{id}", args[3], "{level}", String.valueOf(level));
            return;
        }

        messages.send(sender, "command.usage",
                "{usage}", "/cw admin generator <arena> <add|remove|setlevel> ...");
    }

    // ------------------------------------------------------------------
    // Supporto
    // ------------------------------------------------------------------

    /**
     * Salva l'arena, ricrea la partita e conferma l'operazione.
     */
    private void saveAndReport(CommandSender sender, ArenaDefinition arena,
                               String messageKey, String... replacements) {
        MessageService messages = services.getMessages();
        if (!arenas.save(arena)) {
            messages.send(sender, "admin.save-failed", "{arena}", arena.getId());
            return;
        }
        arenas.rebuildGame(arena.getId());

        String[] combined = new String[replacements.length + 2];
        combined[0] = "{arena}";
        combined[1] = arena.getId();
        System.arraycopy(replacements, 0, combined, 2, replacements.length);
        messages.send(sender, messageKey, combined);
    }

    /**
     * Posizione corrente del mittente, se e' un giocatore.
     *
     * @return la posizione, oppure {@code null} dopo aver segnalato l'errore
     */
    private SimpleLocation senderLocation(CommandSender sender) {
        if (!(sender instanceof Player)) {
            services.getMessages().send(sender, "command.players-only");
            return null;
        }
        Location location = ((Player) sender).getLocation();
        return SimpleLocation.of(location);
    }

    /**
     * Allinea il mondo dell'arena a quello della posizione appena impostata.
     */
    private void applyWorld(ArenaDefinition arena, SimpleLocation location) {
        if (arena.getWorld() == null || arena.getWorld().trim().isEmpty()) {
            arena.setWorld(location.getWorld());
        }
    }

    private Integer parseInteger(String raw) {
        try {
            return Integer.valueOf(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void sendArenaList(CommandSender sender) {
        MessageService messages = services.getMessages();
        if (arenas.getDefinitions().isEmpty()) {
            messages.send(sender, "arena.none-configured");
            return;
        }
        for (ArenaDefinition definition : arenas.getDefinitions()) {
            Game game = arenas.getGame(definition.getId());
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE
                    + definition.getId() + ChatColor.GRAY + " ["
                    + (game == null ? "?" : game.getState().name()) + "] "
                    + (definition.isComplete()
                    ? ChatColor.GREEN + "completa" : ChatColor.RED + "incompleta"));
        }
    }

    private void sendHelp(CommandSender sender) {
        help.send(sender, "admin");
    }

    /**
     * Suggerimenti per i sottocomandi amministrativi.
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return ChickenWarsCommand.filter(SUBCOMMANDS,
                    args.length == 0 ? "" : args[0]);
        }
        String action = args[0].toLowerCase(Locale.ROOT);

        if ("world".equals(action)) {
            if (args.length == 2) {
                return ChickenWarsCommand.filter(WORLD_ACTIONS, args[1]);
            }
            if (args.length == 3
                    && !"list".equals(args[1].toLowerCase(Locale.ROOT))) {
                return ChickenWarsCommand.filter(
                        worlds.getRegisteredWorlds(), args[2]);
            }
            return Collections.emptyList();
        }
        if ("help".equals(action) && args.length == 2) {
            return ChickenWarsCommand.filter(help.getVisibleTopics(sender), args[1]);
        }
        if ("create".equals(action) && args.length == 3) {
            List<String> options = new ArrayList<String>(TEMPLATES);
            options.addAll(worlds.getRegisteredWorlds());
            return ChickenWarsCommand.filter(options, args[2]);
        }
        if ("setworld".equals(action) && args.length == 3) {
            List<String> options = new ArrayList<String>();
            options.add("here");
            options.addAll(worlds.getRegisteredWorlds());
            return ChickenWarsCommand.filter(options, args[2]);
        }

        if (args.length == 2) {
            return ChickenWarsCommand.filter(arenas.getArenaIds(), args[1]);
        }
        if ("team".equals(action)) {
            if (args.length == 3) {
                return ChickenWarsCommand.filter(TEAM_ACTIONS, args[2]);
            }
            if (args.length == 4) {
                return ChickenWarsCommand.filter(teamIds(args[1]), args[3]);
            }
            if (args.length == 5 && "add".equals(args[2].toLowerCase(Locale.ROOT))) {
                return ChickenWarsCommand.filter(enumNames(TeamColor.values()), args[4]);
            }
        }
        if ("generator".equals(action)) {
            if (args.length == 3) {
                return ChickenWarsCommand.filter(GENERATOR_ACTIONS, args[2]);
            }
            if (args.length == 4 && "add".equals(args[2].toLowerCase(Locale.ROOT))) {
                return ChickenWarsCommand.filter(
                        enumNames(ResourceType.values()), args[3]);
            }
            if (args.length == 5 && "add".equals(args[2].toLowerCase(Locale.ROOT))) {
                return ChickenWarsCommand.filter(teamIds(args[1]), args[4]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> teamIds(String arenaId) {
        ArenaDefinition arena = arenas.getDefinition(arenaId);
        List<String> ids = new ArrayList<String>();
        if (arena != null) {
            for (TeamDefinition team : arena.getTeams()) {
                ids.add(team.getId());
            }
        }
        return ids;
    }

    private List<String> enumNames(Enum<?>[] values) {
        List<String> names = new ArrayList<String>();
        for (Enum<?> value : values) {
            names.add(value.name());
        }
        return names;
    }
}
