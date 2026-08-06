package it.legacynetwork.regions;

import it.legacynetwork.language.TranslationInstaller;
import it.legacynetwork.regions.api.LegacyRegionsService;
import it.legacynetwork.regions.api.LegacyRegionsServiceImpl;
import it.legacynetwork.regions.command.RegionCommand;
import it.legacynetwork.regions.config.RegionConfigLoader;
import it.legacynetwork.regions.config.WorldFlagsConfigLoader;
import it.legacynetwork.regions.core.RegionIndex;
import it.legacynetwork.regions.core.RegionResolver;
import it.legacynetwork.regions.listener.RegionProtectionListener;
import it.legacynetwork.regions.message.RegionMessageService;
import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionFlag;
import it.legacynetwork.regions.model.WorldRegionFlags;
import it.legacynetwork.regions.selection.SelectionProvider;
import it.legacynetwork.regions.selection.UnavailableSelectionProvider;
import it.legacynetwork.regions.selection.WorldEditSelectionProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class LegacyRegionsPlugin extends JavaPlugin {

    private volatile List<CuboidRegion> snapshot = Collections.emptyList();
    private volatile RegionIndex index = new RegionIndex();
    private volatile RegionResolver resolver;
    private volatile Map<RegionFlag, FlagState> defaultFlags = Collections.emptyMap();
    private volatile List<WorldRegionFlags> worldFlagsSnapshot = Collections.emptyList();
    private volatile SelectionProvider selectionProvider =
            new UnavailableSelectionProvider();
    private RegionMessageService messageService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        saveResource("regions.yml", false);
        saveResource("worlds.yml", false);
        int installed = TranslationInstaller.install(getDataFolder(),
                "translations", getLogger(), getClassLoader());
        getLogger().info("Traduzioni LegacyRegions installate: "
                + installed + " file.");

        try {
            messageService = new RegionMessageService(this);
            selectionProvider = initSelectionProvider();
            if (!reloadRegions()) {
                throw new IllegalStateException(
                        "config.yml, regions.yml, worlds.yml o file messaggi non validi");
            }

            PluginCommand command = getCommand("legacyregion");
            if (command == null) {
                throw new IllegalStateException(
                        "Comando legacyregion mancante in plugin.yml");
            }
            RegionCommand regionCommand = new RegionCommand(this);
            command.setExecutor(regionCommand);
            command.setTabCompleter(regionCommand);

            getServer().getPluginManager().registerEvents(
                    new RegionProtectionListener(this, messageService), this);

            getServer().getServicesManager().register(
                    LegacyRegionsService.class,
                    new LegacyRegionsServiceImpl(new Supplier<RegionResolver>() {
                        @Override
                        public RegionResolver get() {
                            return resolver;
                        }
                    }),
                    this,
                    ServicePriority.Normal);

            getLogger().info("LegacyRegions inizializzato con "
                    + snapshot.size() + " regioni e "
                    + worldFlagsSnapshot.size() + " mondi.");
            getLogger().info("WorldEdit/FAWE: "
                    + (selectionProvider.isAvailable()
                    ? "integrazione attiva" : "non disponibile"));
        } catch (RuntimeException exception) {
            getLogger().severe("Impossibile inizializzare LegacyRegions: "
                    + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        } catch (LinkageError error) {
            getLogger().severe("Dipendenza incompatibile durante l'avvio: "
                    + error.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (messageService != null) {
            messageService.close();
            messageService = null;
        }
        snapshot = Collections.emptyList();
        index = new RegionIndex();
        resolver = null;
        defaultFlags = Collections.emptyMap();
        worldFlagsSnapshot = Collections.emptyList();
        selectionProvider = new UnavailableSelectionProvider();
    }

    public boolean reloadRegions() {
        try {
            reloadConfig();
            Map<RegionFlag, FlagState> newDefaults = loadDefaultFlags();
            List<CuboidRegion> newRegions = RegionConfigLoader.loadRegions(
                    new File(getDataFolder(), "regions.yml"), getLogger());
            if (newRegions == null) {
                getLogger().warning("Reload annullato: mantengo lo snapshot precedente.");
                return false;
            }
            List<WorldRegionFlags> newWorldFlags = WorldFlagsConfigLoader.load(
                    new File(getDataFolder(), "worlds.yml"), getLogger());
            if (newWorldFlags == null) {
                getLogger().warning("Reload annullato: worlds.yml non valido.");
                return false;
            }
            if (messageService != null && !messageService.reload()) {
                if (resolver == null) {
                    getLogger().warning(
                            "Avvio annullato: impossibile caricare i messaggi.");
                    return false;
                }
                getLogger().warning(
                        "Messaggi non ricaricati: mantengo quelli precedenti.");
            }
            publish(newRegions, newDefaults, newWorldFlags);
            getLogger().info("Regions reload completato ("
                    + newRegions.size() + " regioni, "
                    + newWorldFlags.size() + " mondi).");
            return true;
        } catch (RuntimeException exception) {
            getLogger().warning("Reload annullato: " + exception.getMessage());
            return false;
        }
    }

    public boolean saveAndRebuild(List<CuboidRegion> newSnapshot) {
        if (newSnapshot == null) {
            return false;
        }
        List<CuboidRegion> safeSnapshot =
                new ArrayList<CuboidRegion>(newSnapshot);
        Set<String> ids = new HashSet<String>();
        for (CuboidRegion region : safeSnapshot) {
            if (region == null || !ids.add(region.getId())) {
                getLogger().warning("Snapshot regioni non valido o con ID duplicati.");
                return false;
            }
        }

        if (!RegionConfigLoader.saveRegions(
                new File(getDataFolder(), "regions.yml"),
                safeSnapshot,
                getLogger())) {
            return false;
        }
        publish(safeSnapshot, defaultFlags, worldFlagsSnapshot);
        return true;
    }

    public boolean saveWorldFlagsAndRebuild(List<WorldRegionFlags> newWorldFlags) {
        if (newWorldFlags == null) {
            return false;
        }
        if (!WorldFlagsConfigLoader.save(
                new File(getDataFolder(), "worlds.yml"),
                newWorldFlags, getLogger())) {
            return false;
        }
        List<WorldRegionFlags> immutable = Collections.unmodifiableList(
                new ArrayList<WorldRegionFlags>(newWorldFlags));
        this.worldFlagsSnapshot = immutable;
        publish(snapshot, defaultFlags, immutable);
        return true;
    }

    private void publish(List<CuboidRegion> regions,
                          Map<RegionFlag, FlagState> defaults,
                          List<WorldRegionFlags> worldFlags) {
        List<CuboidRegion> immutableRegions = Collections.unmodifiableList(
                new ArrayList<CuboidRegion>(regions));
        EnumMap<RegionFlag, FlagState> copiedDefaults =
                new EnumMap<RegionFlag, FlagState>(RegionFlag.class);
        copiedDefaults.putAll(defaults);
        Map<RegionFlag, FlagState> immutableDefaults =
                Collections.unmodifiableMap(copiedDefaults);

        List<WorldRegionFlags> immutableWorldFlags = Collections.unmodifiableList(
                new ArrayList<WorldRegionFlags>(worldFlags));

        RegionIndex newIndex = new RegionIndex();
        newIndex.build(immutableRegions);
        RegionResolver newResolver =
                new RegionResolver(newIndex, immutableDefaults, immutableWorldFlags);

        this.snapshot = immutableRegions;
        this.index = newIndex;
        this.defaultFlags = immutableDefaults;
        this.worldFlagsSnapshot = immutableWorldFlags;
        this.resolver = newResolver;
    }

    private Map<RegionFlag, FlagState> loadDefaultFlags() {
        ConfigurationSection section =
                getConfig().getConfigurationSection("defaults");
        if (section == null) {
            throw new IllegalArgumentException(
                    "Sezione defaults mancante in config.yml");
        }

        EnumMap<RegionFlag, FlagState> flags =
                new EnumMap<RegionFlag, FlagState>(RegionFlag.class);
        for (RegionFlag flag : RegionFlag.values()) {
            String key = flag.getPermissionKey();
            if (!section.contains(key)) {
                flags.put(flag,
                        flag.isSpecific() ? FlagState.INHERIT : FlagState.ALLOW);
                continue;
            }
            FlagState state = FlagState.fromString(section.getString(key));
            if (state == null) {
                throw new IllegalArgumentException(
                        "Default non valido per il flag " + key);
            }
            flags.put(flag, state);
        }
        return Collections.unmodifiableMap(flags);
    }

    private SelectionProvider initSelectionProvider() {
        Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
        SelectionProvider provider = createSelectionProvider(worldEdit);
        if (provider != null) {
            return provider;
        }

        Plugin fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        provider = createSelectionProvider(fawe);
        if (provider != null) {
            return provider;
        }

        getLogger().warning("WorldEdit/FAWE non disponibile: create e redefine disabilitati.");
        return new UnavailableSelectionProvider();
    }

    private SelectionProvider createSelectionProvider(Plugin plugin) {
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        try {
            WorldEditSelectionProvider provider =
                    new WorldEditSelectionProvider(plugin);
            if (provider.isAvailable()) {
                getLogger().info(plugin.getName()
                        + " rilevato, selezioni regioni abilitate.");
                return provider;
            }
        } catch (RuntimeException exception) {
            getLogger().warning("Bridge " + plugin.getName()
                    + " non disponibile: " + exception.getMessage());
        } catch (LinkageError error) {
            getLogger().warning("Bridge " + plugin.getName()
                    + " incompatibile: " + error.getMessage());
        }
        return null;
    }

    public List<CuboidRegion> getSnapshot() {
        return snapshot;
    }

    public RegionIndex getIndex() {
        return index;
    }

    public RegionResolver getResolver() {
        return resolver;
    }

    public Map<RegionFlag, FlagState> getDefaultFlags() {
        return defaultFlags;
    }

    public List<WorldRegionFlags> getWorldFlagsSnapshot() {
        return worldFlagsSnapshot;
    }

    public SelectionProvider getSelectionProvider() {
        return selectionProvider;
    }
}
