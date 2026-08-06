package it.legacynetwork.regions.config;

import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionFlag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
        try {
            return loadRegionsStrict(file);
        } catch (Exception exception) {
            if (logger != null) {
                logger.warning("Configurazione regioni non valida: "
                        + exception.getMessage());
            }
            return null;
        }
    }

    public static List<CuboidRegion> loadRegionsStrict(File file)
            throws IOException, InvalidConfigurationException {
        if (file == null || !file.isFile()) {
            throw new IOException("File regioni non trovato");
        }

        YamlConfiguration config = new YamlConfiguration();
        config.load(file);
        ConfigurationSection regionsSection =
                config.getConfigurationSection("regions");
        if (regionsSection == null) {
            throw new InvalidConfigurationException(
                    "Sezione 'regions' mancante");
        }

        List<CuboidRegion> regions = new ArrayList<CuboidRegion>();
        Set<String> normalizedIds = new HashSet<String>();
        for (String rawId : regionsSection.getKeys(false)) {
            String normalizedId = CuboidRegion.normalizeId(rawId);
            if (!normalizedIds.add(normalizedId)) {
                throw new InvalidConfigurationException(
                        "ID regione duplicato: " + normalizedId);
            }
            ConfigurationSection regionSection =
                    regionsSection.getConfigurationSection(rawId);
            if (regionSection == null) {
                throw new InvalidConfigurationException(
                        "Regione non valida: " + rawId);
            }
            regions.add(parseRegion(normalizedId, regionSection));
        }
        return Collections.unmodifiableList(regions);
    }

    private static CuboidRegion parseRegion(String id,
                                            ConfigurationSection section)
            throws InvalidConfigurationException {
        ConfigurationSection worldSection =
                requireSection(section, "world", id);
        String worldName = trimToNull(worldSection.getString("name"));
        String worldUuid = trimToNull(worldSection.getString("uuid"));
        if (worldName == null && worldUuid == null) {
            throw new InvalidConfigurationException(
                    "Mondo mancante per la regione '" + id + "'");
        }

        ConfigurationSection minimum =
                requireSection(section, "minimum", id);
        ConfigurationSection maximum =
                requireSection(section, "maximum", id);
        int minX = requireInt(minimum, "x", id);
        int minY = requireInt(minimum, "y", id);
        int minZ = requireInt(minimum, "z", id);
        int maxX = requireInt(maximum, "x", id);
        int maxY = requireInt(maximum, "y", id);
        int maxZ = requireInt(maximum, "z", id);

        if (section.contains("priority") && !section.isInt("priority")) {
            throw new InvalidConfigurationException(
                    "Priorita' non intera per la regione '" + id + "'");
        }
        int priority = section.getInt("priority", 0);

        Map<RegionFlag, FlagState> flags =
                new HashMap<RegionFlag, FlagState>();
        ConfigurationSection flagsSection =
                section.getConfigurationSection("flags");
        if (flagsSection != null) {
            for (String flagKey : flagsSection.getKeys(false)) {
                RegionFlag flag = RegionFlag.fromString(flagKey);
                if (flag == null) {
                    throw new InvalidConfigurationException(
                            "Flag sconosciuto '" + flagKey
                                    + "' nella regione '" + id + "'");
                }
                FlagState state = FlagState.fromString(
                        flagsSection.getString(flagKey));
                if (state == null) {
                    throw new InvalidConfigurationException(
                            "Valore non valido per il flag '" + flagKey
                                    + "' nella regione '" + id + "'");
                }
                if (state != FlagState.INHERIT) {
                    flags.put(flag, state);
                }
            }
        }

        try {
            return new CuboidRegion(id, worldName, worldUuid,
                    minX, minY, minZ, maxX, maxY, maxZ,
                    priority, flags);
        } catch (IllegalArgumentException exception) {
            throw new InvalidConfigurationException(
                    "Regione '" + id + "': " + exception.getMessage());
        }
    }

    private static ConfigurationSection requireSection(
            ConfigurationSection parent, String key, String regionId)
            throws InvalidConfigurationException {
        ConfigurationSection section = parent.getConfigurationSection(key);
        if (section == null) {
            throw new InvalidConfigurationException(
                    "Sezione '" + key + "' mancante nella regione '"
                            + regionId + "'");
        }
        return section;
    }

    private static int requireInt(ConfigurationSection section,
                                  String key, String regionId)
            throws InvalidConfigurationException {
        if (!section.contains(key) || !section.isInt(key)) {
            throw new InvalidConfigurationException(
                    "Coordinata '" + key + "' non valida nella regione '"
                            + regionId + "'");
        }
        return section.getInt(key);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean saveRegions(File file, List<CuboidRegion> regions,
                                      Logger logger) {
        try {
            saveRegionsStrict(file, regions);
            return true;
        } catch (Exception exception) {
            if (logger != null) {
                logger.warning("Impossibile salvare regions.yml: "
                        + exception.getMessage());
            }
            return false;
        }
    }

    public static void saveRegionsStrict(File file, List<CuboidRegion> regions)
            throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Impossibile creare la cartella del plugin");
        }

        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection regionsSection = config.createSection("regions");
        for (CuboidRegion region : regions) {
            ConfigurationSection regionSection =
                    regionsSection.createSection(region.getId());
            ConfigurationSection worldSection =
                    regionSection.createSection("world");
            worldSection.set("name",
                    region.getWorldName() == null ? "" : region.getWorldName());
            worldSection.set("uuid",
                    region.getWorldUuid() == null ? "" : region.getWorldUuid());

            ConfigurationSection minimum =
                    regionSection.createSection("minimum");
            minimum.set("x", region.getMinX());
            minimum.set("y", region.getMinY());
            minimum.set("z", region.getMinZ());

            ConfigurationSection maximum =
                    regionSection.createSection("maximum");
            maximum.set("x", region.getMaxX());
            maximum.set("y", region.getMaxY());
            maximum.set("z", region.getMaxZ());
            regionSection.set("priority", region.getPriority());

            ConfigurationSection flagsSection =
                    regionSection.createSection("flags");
            for (Map.Entry<RegionFlag, FlagState> entry
                    : region.getFlags().entrySet()) {
                flagsSection.set(entry.getKey().getPermissionKey(),
                        entry.getValue().name());
            }
        }

        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        config.save(temporary);
        try {
            Files.move(temporary.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (temporary.exists() && !temporary.equals(file)) {
                temporary.delete();
            }
        }
    }
}
