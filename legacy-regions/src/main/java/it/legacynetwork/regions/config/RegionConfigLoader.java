package it.legacynetwork.regions.config;

import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionFlag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class RegionConfigLoader {

    private RegionConfigLoader() {
    }

    public static List<CuboidRegion> loadRegions(File file) {
        return loadRegions(file, null);
    }

    public static List<CuboidRegion> loadRegions(File file, Logger logger) {
        if (!file.exists()) {
            if (logger != null) {
                logger.warning("File regioni non trovato: " + file.getAbsolutePath());
            }
            return new ArrayList<CuboidRegion>();
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection regionsSection = config.getConfigurationSection("regions");
            if (regionsSection == null) {
                if (logger != null) {
                    logger.warning("Sezione 'regions' non trovata in " + file.getName());
                }
                return new ArrayList<CuboidRegion>();
            }

            Set<String> regionIds = regionsSection.getKeys(false);
            List<CuboidRegion> regions = new ArrayList<CuboidRegion>();

            for (String id : regionIds) {
                ConfigurationSection regionSection = regionsSection.getConfigurationSection(id);
                if (regionSection == null) {
                    if (logger != null) {
                        logger.warning("Sezione regione '" + id + "' non valida, saltata.");
                    }
                    continue;
                }

                try {
                    CuboidRegion region = parseRegion(id, regionSection);
                    if (region != null) {
                        regions.add(region);
                    }
                } catch (Exception e) {
                    if (logger != null) {
                        logger.warning("Errore nel parsing della regione '" + id + "': " + e.getMessage());
                    }
                }
            }

            return regions;
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("Errore nel caricamento del file regioni: " + e.getMessage());
            }
            return new ArrayList<CuboidRegion>();
        }
    }

    private static CuboidRegion parseRegion(String id, ConfigurationSection section) {
        String worldName = null;
        String worldUuid = null;
        ConfigurationSection worldSection = section.getConfigurationSection("world");
        if (worldSection != null) {
            worldName = worldSection.getString("name", null);
            worldUuid = worldSection.getString("uuid", null);
        }

        ConfigurationSection minSection = section.getConfigurationSection("minimum");
        ConfigurationSection maxSection = section.getConfigurationSection("maximum");
        if (minSection == null || maxSection == null) {
            return null;
        }

        int x1 = minSection.getInt("x");
        int y1 = minSection.getInt("y");
        int z1 = minSection.getInt("z");
        int x2 = maxSection.getInt("x");
        int y2 = maxSection.getInt("y");
        int z2 = maxSection.getInt("z");

        int priority = section.getInt("priority", 0);

        Map<RegionFlag, FlagState> flags = new HashMap<RegionFlag, FlagState>();
        ConfigurationSection flagsSection = section.getConfigurationSection("flags");
        if (flagsSection != null) {
            for (String flagKey : flagsSection.getKeys(false)) {
                RegionFlag flag = RegionFlag.fromString(flagKey);
                if (flag == null) {
                    continue;
                }
                String value = flagsSection.getString(flagKey);
                FlagState state = FlagState.fromString(value);
                if (state != null && state != FlagState.INHERIT) {
                    flags.put(flag, state);
                }
            }
        }

        return new CuboidRegion(id, worldName, worldUuid,
                x1, y1, z1, x2, y2, z2,
                priority, flags);
    }

    public static void saveRegions(File file, List<CuboidRegion> regions, Logger logger) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection regionsSection = config.createSection("regions");

        for (int i = 0; i < regions.size(); i++) {
            CuboidRegion region = regions.get(i);
            ConfigurationSection regionSection = regionsSection.createSection(region.getId());

            ConfigurationSection worldSection = regionSection.createSection("world");
            worldSection.set("name", region.getWorldName() != null ? region.getWorldName() : "");
            worldSection.set("uuid", region.getWorldUuid() != null ? region.getWorldUuid() : "");

            ConfigurationSection minSection = regionSection.createSection("minimum");
            minSection.set("x", region.getMinX());
            minSection.set("y", region.getMinY());
            minSection.set("z", region.getMinZ());

            ConfigurationSection maxSection = regionSection.createSection("maximum");
            maxSection.set("x", region.getMaxX());
            maxSection.set("y", region.getMaxY());
            maxSection.set("z", region.getMaxZ());

            regionSection.set("priority", region.getPriority());

            ConfigurationSection flagsSection = regionSection.createSection("flags");
            Map<RegionFlag, FlagState> flags = region.getFlags();
            for (Map.Entry<RegionFlag, FlagState> entry : flags.entrySet()) {
                if (entry.getValue() != FlagState.INHERIT) {
                    flagsSection.set(entry.getKey().getPermissionKey(), entry.getValue().name());
                }
            }
        }

        try {
            config.save(file);
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("Errore nel salvataggio del file regioni: " + e.getMessage());
            }
        }
    }
}
