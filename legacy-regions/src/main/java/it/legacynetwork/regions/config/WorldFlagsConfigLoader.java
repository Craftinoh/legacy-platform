package it.legacynetwork.regions.config;

import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionFlag;
import it.legacynetwork.regions.model.WorldRegionFlags;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class WorldFlagsConfigLoader {

    private WorldFlagsConfigLoader() {
    }

    public static List<WorldRegionFlags> load(File file, Logger logger) {
        if (!file.exists()) {
            return new ArrayList<WorldRegionFlags>();
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection == null) {
            return new ArrayList<WorldRegionFlags>();
        }

        List<WorldRegionFlags> result = new ArrayList<WorldRegionFlags>();
        for (String worldName : worldsSection.getKeys(false)) {
            ConfigurationSection worldSection =
                    worldsSection.getConfigurationSection(worldName);
            if (worldSection == null) {
                logger.warning("worlds.yml: sezione mondo '" + worldName + "' non valida, saltata.");
                return null;
            }
            String uuidStr = worldSection.getString("uuid", "");
            if (uuidStr != null && !uuidStr.isEmpty()) {
                try {
                    UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    logger.warning("worlds.yml: UUID non valido per '" + worldName + "': " + uuidStr);
                    return null;
                }
            }

            ConfigurationSection flagsSection =
                    worldSection.getConfigurationSection("flags");
            if (flagsSection == null) {
                logger.warning("worlds.yml: sezione flags mancante per '" + worldName + "', saltata.");
                result.add(new WorldRegionFlags(worldName, uuidStr,
                        new EnumMap<RegionFlag, FlagState>(RegionFlag.class)));
                continue;
            }

            Map<RegionFlag, FlagState> flags =
                    new EnumMap<RegionFlag, FlagState>(RegionFlag.class);
            for (String flagKey : flagsSection.getKeys(false)) {
                RegionFlag flag = RegionFlag.fromString(flagKey);
                if (flag == null) {
                    logger.warning("worlds.yml: flag sconosciuto '" + flagKey
                            + "' per il mondo '" + worldName + "'");
                    return null;
                }
                FlagState state = FlagState.fromString(flagsSection.getString(flagKey));
                if (state == null) {
                    logger.warning("worlds.yml: stato non valido '" + flagsSection.getString(flagKey)
                            + "' per il flag '" + flagKey + "' nel mondo '" + worldName + "'");
                    return null;
                }
                flags.put(flag, state);
            }
            result.add(new WorldRegionFlags(worldName, uuidStr, flags));
        }
        return result;
    }

    public static boolean save(File file, List<WorldRegionFlags> worldFlags,
                                 Logger logger) {
        if (worldFlags == null) {
            return false;
        }
        FileConfiguration config = new YamlConfiguration();
        for (WorldRegionFlags wf : worldFlags) {
            if (wf.getFlags().isEmpty()) {
                continue;
            }
            String path = "worlds." + wf.getWorldName();
            config.set(path + ".uuid", wf.getWorldUuid() != null ? wf.getWorldUuid() : "");
            for (Map.Entry<RegionFlag, FlagState> entry : wf.getFlags().entrySet()) {
                config.set(path + ".flags." + entry.getKey().getPermissionKey(),
                        entry.getValue().name());
            }
        }

        File tempFile = new File(file.getParentFile(),
                file.getName() + ".tmp");
        try {
            tempFile.getParentFile().mkdirs();
            config.save(tempFile);
            Files.move(tempFile.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            logger.warning("worlds.yml: impossibile salvare: " + e.getMessage());
            tempFile.delete();
            return false;
        }
    }
}
