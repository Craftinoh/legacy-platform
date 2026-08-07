package it.legacynetwork.chickenwars.arena;

import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import it.legacynetwork.chickenwars.model.TeamColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/** Lettura e scrittura dei file arena in {@code arenas/}. */
public final class ArenaConfigLoader {

    private ArenaConfigLoader() {
    }

    public static ArenaDefinition load(File file, Logger logger) {
        if (file == null || !file.isFile()) {
            return null;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(file);
            return read(configuration, stripExtension(file.getName()), logger);
        } catch (IOException exception) {
            logger.warning("Impossibile leggere " + file.getName()
                    + ": " + exception.getMessage());
        } catch (InvalidConfigurationException exception) {
            logger.warning("File arena non valido " + file.getName()
                    + ": " + exception.getMessage());
        } catch (RuntimeException exception) {
            logger.warning("Arena " + file.getName() + " ignorata: "
                    + exception.getMessage());
        }
        return null;
    }

    public static List<ArenaDefinition> loadAll(File folder, Logger logger) {
        List<ArenaDefinition> result = new ArrayList<ArenaDefinition>();
        if (folder == null || !folder.isDirectory()) {
            return result;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return result;
        }
        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT)
                    .endsWith(".yml")) {
                continue;
            }
            ArenaDefinition arena = load(file, logger);
            if (arena != null) {
                result.add(arena);
            }
        }
        return result;
    }

    private static ArenaDefinition read(YamlConfiguration configuration,
                                        String fallbackId, Logger logger) {
        ConfigurationSection arenaSection =
                configuration.getConfigurationSection("arena");
        if (arenaSection == null) {
            throw new IllegalArgumentException("sezione arena mancante");
        }

        ArenaDefinition arena = new ArenaDefinition(
                arenaSection.getString("id", fallbackId));
        arena.setDisplayName(arenaSection.getString("display-name"));
        arena.setEnabled(arenaSection.getBoolean("enabled", false));
        arena.setWorld(arenaSection.getString("world"));
        arena.setMinimumPlayers(arenaSection.getInt("minimum-players", 2));
        arena.setPlayersPerTeam(arenaSection.getInt("players-per-team", 1));

        String rawMode = arenaSection.getString("mode");
        MatchMode mode = MatchMode.fromString(rawMode);
        if (rawMode != null && mode == null) {
            logger.warning("Arena " + arena.getId() + ": modalita' non valida '"
                    + rawMode + "'; verra' dedotta automaticamente.");
        }
        arena.setMode(mode);

        ConfigurationSection locations =
                configuration.getConfigurationSection("locations");
        if (locations != null) {
            arena.setLobby(SimpleLocation.parse(locations.getString("lobby")));
            arena.setSpectator(SimpleLocation.parse(
                    locations.getString("spectator")));
        }

        ConfigurationSection region =
                configuration.getConfigurationSection("region");
        if (region != null) {
            arena.setPos1(SimpleLocation.parse(region.getString("pos1")));
            arena.setPos2(SimpleLocation.parse(region.getString("pos2")));
            arena.setVoidY(region.getInt("void-y", 0));
            arena.setMaximumBuildY(region.getInt("maximum-build-y", 256));
        }

        readTeams(configuration.getConfigurationSection("teams"), arena, logger);
        readGenerators(configuration.getConfigurationSection("generators"),
                arena, logger);
        return arena;
    }

    private static void readTeams(ConfigurationSection section,
                                  ArenaDefinition arena, Logger logger) {
        if (section == null) {
            return;
        }
        for (String teamId : section.getKeys(false)) {
            ConfigurationSection teamSection =
                    section.getConfigurationSection(teamId);
            if (teamSection == null) {
                continue;
            }
            TeamColor color = TeamColor.fromString(
                    teamSection.getString("color"));
            if (color == null) {
                logger.warning("Squadra " + teamId + " dell'arena "
                        + arena.getId() + " ignorata: colore non valido.");
                continue;
            }
            TeamDefinition team = new TeamDefinition(teamId,
                    teamSection.getString("display-name"), color,
                    teamSection.getInt("maximum-players",
                            arena.getPlayersPerTeam()));
            team.setSpawn(SimpleLocation.parse(teamSection.getString("spawn")));
            team.setNest(SimpleLocation.parse(teamSection.getString("nest")));
            team.setChicken(SimpleLocation.parse(
                    teamSection.getString("chicken")));
            team.setShop(SimpleLocation.parse(teamSection.getString("shop")));
            team.setUpgrades(SimpleLocation.parse(
                    teamSection.getString("upgrades")));
            arena.addTeam(team);
        }
    }

    private static void readGenerators(ConfigurationSection section,
                                       ArenaDefinition arena, Logger logger) {
        if (section == null) {
            return;
        }
        for (String generatorId : section.getKeys(false)) {
            ConfigurationSection generatorSection =
                    section.getConfigurationSection(generatorId);
            if (generatorSection == null) {
                continue;
            }
            ResourceType type = ResourceType.fromString(
                    generatorSection.getString("type"));
            SimpleLocation location = SimpleLocation.parse(
                    generatorSection.getString("location"));
            if (type == null || location == null) {
                logger.warning("Generatore " + generatorId + " dell'arena "
                        + arena.getId()
                        + " ignorato: tipo o posizione non validi.");
                continue;
            }
            arena.addGenerator(new GeneratorDefinition(generatorId, type,
                    location, generatorSection.getString("team"),
                    generatorSection.getInt("level", 1),
                    generatorSection.getBoolean("hologram", true)));
        }
    }

    public static boolean save(File file, ArenaDefinition arena, Logger logger) {
        if (file == null || arena == null) {
            return false;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            logger.warning("Impossibile creare la cartella " + parent.getPath());
            return false;
        }

        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("arena.id", arena.getId());
        configuration.set("arena.display-name", arena.getDisplayName());
        configuration.set("arena.enabled", arena.isEnabled());
        configuration.set("arena.world", arena.getWorld());
        configuration.set("arena.mode", arena.getMode().getKey());
        configuration.set("arena.minimum-players", arena.getMinimumPlayers());
        configuration.set("arena.players-per-team", arena.getPlayersPerTeam());
        configuration.set("locations.lobby", serialize(arena.getLobby()));
        configuration.set("locations.spectator", serialize(arena.getSpectator()));
        configuration.set("region.pos1", serialize(arena.getPos1()));
        configuration.set("region.pos2", serialize(arena.getPos2()));
        configuration.set("region.void-y", arena.getVoidY());
        configuration.set("region.maximum-build-y", arena.getMaximumBuildY());

        for (TeamDefinition team : arena.getTeams()) {
            String path = "teams." + team.getId() + ".";
            configuration.set(path + "display-name", team.getDisplayName());
            configuration.set(path + "color", team.getColor().name());
            configuration.set(path + "maximum-players", team.getMaxPlayers());
            configuration.set(path + "spawn", serialize(team.getSpawn()));
            configuration.set(path + "nest", serialize(team.getNest()));
            configuration.set(path + "chicken", serialize(team.getChicken()));
            configuration.set(path + "shop", serialize(team.getShop()));
            configuration.set(path + "upgrades", serialize(team.getUpgrades()));
        }

        for (GeneratorDefinition generator : arena.getGenerators()) {
            String path = "generators." + generator.getId() + ".";
            configuration.set(path + "type", generator.getType().name());
            configuration.set(path + "location",
                    serialize(generator.getLocation()));
            configuration.set(path + "team", generator.getTeamId());
            configuration.set(path + "level", generator.getLevel());
            configuration.set(path + "hologram", generator.hasHologram());
        }

        try {
            configuration.save(file);
            return true;
        } catch (IOException exception) {
            logger.warning("Impossibile salvare " + file.getName()
                    + ": " + exception.getMessage());
            return false;
        }
    }

    private static String serialize(SimpleLocation location) {
        return location == null ? null : location.serialize();
    }

    private static String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index <= 0 ? fileName : fileName.substring(0, index);
    }
}
