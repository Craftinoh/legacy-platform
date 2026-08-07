package it.legacynetwork.chickenwars.command;

import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.game.GameTeam;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.model.ArenaState;
import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.mode.ModeProfileRegistry;
import it.legacynetwork.chickenwars.persistence.QuickBuyPresetRecord;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.shop.QuickBuyService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Comando principale destinato ai giocatori.
 *
 * <p>Le operazioni amministrative sono delegate a {@link AdminCommand} tramite
 * il sottocomando {@code admin}.</p>
 */
public final class ChickenWarsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PLAYER_SUBCOMMANDS = Collections.unmodifiableList(
            Arrays.asList("help", "join", "quickjoin", "leave", "team", "shop",
                    "quickbuy", "list", "stats", "play", "queueleave"));

    private final ArenaManager arenas;
    private final GameServices services;
    private final AdminCommand adminCommand;
    private final HelpService help;

    public ChickenWarsCommand(ArenaManager arenas, GameServices services,
                              AdminCommand adminCommand, HelpService help) {
        this.arenas = arenas;
        this.services = services;
        this.adminCommand = adminCommand;
        this.help = help;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        MessageService messages = services.getMessages();
        if (args.length == 0) {
            help.send(sender, null);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        if ("admin".equals(subcommand)) {
            if (!sender.hasPermission("chickenwars.admin")) {
                messages.send(sender, "command.no-permission");
                return true;
            }
            return adminCommand.execute(sender, rest);
        }

        if ("help".equals(subcommand)) {
            help.send(sender, rest.length == 0 ? null : rest[0]);
            return true;
        }

        if ("list".equals(subcommand)) {
            sendArenaList(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            messages.send(sender, "command.players-only");
            return true;
        }
        Player player = (Player) sender;

        if ("join".equals(subcommand)) {
            handleJoin(player, rest);
            return true;
        }
        if ("play".equals(subcommand)) {
            handlePlay(player, rest);
            return true;
        }
        if ("queueleave".equals(subcommand)) {
            if (!services.getLobby().leave(player)) {
                messages.send(player, "routing.none");
            }
            return true;
        }
        if ("quickjoin".equals(subcommand)) {
            handleQuickJoin(player);
            return true;
        }
        if ("leave".equals(subcommand)) {
            handleLeave(player);
            return true;
        }
        if ("team".equals(subcommand)) {
            handleTeam(player, rest);
            return true;
        }
        if ("shop".equals(subcommand)) {
            handleShop(player);
            return true;
        }
        if ("stats".equals(subcommand)) {
            handleStats(player);
            return true;
        }
        if ("quickbuy".equals(subcommand)) {
            handleQuickBuy(player, rest);
            return true;
        }

        messages.send(sender, "command.unknown-subcommand",
                "{subcommand}", subcommand);
        return true;
    }

    private void handleJoin(Player player, String[] args) {
        MessageService messages = services.getMessages();
        if (!player.hasPermission("chickenwars.command.join")) {
            messages.send(player, "command.no-permission");
            return;
        }
        if (args.length < 1) {
            messages.send(player, "command.usage", "{usage}", "/cw join <arena>");
            return;
        }
        if (arenas.getGameOf(player) != null) {
            messages.send(player, "command.already-in-game");
            return;
        }

        Game game = arenas.getGame(args[0]);
        if (game == null) {
            messages.send(player, "arena.not-found", "{arena}", args[0]);
            return;
        }
        if (!game.canJoin()) {
            messages.send(player, "arena.not-joinable",
                    "{arena}", game.getDefinition().getDisplayName());
            return;
        }
        game.join(player);
    }

    private void handleQuickJoin(Player player) {
        MessageService messages = services.getMessages();
        if (!player.hasPermission("chickenwars.command.join")) {
            messages.send(player, "command.no-permission");
            return;
        }
        if (arenas.getGameOf(player) != null) {
            messages.send(player, "command.already-in-game");
            return;
        }
        Game game = arenas.findBestGame();
        if (game == null) {
            messages.send(player, "arena.none-available");
            return;
        }
        game.join(player);
    }

    private void handlePlay(Player player, String[] args) {
        if (args.length == 0) {
            services.getLobbySelector().open(player);
            return;
        }
        if (args.length != 1) {
            services.getMessages().send(player, "command.usage", "{usage}",
                    "/cw play [duel|solo|doubles|trio]");
            return;
        }
        MatchMode mode = MatchMode.fromString(args[0]);
        if (mode == null) {
            services.getMessages().send(player, "routing.none");
            return;
        }
        if (ModeProfileRegistry.defaults().get(mode).isTracked()
                && !services.getProgression().getProfiles()
                .mayEnterTracked(player.getUniqueId())) {
            services.getMessages().send(player,
                    "persistence.profile-unavailable");
            return;
        }
        services.getLobby().join(player, mode, System.currentTimeMillis());
    }

    private void handleLeave(Player player) {
        MessageService messages = services.getMessages();
        Game game = arenas.getGameOf(player);
        if (game == null) {
            messages.send(player, "command.not-in-game");
            return;
        }
        game.leave(player, true);
        messages.send(player, "command.left");
    }

    private void handleTeam(Player player, String[] args) {
        MessageService messages = services.getMessages();
        Game game = arenas.getGameOf(player);
        if (game == null) {
            messages.send(player, "command.not-in-game");
            return;
        }
        if (game.getState() == ArenaState.IN_GAME) {
            messages.send(player, "team.already-started");
            return;
        }
        if (args.length < 1) {
            messages.send(player, "command.usage", "{usage}", "/cw team <squadra>");
            return;
        }

        GameTeam team = game.getTeam(args[0].toLowerCase(Locale.ROOT));
        if (team == null) {
            messages.send(player, "team.not-found", "{team}", args[0]);
            return;
        }
        if (team.isFull()) {
            messages.send(player, "team.full", "{team}", team.getColoredName());
            return;
        }

        PlayerSession session = game.getSession(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.setTeamId(team.getId());
        messages.send(player, "team.selected", "{team}", team.getColoredName());
    }

    private void handleShop(Player player) {
        MessageService messages = services.getMessages();
        Game game = arenas.getGameOf(player);
        if (game == null) {
            messages.send(player, "command.not-in-game");
            return;
        }
        PlayerSession session = game.getSession(player.getUniqueId());
        if (session == null || !session.getState().isActive()) {
            messages.send(player, "shop.unavailable");
            return;
        }
        if (!services.getShop().isAvailable()) {
            messages.send(player, "shop.unavailable");
            return;
        }
        GameTeam team = game.getTeam(session.getTeamId());
        services.getShop().open(player, game.getDefinition().getId(), null,
                session, team == null ? null : team.getColor(),
                game.getDefinition().getModeProfile());
    }

    /**
     * Elenca, seleziona o crea i preset Quick Buy.
     *
     * <p>Utilizzabile anche a partita in corso, come previsto dal
     * regolamento.</p>
     */
    private void handleQuickBuy(Player player, String[] args) {
        MessageService messages = services.getMessages();
        QuickBuyService quickBuy = services.getShop().getQuickBuy();

        if (args.length == 0) {
            StringBuilder names = new StringBuilder();
            for (QuickBuyPresetRecord preset
                    : quickBuy.list(player.getUniqueId())) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(preset.getPresetId());
            }
            messages.send(player, "shop.quickbuy.preset-selected",
                    "{preset}", quickBuy.getSelected(player.getUniqueId())
                            .getPresetId());
            messages.send(player, "command.usage",
                    "{usage}", "/cw quickbuy <" + names + "|create <nome>>");
            return;
        }

        if ("create".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                messages.send(player, "command.usage",
                        "{usage}", "/cw quickbuy create <nome>");
                return;
            }
            if (!quickBuy.create(player, args[1])) {
                messages.send(player, "shop.quickbuy.preset-limit",
                        "{limit}", String.valueOf(
                                quickBuy.getPresetLimit(player)));
                return;
            }
            quickBuy.select(player.getUniqueId(), args[1]);
            messages.send(player, "shop.quickbuy.preset-created",
                    "{preset}", args[1]);
            return;
        }

        if (!quickBuy.select(player.getUniqueId(), args[0])) {
            messages.send(player, "shop.quickbuy.preset-missing",
                    "{preset}", args[0]);
            return;
        }
        Game game = arenas.getGameOf(player);
        if (game != null) {
            PlayerSession session = game.getSession(player.getUniqueId());
            if (session != null) {
                session.getEquipmentState().selectQuickBuyPreset(args[0]);
            }
        }
        messages.send(player, "shop.quickbuy.preset-selected",
                "{preset}", args[0]);
    }

    private void handleStats(Player player) {
        MessageService messages = services.getMessages();
        Game game = arenas.getGameOf(player);
        if (game == null) {
            messages.send(player, "command.not-in-game");
            return;
        }
        PlayerSession session = game.getSession(player.getUniqueId());
        if (session == null) {
            return;
        }
        messages.send(player, "game.summary",
                "{kills}", String.valueOf(session.getKills()),
                "{final_kills}", String.valueOf(session.getFinalKills()),
                "{chickens}", String.valueOf(session.getChickensKilled()),
                "{deaths}", String.valueOf(session.getDeaths()));
    }

    private void sendArenaList(CommandSender sender) {
        MessageService messages = services.getMessages();
        if (arenas.getDefinitions().isEmpty()) {
            messages.send(sender, "arena.none-configured");
            return;
        }
        messages.send(sender, "arena.list-header");
        for (ArenaDefinition definition : arenas.getDefinitions()) {
            Game game = arenas.getGame(definition.getId());
            messages.sendRaw(sender, "arena.list-entry",
                    "{arena}", definition.getId(),
                    "{state}", game == null
                            ? ArenaState.DISABLED.name() : game.getState().name(),
                    "{online}", game == null
                            ? "0" : String.valueOf(game.getPlayerCount()),
                    "{max}", String.valueOf(definition.getMaximumPlayers()));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<String>(PLAYER_SUBCOMMANDS);
            if (sender.hasPermission("chickenwars.admin")) {
                options.add("admin");
            }
            return filter(options, args[0]);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if ("admin".equals(subcommand) && sender.hasPermission("chickenwars.admin")) {
            return adminCommand.tabComplete(sender,
                    Arrays.copyOfRange(args, 1, args.length));
        }
        if ("help".equals(subcommand) && args.length == 2) {
            return filter(help.getVisibleTopics(sender), args[1]);
        }
        if ("join".equals(subcommand) && args.length == 2) {
            return filter(arenas.getArenaIds(), args[1]);
        }
        if ("play".equals(subcommand) && args.length == 2) {
            return filter(Arrays.asList("duel", "solo", "doubles", "trio"),
                    args[1]);
        }
        if ("team".equals(subcommand) && args.length == 2 && sender instanceof Player) {
            Game game = arenas.getGameOf((Player) sender);
            if (game != null) {
                List<String> teamIds = new ArrayList<String>();
                for (GameTeam team : game.getTeams()) {
                    teamIds.add(team.getId());
                }
                return filter(teamIds, args[1]);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Filtra i suggerimenti in base al prefisso gia' digitato.
     */
    static List<String> filter(List<String> options, String prefix) {
        String normalized = prefix == null
                ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(option);
            }
        }
        return result;
    }
}
