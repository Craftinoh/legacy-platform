package it.legacynetwork.regions;

import it.legacynetwork.regions.api.LegacyRegionsService;
import it.legacynetwork.regions.api.LegacyRegionsServiceImpl;
import it.legacynetwork.regions.command.RegionCommand;
import it.legacynetwork.regions.config.RegionConfigLoader;
import it.legacynetwork.regions.core.RegionIndex;
import it.legacynetwork.regions.core.RegionResolver;
import it.legacynetwork.regions.listener.RegionProtectionListener;
import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionFlag;
import it.legacynetwork.regions.selection.SelectionProvider;
import it.legacynetwork.regions.selection.UnavailableSelectionProvider;
import it.legacynetwork.regions.selection.WorldEditSelectionProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LegacyRegionsPlugin extends JavaPlugin {

    private volatile List<CuboidRegion> snapshot = new ArrayList<CuboidRegion>();
    private volatile RegionIndex index = new RegionIndex();
    private volatile RegionResolver resolver;
    private volatile Map<RegionFlag, FlagState> defaultFlags = new HashMap<RegionFlag, FlagState>();
    private volatile SelectionProvider selectionProvider;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("regions.yml", false);

        try {
            loadDefaultFlags();
            selectionProvider = initSelectionProvider();
            loadRegions();

            RegionCommand regionCommand = new RegionCommand(this);
            getCommand("legacyregion").setExecutor(regionCommand);
            getCommand("legacyregion").setTabCompleter(regionCommand);

            getServer().getPluginManager().registerEvents(
                    new RegionProtectionListener(this), this);

            getServer().getServicesManager().register(
                    LegacyRegionsService.class,
                    new LegacyRegionsServiceImpl(resolver),
                    this,
                    org.bukkit.plugin.ServicePriority.Normal);

            getLogger().info("LegacyRegions inizializzato con " + snapshot.size() + " regioni.");
            getLogger().info("WorldEdit: " + (selectionProvider.isAvailable() ? "disponibile" : "non disponibile"));
        } catch (RuntimeException exception) {
            getLogger().severe("Impossibile inizializzare LegacyRegions: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
    }

    public void reloadRegions() {
        List<CuboidRegion> newRegions = RegionConfigLoader.loadRegions(
                new File(getDataFolder(), "regions.yml"), getLogger());
        if (newRegions == null) {
            getLogger().warning("Reload fallito: impossibile caricare le regioni. "
                    + "Mantengo la configurazione precedente.");
            return;
        }

        RegionIndex newIndex = new RegionIndex();
        newIndex.build(newRegions);
        RegionResolver newResolver = new RegionResolver(newIndex, defaultFlags);

        this.snapshot = newRegions;
        this.index = newIndex;
        this.resolver = newResolver;
        getLogger().info("Regions reload completato (" + newRegions.size() + " regioni).");
    }

    public void saveAndRebuild(List<CuboidRegion> newSnapshot) {
        RegionConfigLoader.saveRegions(
                new File(getDataFolder(), "regions.yml"), newSnapshot, getLogger());

        RegionIndex newIndex = new RegionIndex();
        newIndex.build(newSnapshot);
        RegionResolver newResolver = new RegionResolver(newIndex, defaultFlags);

        this.snapshot = newSnapshot;
        this.index = newIndex;
        this.resolver = newResolver;
    }

    private void loadRegions() {
        List<CuboidRegion> regions = RegionConfigLoader.loadRegions(
                new File(getDataFolder(), "regions.yml"), getLogger());
        if (regions == null) {
            regions = new ArrayList<CuboidRegion>();
        }
        this.snapshot = regions;
        this.index = new RegionIndex();
        this.index.build(regions);
        this.resolver = new RegionResolver(index, defaultFlags);
    }

    private void loadDefaultFlags() {
        Map<RegionFlag, FlagState> flags = new HashMap<RegionFlag, FlagState>();
        org.bukkit.configuration.ConfigurationSection defaultsSection = getConfig()
                .getConfigurationSection("defaults");
        if (defaultsSection != null) {
            for (String key : defaultsSection.getKeys(false)) {
                RegionFlag flag = RegionFlag.fromString(key);
                if (flag == null) {
                    continue;
                }
                String value = defaultsSection.getString(key);
                FlagState state = FlagState.fromString(value);
                if (state == null) {
                    state = FlagState.ALLOW;
                }
                flags.put(flag, state);
            }
        }
        for (RegionFlag flag : RegionFlag.values()) {
            if (!flags.containsKey(flag)) {
                flags.put(flag, FlagState.ALLOW);
            }
        }
        this.defaultFlags = flags;
    }

    private SelectionProvider initSelectionProvider() {
        org.bukkit.plugin.Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (we != null && we.isEnabled()) {
            try {
                WorldEditSelectionProvider provider = new WorldEditSelectionProvider(we);
                if (provider.isAvailable()) {
                    getLogger().info("WorldEdit rilevato, integrazione attiva.");
                    return provider;
                }
            } catch (Exception e) {
                getLogger().warning("Errore nell'inizializzare WorldEdit: " + e.getMessage());
            }
        } else {
            org.bukkit.plugin.Plugin fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
            if (fawe != null && fawe.isEnabled()) {
                try {
                    WorldEditSelectionProvider provider = new WorldEditSelectionProvider(fawe);
                    if (provider.isAvailable()) {
                        getLogger().info("FastAsyncWorldEdit rilevato, integrazione attiva.");
                        return provider;
                    }
                } catch (Exception e) {
                    getLogger().warning("Errore nell'inizializzare FastAsyncWorldEdit: " + e.getMessage());
                }
            }
        }
        getLogger().warning("WorldEdit non trovato. Comando di selezione non disponibile.");
        return new UnavailableSelectionProvider();
    }

    public List<CuboidRegion> getSnapshot() {
        return new ArrayList<CuboidRegion>(snapshot);
    }

    public RegionIndex getIndex() {
        return index;
    }

    public RegionResolver getResolver() {
        return resolver;
    }

    public Map<RegionFlag, FlagState> getDefaultFlags() {
        return new HashMap<RegionFlag, FlagState>(defaultFlags);
    }

    public SelectionProvider getSelectionProvider() {
        return selectionProvider;
    }
}
