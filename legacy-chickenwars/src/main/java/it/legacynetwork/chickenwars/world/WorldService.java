package it.legacynetwork.chickenwars.world;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gestione dei mondi delle arene, nello stile di un world manager.
 *
 * <p>I mondi creati qui vengono registrati in {@code worlds.yml} insieme al
 * template usato: e' indispensabile perche' un mondo con generatore
 * personalizzato, se ricaricato senza indicarlo, tornerebbe a generare terreno
 * normale attorno alla mappa.</p>
 */
public final class WorldService {

    private static final String REGISTRY_FILE = "worlds.yml";

    private final JavaPlugin plugin;
    private final Map<String, WorldTemplate> registry =
            new LinkedHashMap<String, WorldTemplate>();

    private String namePrefix = "cw_";
    private WorldTemplate defaultTemplate = WorldTemplate.VOID;
    private boolean clearEntities = true;

    public WorldService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Registro
    // ------------------------------------------------------------------

    /**
     * Ricarica il registro dei mondi gestiti.
     */
    public void reload(String namePrefix, WorldTemplate defaultTemplate,
                       boolean clearEntities) {
        this.namePrefix = namePrefix == null ? "" : namePrefix.trim();
        this.defaultTemplate = defaultTemplate == null
                ? WorldTemplate.VOID : defaultTemplate;
        this.clearEntities = clearEntities;

        registry.clear();
        File file = new File(plugin.getDataFolder(), REGISTRY_FILE);
        if (!file.isFile()) {
            return;
        }

        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Impossibile leggere " + REGISTRY_FILE
                    + ": " + exception.getMessage());
            return;
        } catch (InvalidConfigurationException exception) {
            plugin.getLogger().warning(REGISTRY_FILE + " non valido: "
                    + exception.getMessage());
            return;
        }

        for (String worldName : configuration.getKeys(false)) {
            WorldTemplate template = WorldTemplate.fromString(
                    configuration.getString(worldName + ".template"));
            registry.put(worldName.toLowerCase(Locale.ROOT),
                    template == null ? WorldTemplate.VOID : template);
        }
    }

    private void saveRegistry() {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<String, WorldTemplate> entry : registry.entrySet()) {
            configuration.set(entry.getKey() + ".template",
                    entry.getValue().name());
        }
        try {
            configuration.save(new File(plugin.getDataFolder(), REGISTRY_FILE));
        } catch (IOException exception) {
            plugin.getLogger().warning("Impossibile salvare " + REGISTRY_FILE
                    + ": " + exception.getMessage());
        }
    }

    /**
     * Compone il nome del mondo a partire dall'ID dell'arena.
     */
    public String worldNameFor(String arenaId) {
        return namePrefix + arenaId;
    }

    /**
     * Template registrato per un mondo.
     *
     * <p>Un mondo non registrato e' considerato {@link WorldTemplate#EXISTING}:
     * il plugin non ha motivo di imporre una generazione a un mondo che non ha
     * creato lui, e farlo trasformerebbe in vuoti i chunk nuovi di una mappa
     * gestita da altri.</p>
     *
     * @return il template registrato, oppure {@code EXISTING} se sconosciuto
     */
    public WorldTemplate getTemplate(String worldName) {
        if (worldName == null) {
            return WorldTemplate.EXISTING;
        }
        WorldTemplate template = registry.get(worldName.toLowerCase(Locale.ROOT));
        return template == null ? WorldTemplate.EXISTING : template;
    }

    public WorldTemplate getDefaultTemplate() {
        return defaultTemplate;
    }

    /**
     * Elenca i mondi registrati, caricati o meno.
     */
    public List<String> getRegisteredWorlds() {
        return new ArrayList<String>(registry.keySet());
    }

    // ------------------------------------------------------------------
    // Creazione e caricamento
    // ------------------------------------------------------------------

    /**
     * Carica il mondo se esiste, altrimenti lo crea con il template indicato.
     *
     * <p>Va eseguito sul thread principale: la creazione di un mondo blocca il
     * server per il tempo necessario a generare lo spawn.</p>
     *
     * @param worldName nome della cartella mondo
     * @param template  template da usare in creazione, eventualmente nullo
     * @return il mondo pronto all'uso, oppure {@code null} in caso di errore
     */
    public World loadOrCreate(String worldName, WorldTemplate template) {
        if (worldName == null || worldName.trim().isEmpty()) {
            return null;
        }
        String name = worldName.trim();

        World existing = Bukkit.getWorld(name);
        if (existing != null) {
            applyArenaRules(existing);
            return existing;
        }

        boolean alreadyOnDisk = folderExists(name);
        WorldTemplate effective = template != null
                ? template : getTemplate(name);

        WorldCreator creator = new WorldCreator(name);
        switch (effective) {
            case VOID:
                creator.generateStructures(false);
                creator.type(WorldType.FLAT);
                creator.generator(new VoidChunkGenerator());
                break;
            case FLAT:
                creator.generateStructures(false);
                creator.type(WorldType.FLAT);
                break;
            case NORMAL:
                creator.generateStructures(false);
                creator.type(WorldType.NORMAL);
                break;
            case EXISTING:
            default:
                // Nessuna impostazione di generazione: il mondo viene caricato
                // esattamente com'e' descritto nel suo level.dat.
                break;
        }

        World world;
        try {
            world = creator.createWorld();
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Impossibile creare il mondo " + name
                    + ": " + exception.getMessage());
            return null;
        }
        if (world == null) {
            plugin.getLogger().warning("Creazione del mondo " + name + " fallita.");
            return null;
        }

        registry.put(name.toLowerCase(Locale.ROOT), effective);
        saveRegistry();

        applyArenaRules(world);
        if (!alreadyOnDisk) {
            prepareSpawn(world, effective);
        }

        plugin.getLogger().info((alreadyOnDisk ? "Mondo caricato: " : "Mondo creato: ")
                + name + " (" + effective.name() + ")");
        return world;
    }

    /**
     * Adotta un mondo gia' esistente, senza alterarne la generazione.
     *
     * <p>Serve per usare come arena una mappa creata a mano o gestita da un
     * world manager esterno. Il mondo viene registrato come
     * {@link WorldTemplate#EXISTING}, cosi' i caricamenti successivi non gli
     * applicheranno mai un generatore.</p>
     *
     * @param worldName nome del mondo da adottare
     * @return il mondo pronto all'uso, oppure {@code null} se non esiste
     */
    public World adopt(String worldName) {
        if (worldName == null || worldName.trim().isEmpty()) {
            return null;
        }
        String name = worldName.trim();

        World world = Bukkit.getWorld(name);
        if (world == null) {
            if (!folderExists(name)) {
                return null;
            }
            world = loadOrCreate(name, WorldTemplate.EXISTING);
            if (world == null) {
                return null;
            }
        }

        registry.put(world.getName().toLowerCase(Locale.ROOT),
                WorldTemplate.EXISTING);
        saveRegistry();
        applyArenaRules(world);

        int removed = clearLivingEntities(world);
        if (removed > 0) {
            plugin.getLogger().info("Mondo " + world.getName() + ": rimosse "
                    + removed + " creature preesistenti.");
        }
        return world;
    }

    /**
     * Carica i mondi di tutte le arene indicate, se non gia' attivi.
     *
     * @param worldNames nomi dei mondi richiesti dalle arene
     * @return il numero di mondi caricati in questa chiamata
     */
    public int loadAll(Iterable<String> worldNames) {
        int loaded = 0;
        for (String worldName : worldNames) {
            if (worldName == null || worldName.trim().isEmpty()
                    || isLoaded(worldName)) {
                continue;
            }
            if (!folderExists(worldName)) {
                plugin.getLogger().warning("Mondo " + worldName
                        + " non presente su disco: l'arena resta inutilizzabile.");
                continue;
            }
            World world = loadOrCreate(worldName, null);
            if (world != null) {
                clearLivingEntities(world);
                loaded++;
            }
        }
        return loaded;
    }

    /**
     * Scarica un mondo salvandone il contenuto.
     *
     * <p>I giocatori ancora presenti vengono spostati nel mondo principale.</p>
     *
     * @return {@code true} se il mondo e' stato scaricato
     */
    public boolean unload(String worldName, boolean save) {
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }

        World fallback = Bukkit.getWorlds().isEmpty()
                ? null : Bukkit.getWorlds().get(0);
        if (fallback != null && !fallback.equals(world)) {
            for (Player player : world.getPlayers()) {
                player.teleport(fallback.getSpawnLocation());
            }
        }
        return Bukkit.unloadWorld(world, save);
    }

    public boolean isLoaded(String worldName) {
        return worldName != null && Bukkit.getWorld(worldName) != null;
    }

    /**
     * Verifica la presenza della cartella mondo, anche se non caricata.
     */
    public boolean folderExists(String worldName) {
        if (worldName == null || worldName.trim().isEmpty()) {
            return false;
        }
        File container = Bukkit.getWorldContainer();
        File folder = new File(container, worldName.trim());
        return folder.isDirectory()
                && new File(folder, "level.dat").isFile();
    }

    // ------------------------------------------------------------------
    // Preparazione
    // ------------------------------------------------------------------

    /**
     * Rimuove animali e mostri gia' presenti nel mondo.
     *
     * <p>Disattivare lo spawn impedisce la comparsa di nuove creature, ma non
     * tocca quelle gia' salvate nei chunk: una mappa costruita a mano o adottata
     * da un world manager puo' contenerne parecchie, e in partita
     * interferirebbero con la Gallina Reale.</p>
     *
     * <p>Gli armor stand vengono risparmiati: fanno parte delle decorazioni
     * della mappa e degli ologrammi del plugin.</p>
     *
     * @param world mondo da ripulire, eventualmente nullo
     * @return il numero di creature rimosse
     */
    public int clearLivingEntities(World world) {
        if (world == null || !clearEntities) {
            return 0;
        }
        int removed = 0;
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity instanceof Player || entity instanceof ArmorStand) {
                continue;
            }
            entity.remove();
            removed++;
        }
        return removed;
    }

    /**
     * Indica se la pulizia automatica delle creature e' attiva.
     */
    public boolean isClearEntities() {
        return clearEntities;
    }

    /**
     * Applica le regole adatte a un mondo che ospita partite.
     */
    public void applyArenaRules(World world) {
        if (world == null) {
            return;
        }
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doFireTick", "false");
        world.setGameRuleValue("mobGriefing", "false");
        world.setGameRuleValue("showDeathMessages", "false");
        world.setSpawnFlags(false, false);
        world.setDifficulty(Difficulty.NORMAL);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setAutoSave(true);
    }

    /**
     * Prepara lo spawn di un mondo appena creato.
     *
     * <p>Nei mondi vuoti viene posata una piattaforma minima, cosi' che chi
     * arriva non cada immediatamente nel vuoto.</p>
     */
    private void prepareSpawn(World world, WorldTemplate template) {
        if (template != WorldTemplate.VOID) {
            return;
        }
        int spawnY = 64;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.getBlockAt(x, spawnY - 1, z).setType(Material.STONE);
            }
        }
        world.setSpawnLocation(0, spawnY, 0);
    }

    /**
     * Teletrasporta un giocatore allo spawn del mondo indicato.
     *
     * @return {@code true} se il teletrasporto e' avvenuto
     */
    public boolean teleport(Player player, String worldName) {
        if (player == null) {
            return false;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }
        Location spawn = world.getSpawnLocation();
        return player.teleport(new Location(world,
                spawn.getBlockX() + 0.5D, spawn.getY(),
                spawn.getBlockZ() + 0.5D, spawn.getYaw(), spawn.getPitch()));
    }

    /**
     * Numero di giocatori presenti nel mondo indicato.
     */
    public int countPlayers(String worldName) {
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        return world == null ? 0 : world.getPlayers().size();
    }
}
