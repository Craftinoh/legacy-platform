package it.legacynetwork.chickenwars.setup;

import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.arena.GeneratorDefinition;
import it.legacynetwork.chickenwars.arena.TeamDefinition;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import it.legacynetwork.chickenwars.model.TeamColor;
import it.legacynetwork.chickenwars.player.InventorySnapshot;
import it.legacynetwork.chickenwars.player.PendingRestoreService;
import it.legacynetwork.chickenwars.world.WorldService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Editor guidato delle arene, basato su strumenti nella barra rapida.
 *
 * <p>All'ingresso l'inventario personale viene salvato e sostituito dagli
 * strumenti; all'uscita viene ripristinato esattamente com'era. Ogni azione
 * lavora sulla posizione in cui si trova l'amministratore.</p>
 */
public final class SetupService {

    private static final int TEAM_MENU_SIZE = 36;
    private static final int MENU_SIZE = 27;
    private static final int ADD_TEAM_SLOT = 31;

    /** Slot utilizzabili nei menu a griglia, bordi esclusi. */
    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private final ArenaManager arenas;
    private final MessageService messages;
    private final PendingRestoreService pendingRestores;
    private final WorldService worlds;

    private final Map<UUID, SetupSession> sessions =
            new LinkedHashMap<UUID, SetupSession>();

    public SetupService(ArenaManager arenas, MessageService messages,
                        PendingRestoreService pendingRestores,
                        WorldService worlds) {
        this.arenas = arenas;
        this.messages = messages;
        this.pendingRestores = pendingRestores;
        this.worlds = worlds;
    }

    // ------------------------------------------------------------------
    // Ciclo di vita della sessione
    // ------------------------------------------------------------------

    /**
     * Apre l'editor sull'arena indicata.
     *
     * @return {@code true} se la sessione e' stata aperta
     */
    public boolean enter(Player player, ArenaDefinition arena) {
        if (player == null || arena == null) {
            return false;
        }
        if (sessions.containsKey(player.getUniqueId())) {
            messages.send(player, "setup.already-editing");
            return false;
        }

        SetupSession session = new SetupSession(player.getUniqueId(),
                arena.getId(), InventorySnapshot.capture(player));
        sessions.put(player.getUniqueId(), session);

        if (arena.getWorld() == null || arena.getWorld().trim().isEmpty()) {
            arena.setWorld(player.getWorld().getName());
        }

        InventorySnapshot.clear(player);
        player.setGameMode(GameMode.CREATIVE);
        giveTools(player);

        for (String line : messages.getList(player, "setup.entered",
                "{arena}", arena.getId())) {
            player.sendMessage(line);
        }

        // Il mondo potrebbe essere stato caricato da un gestore esterno dopo
        // l'adozione: le regole vanno riconfermate qui.
        World arenaWorld = Bukkit.getWorld(arena.getWorld());
        worlds.applyArenaRules(arenaWorld);

        // Una mappa appena importata puo' contenere animali gia' salvati nei
        // chunk: vanno rimossi ora, non a partita avviata.
        int removed = worlds.clearLivingEntities(arenaWorld);
        if (removed > 0) {
            messages.send(player, "setup.entities-cleared",
                    "{amount}", String.valueOf(removed));
        }
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0F, 1.6F);
        return true;
    }

    /**
     * Chiude l'editor ripristinando lo stato precedente del giocatore.
     *
     * @param save indica se salvare l'arena prima di uscire
     */
    public void exit(Player player, boolean save) {
        if (player == null) {
            return;
        }
        SetupSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        ArenaDefinition arena = arenas.getDefinition(session.getArenaId());
        if (save && arena != null) {
            if (arenas.save(arena)) {
                arenas.rebuildGame(arena.getId());
                messages.send(player, "admin.saved", "{arena}", arena.getId());
            } else {
                messages.send(player, "admin.save-failed",
                        "{arena}", arena.getId());
            }
        }

        if (!player.isOnline()) {
            // Disconnessione durante l'editor: l'inventario torna al rientro.
            pendingRestores.register(player.getUniqueId(), session.getSnapshot());
            return;
        }

        player.closeInventory();
        if (session.getSnapshot() != null) {
            session.getSnapshot().restore(player);
        } else {
            InventorySnapshot.clear(player);
            player.setGameMode(GameMode.SURVIVAL);
        }
        messages.send(player, "setup.exited");
    }

    /**
     * Chiude tutte le sessioni aperte, allo spegnimento del plugin.
     */
    public void shutdown() {
        for (UUID playerId : new ArrayList<UUID>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                exit(player, false);
            } else {
                sessions.remove(playerId);
            }
        }
        sessions.clear();
    }

    public boolean isEditing(Player player) {
        return player != null && sessions.containsKey(player.getUniqueId());
    }

    public SetupSession getSession(Player player) {
        return player == null ? null : sessions.get(player.getUniqueId());
    }

    // ------------------------------------------------------------------
    // Strumenti
    // ------------------------------------------------------------------

    private void giveTools(Player player) {
        for (SetupTool tool : SetupTool.values()) {
            player.getInventory().setItem(tool.getSlot(),
                    createIcon(player, tool.getMaterial(), (byte) 0,
                            tool.getNameKey(), tool.getLoreKey()));
        }
        player.getInventory().setHeldItemSlot(0);
        player.updateInventory();
    }

    /**
     * Esegue l'azione associata a uno strumento.
     */
    public void handleTool(Player player, SetupTool tool) {
        SetupSession session = getSession(player);
        if (session == null || tool == null) {
            return;
        }
        ArenaDefinition arena = arenas.getDefinition(session.getArenaId());
        if (arena == null) {
            messages.send(player, "arena.not-found",
                    "{arena}", session.getArenaId());
            exit(player, false);
            return;
        }

        switch (tool) {
            case POSITIONS:
                openMenu(player, SetupMenu.Type.POSITIONS);
                return;
            case TEAMS:
                openMenu(player, SetupMenu.Type.TEAMS);
                return;
            case GENERATORS:
                openMenu(player, SetupMenu.Type.GENERATORS);
                return;
            case VALIDATE:
                sendValidation(player, arena);
                return;
            case SAVE_EXIT:
                exit(player, true);
                return;
            default:
                break;
        }

        TeamDefinition team = resolveSelectedTeam(player, session, arena);
        if (team == null) {
            return;
        }
        SimpleLocation location = SimpleLocation.of(player.getLocation());

        String messageKey;
        if (tool == SetupTool.TEAM_SPAWN) {
            team.setSpawn(location);
            messageKey = "setup.team-spawn-set";
        } else if (tool == SetupTool.TEAM_NEST) {
            team.setNest(location);
            messageKey = "setup.team-nest-set";
        } else if (tool == SetupTool.TEAM_CHICKEN) {
            team.setChicken(location);
            messageKey = "setup.team-chicken-set";
        } else {
            team.setShop(location);
            messageKey = "setup.team-shop-set";
        }

        arenas.save(arena);
        messages.send(player, messageKey, "{team}", team.getColoredName());
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.8F);
    }

    private TeamDefinition resolveSelectedTeam(Player player, SetupSession session,
                                               ArenaDefinition arena) {
        if (!session.hasSelectedTeam()) {
            messages.send(player, "setup.no-team-selected");
            return null;
        }
        TeamDefinition team = arena.getTeam(session.getSelectedTeamId());
        if (team == null) {
            session.setSelectedTeamId(null);
            messages.send(player, "setup.no-team-selected");
        }
        return team;
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    /**
     * Apre uno dei menu dell'editor.
     */
    public void openMenu(Player player, SetupMenu.Type type) {
        SetupSession session = getSession(player);
        if (session == null) {
            return;
        }
        ArenaDefinition arena = arenas.getDefinition(session.getArenaId());
        if (arena == null) {
            return;
        }

        int size = type == SetupMenu.Type.TEAMS ? TEAM_MENU_SIZE : MENU_SIZE;
        Inventory inventory = Bukkit.createInventory(
                new SetupMenu(type, arena.getId()), size,
                messages.get(player, "setup.menu." + type.name().toLowerCase(
                        Locale.ROOT) + ".title"));

        render(player, inventory, type, arena, session);
        player.openInventory(inventory);
    }

    private void render(Player player, Inventory inventory, SetupMenu.Type type,
                        ArenaDefinition arena, SetupSession session) {
        inventory.clear();
        switch (type) {
            case POSITIONS:
                renderPositions(player, inventory, arena);
                break;
            case TEAMS:
                renderTeams(player, inventory, arena, session);
                break;
            case TEAM_COLORS:
                renderTeamColors(player, inventory, arena);
                break;
            case GENERATORS:
                renderGenerators(player, inventory, session, arena);
                break;
            default:
                break;
        }
    }

    private void renderPositions(Player player, Inventory inventory,
                                 ArenaDefinition arena) {
        inventory.setItem(10, statusIcon(player, Material.COMPASS,
                "setup.position.lobby", arena.getLobby()));
        inventory.setItem(11, statusIcon(player, Material.EYE_OF_ENDER,
                "setup.position.spectator", arena.getSpectator()));
        inventory.setItem(13, statusIcon(player, Material.GOLD_AXE,
                "setup.position.pos1", arena.getPos1()));
        inventory.setItem(14, statusIcon(player, Material.IRON_AXE,
                "setup.position.pos2", arena.getPos2()));
        inventory.setItem(16, createIcon(player, Material.LADDER, (byte) 0,
                "setup.position.build-limit.name",
                "setup.position.build-limit.lore"));
    }

    private void renderTeams(Player player, Inventory inventory,
                             ArenaDefinition arena, SetupSession session) {
        int index = 0;
        for (TeamDefinition team : arena.getTeams()) {
            if (index >= GRID_SLOTS.length) {
                break;
            }
            boolean selected = team.getId().equals(session.getSelectedTeamId());
            List<String> lore = new ArrayList<String>();
            lore.add(colorize(messages.get(player,
                    team.isComplete() ? "setup.team.complete"
                            : "setup.team.incomplete")));
            lore.add("");
            lore.addAll(messages.getList(player, "setup.team.actions"));
            if (selected) {
                lore.add(colorize(messages.get(player, "setup.team.selected")));
            }

            ItemStack icon = new ItemStack(Material.WOOL, 1,
                    (short) team.getColor().getWoolData());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((selected ? ChatColor.GREEN + "> " : "")
                        + team.getColor().getChatColor()
                        + team.getDisplayName()
                        + ChatColor.GRAY + " (" + team.getId() + ")");
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inventory.setItem(GRID_SLOTS[index++], icon);
        }

        inventory.setItem(ADD_TEAM_SLOT, createIcon(player,
                Material.NETHER_STAR, (byte) 0,
                "setup.team.add.name", "setup.team.add.lore"));
    }

    private void renderTeamColors(Player player, Inventory inventory,
                                  ArenaDefinition arena) {
        int index = 0;
        for (TeamColor color : TeamColor.values()) {
            if (index >= GRID_SLOTS.length) {
                break;
            }
            boolean used = arena.getTeam(
                    color.name().toLowerCase(Locale.ROOT)) != null;
            ItemStack icon = new ItemStack(Material.WOOL, 1,
                    (short) color.getWoolData());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color.getChatColor() + color.getItalianName());
                meta.setLore(messages.getList(player, used
                        ? "setup.color.used" : "setup.color.available"));
                icon.setItemMeta(meta);
            }
            inventory.setItem(GRID_SLOTS[index++], icon);
        }
    }

    private void renderGenerators(Player player, Inventory inventory,
                                  SetupSession session, ArenaDefinition arena) {
        ResourceType[] types = ResourceType.values();
        for (int i = 0; i < types.length && i < 7; i++) {
            inventory.setItem(10 + i, createIcon(player,
                    types[i].getMaterial(), (byte) 0,
                    "setup.generator.central.name", "setup.generator.central.lore",
                    "{type}", types[i].getItalianName()));
        }

        TeamDefinition selected = session.hasSelectedTeam()
                ? arena.getTeam(session.getSelectedTeamId()) : null;
        for (int i = 0; i < types.length && i < 7; i++) {
            String teamName = selected == null
                    ? messages.get(player, "setup.generator.no-team")
                    : selected.getColoredName();
            inventory.setItem(19 + i, createIcon(player,
                    types[i].getMaterial(), (byte) 0,
                    "setup.generator.team.name", "setup.generator.team.lore",
                    "{type}", types[i].getItalianName(),
                    "{team}", teamName));
        }
    }

    /**
     * Gestisce un click all'interno di un menu dell'editor.
     *
     * @param rightClick indica se il click e' stato con il tasto destro
     */
    public void handleMenuClick(Player player, SetupMenu menu, int slot,
                                boolean rightClick) {
        SetupSession session = getSession(player);
        if (session == null || menu == null) {
            return;
        }
        ArenaDefinition arena = arenas.getDefinition(menu.getArenaId());
        if (arena == null) {
            player.closeInventory();
            return;
        }

        switch (menu.getType()) {
            case POSITIONS:
                handlePositionsClick(player, arena, slot);
                break;
            case TEAMS:
                handleTeamsClick(player, session, arena, slot, rightClick);
                break;
            case TEAM_COLORS:
                handleTeamColorsClick(player, session, arena, slot);
                break;
            case GENERATORS:
                handleGeneratorsClick(player, session, arena, slot);
                break;
            default:
                break;
        }
    }

    private void handlePositionsClick(Player player, ArenaDefinition arena,
                                      int slot) {
        SimpleLocation location = SimpleLocation.of(player.getLocation());
        String messageKey;

        if (slot == 10) {
            arena.setLobby(location);
            messageKey = "admin.lobby-set";
        } else if (slot == 11) {
            arena.setSpectator(location);
            messageKey = "admin.spectator-set";
        } else if (slot == 13) {
            arena.setPos1(location);
            messageKey = "admin.pos1-set";
        } else if (slot == 14) {
            arena.setPos2(location);
            messageKey = "admin.pos2-set";
        } else if (slot == 16) {
            arena.setMaximumBuildY(player.getLocation().getBlockY());
            messageKey = "admin.build-limit-set";
        } else {
            return;
        }

        arenas.save(arena);
        messages.send(player, messageKey, "{arena}", arena.getId());
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.8F);
        openMenu(player, SetupMenu.Type.POSITIONS);
    }

    private void handleTeamsClick(Player player, SetupSession session,
                                  ArenaDefinition arena, int slot,
                                  boolean rightClick) {
        if (slot == ADD_TEAM_SLOT) {
            openMenu(player, SetupMenu.Type.TEAM_COLORS);
            return;
        }

        int index = gridIndex(slot);
        if (index < 0) {
            return;
        }
        List<TeamDefinition> teams =
                new ArrayList<TeamDefinition>(arena.getTeams());
        if (index >= teams.size()) {
            return;
        }
        TeamDefinition team = teams.get(index);

        if (rightClick) {
            arena.removeTeam(team.getId());
            if (team.getId().equals(session.getSelectedTeamId())) {
                session.setSelectedTeamId(null);
            }
            arenas.save(arena);
            messages.send(player, "admin.team-removed", "{arena}", arena.getId());
        } else {
            session.setSelectedTeamId(team.getId());
            messages.send(player, "setup.team-selected",
                    "{team}", team.getColoredName());
        }
        player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.6F);
        openMenu(player, SetupMenu.Type.TEAMS);
    }

    private void handleTeamColorsClick(Player player, SetupSession session,
                                       ArenaDefinition arena, int slot) {
        int index = gridIndex(slot);
        TeamColor[] colors = TeamColor.values();
        if (index < 0 || index >= colors.length) {
            return;
        }
        TeamColor color = colors[index];
        String teamId = color.name().toLowerCase(Locale.ROOT);

        if (arena.getTeam(teamId) != null) {
            messages.send(player, "admin.team-exists", "{team}", teamId);
            return;
        }

        arena.addTeam(new TeamDefinition(teamId, color.getItalianName(), color,
                arena.getPlayersPerTeam()));
        session.setSelectedTeamId(teamId);
        arenas.save(arena);

        messages.send(player, "setup.team-created",
                "{team}", color.getChatColor() + color.getItalianName());
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0F, 1.8F);
        openMenu(player, SetupMenu.Type.TEAMS);
    }

    private void handleGeneratorsClick(Player player, SetupSession session,
                                       ArenaDefinition arena, int slot) {
        ResourceType[] types = ResourceType.values();

        boolean teamGenerator;
        int index;
        if (slot >= 10 && slot < 10 + types.length) {
            teamGenerator = false;
            index = slot - 10;
        } else if (slot >= 19 && slot < 19 + types.length) {
            teamGenerator = true;
            index = slot - 19;
        } else {
            return;
        }

        String teamId = null;
        if (teamGenerator) {
            if (!session.hasSelectedTeam()
                    || arena.getTeam(session.getSelectedTeamId()) == null) {
                messages.send(player, "setup.no-team-selected");
                return;
            }
            teamId = session.getSelectedTeamId();
        }

        ResourceType type = types[index];
        String generatorId =
                arena.nextGeneratorId(type.name().toLowerCase(Locale.ROOT));
        SimpleLocation location = SimpleLocation.of(player.getLocation());
        arena.addGenerator(new GeneratorDefinition(generatorId, type,
                location.centered(), teamId, 1, !teamGenerator));
        arenas.save(arena);

        messages.send(player, "setup.generator-added",
                "{id}", generatorId,
                "{type}", type.getItalianName());
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.4F);
        player.closeInventory();
    }

    // ------------------------------------------------------------------
    // Supporto
    // ------------------------------------------------------------------

    /**
     * Mostra il riepilogo di validazione dell'arena.
     */
    public void sendValidation(Player player, ArenaDefinition arena) {
        List<String> missing = arena.findMissing();
        if (missing.isEmpty()) {
            messages.send(player, "admin.validate-ok", "{arena}", arena.getId());
            messages.send(player, "setup.ready-to-enable");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0F, 1.4F);
            return;
        }
        messages.send(player, "admin.validate-failed", "{arena}", arena.getId());
        for (String entry : missing) {
            player.sendMessage(ChatColor.RED + " ✘ " + ChatColor.GRAY + entry);
        }
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0F, 1.0F);
    }

    /**
     * Converte uno slot della griglia nel corrispondente indice progressivo.
     *
     * @return l'indice, oppure {@code -1} se lo slot non fa parte della griglia
     */
    private int gridIndex(int slot) {
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (GRID_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack statusIcon(Player player, Material material,
                                 String baseKey, SimpleLocation current) {
        ItemStack icon = createIcon(player, material, (byte) 0,
                baseKey + ".name", baseKey + ".lore");
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }
        List<String> lore = meta.getLore() == null
                ? new ArrayList<String>() : new ArrayList<String>(meta.getLore());
        lore.add("");
        lore.add(current == null
                ? colorize(messages.get(player, "setup.position.not-set"))
                : colorize(messages.get(player, "setup.position.set",
                "{value}", current.serialize())));
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createIcon(Player player, Material material, byte data,
                                 String nameKey, String loreKey,
                                 String... replacements) {
        ItemStack icon = new ItemStack(material, 1, (short) data);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(messages.get(player, nameKey, replacements));
            meta.setLore(messages.getList(player, loreKey, replacements));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
