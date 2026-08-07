package it.legacynetwork.chickenwars.scoreboard;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreboardConfigLoaderTest {

    @Test
    void bundledConfigurationContainsAllRequiredLayouts() throws Exception {
        InputStream input = getClass().getClassLoader()
                .getResourceAsStream("scoreboard.yml");
        assertNotNull(input);

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                new InputStreamReader(input, StandardCharsets.UTF_8));
        ScoreboardSettings settings = ScoreboardConfigLoader.parse(configuration);

        assertTrue(settings.isEnabled());
        assertEquals(10, settings.getUpdateTicks());
        assertNotNull(settings.getLayout("waiting"));
        assertNotNull(settings.getLayout("starting"));
        assertNotNull(settings.getLayout("playing-eight-teams"));
        assertNotNull(settings.getLayout("playing-compact"));
        assertNotNull(settings.getLayout("duel"));
        assertNotNull(settings.getLayout("spectator"));
        assertNotNull(settings.getLayout("ending"));
        assertNotNull(settings.getLayout("ending-duel"));
        for (ScoreboardLayout layout : settings.getLayouts().values()) {
            assertTrue(layout.getLines().size() <= 15,
                    layout.getId() + " supera il limite della sidebar");
        }
    }

    @Test
    void everyBundledLayoutRendersWithoutUnresolvedPlaceholders() throws Exception {
        InputStream input = getClass().getClassLoader()
                .getResourceAsStream("scoreboard.yml");
        assertNotNull(input);
        ScoreboardSettings settings = ScoreboardConfigLoader.parse(
                YamlConfiguration.loadConfiguration(new InputStreamReader(
                        input, StandardCharsets.UTF_8)));
        ScoreboardPlaceholderModel model = completeModel();
        ScoreboardRenderer renderer = new ScoreboardRenderer();

        for (ScoreboardLayout layout : settings.getLayouts().values()) {
            RenderedScoreboard rendered = renderer.render(layout, model);
            assertTrue(rendered.getLines().size() <= 15, layout.getId());
            for (int index = 0; index < rendered.getLines().size(); index++) {
                String line = rendered.getLines().get(index);
                assertTrue(!line.contains("{") && !line.contains("}"),
                        layout.getId() + ": " + line);
                ScoreboardLine legacy = ScoreboardLine.of(index, line);
                assertTrue(legacy.getPrefix().length() <= 16);
                assertTrue(legacy.getSuffix().length() <= 16);
            }
        }
        assertTrue(settings.getLayout("ending-duel").getLines().stream()
                .noneMatch(line -> line.contains("reward_")));
    }

    private ScoreboardPlaceholderModel completeModel() {
        ScoreboardPlaceholderModel model = new ScoreboardPlaceholderModel();
        String[] keys = {"date", "map", "mode", "players", "max_players",
                "waiting_status", "starting_status", "next_event_name",
                "next_event_time", "team_red", "team_blue", "team_green",
                "team_yellow", "team_aqua", "team_white", "team_pink",
                "team_gray", "kills", "final_kills", "chicken_kills",
                "chicken_health", "chicken_max_health", "chicken_shield",
                "chicken_max_shield", "feathers", "own_team_line",
                "enemy_team_line", "training_notice", "winner_team", "deaths",
                "resources", "reward_xp", "reward_coins", "level", "duration",
                "server_id", "footer"};
        for (String key : keys) model.value(key, "x");
        String[] labels = {"map", "mode", "players", "chicken", "shield",
                "kills", "final_kills", "chickens", "feathers", "spectator",
                "winner", "deaths", "resources", "duration", "level"};
        for (String label : labels) model.value("label_" + label, "label");
        return model.lines("team_lines", Arrays.asList("red", "blue"));
    }
}
