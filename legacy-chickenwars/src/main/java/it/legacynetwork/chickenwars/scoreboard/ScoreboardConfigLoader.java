package it.legacynetwork.chickenwars.scoreboard;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Loads and validates scoreboard.yml. */
public final class ScoreboardConfigLoader {

    private static final String[] REQUIRED_LAYOUTS = {
            "waiting", "starting", "playing-eight-teams", "playing-compact",
            "duel", "spectator", "ending", "ending-duel"
    };

    private ScoreboardConfigLoader() {
    }

    public static ScoreboardSettings load(File file, Logger logger) {
        if (file == null || !file.isFile()) {
            if (logger != null) {
                logger.warning("scoreboard.yml mancante.");
            }
            return null;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(file);
            return parse(configuration);
        } catch (IOException exception) {
            warn(logger, "Impossibile leggere scoreboard.yml: "
                    + exception.getMessage());
        } catch (InvalidConfigurationException exception) {
            warn(logger, "scoreboard.yml non valido: "
                    + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            warn(logger, "scoreboard.yml incompleto: "
                    + exception.getMessage());
        }
        return null;
    }

    public static ScoreboardSettings parse(YamlConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configurazione mancante");
        }
        ConfigurationSection layoutsSection =
                configuration.getConfigurationSection("layouts");
        if (layoutsSection == null) {
            throw new IllegalArgumentException("sezione layouts mancante");
        }

        Map<String, ScoreboardLayout> layouts =
                new LinkedHashMap<String, ScoreboardLayout>();
        for (String id : layoutsSection.getKeys(false)) {
            ConfigurationSection section =
                    layoutsSection.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            String title = section.getString("title", "&6&lCHICKEN WARS");
            List<String> lines = section.getStringList("lines");
            if (lines.size() > 15) {
                throw new IllegalArgumentException(
                        "layout " + id + " supera le 15 righe");
            }
            layouts.put(id, new ScoreboardLayout(id, title, lines));
        }

        for (String required : REQUIRED_LAYOUTS) {
            if (!layouts.containsKey(required)) {
                throw new IllegalArgumentException(
                        "layout obbligatorio mancante: " + required);
            }
        }

        return new ScoreboardSettings(
                configuration.getBoolean("enabled", true),
                configuration.getInt("update-ticks", 10),
                configuration.getString("footer", ""),
                layouts);
    }

    private static void warn(Logger logger, String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }
}
