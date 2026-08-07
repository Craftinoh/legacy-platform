package it.legacynetwork.chickenwars.scoreboard;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
        assertNotNull(settings.getLayout("playing-eight-teams"));
        assertNotNull(settings.getLayout("playing-compact"));
        assertNotNull(settings.getLayout("duel"));
        assertNotNull(settings.getLayout("spectator"));
        assertNotNull(settings.getLayout("ending"));
        for (ScoreboardLayout layout : settings.getLayouts().values()) {
            assertTrue(layout.getLines().size() <= 15,
                    layout.getId() + " supera il limite della sidebar");
        }
    }
}
