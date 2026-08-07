package it.legacynetwork.chickenwars.scoreboard;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreboardRendererTest {

    @Test
    void expandsListsReplacesPlaceholdersAndAppliesLegacyColors() {
        ScoreboardLayout layout = new ScoreboardLayout("playing", "&6{name}",
                Arrays.asList("&7{timer}", "{teams}", "&f{value}"));
        ScoreboardPlaceholderModel model = new ScoreboardPlaceholderModel()
                .value("name", "Chicken Wars").value("timer", "01:30")
                .value("value", 4).lines("teams",
                        Arrays.asList("&cRed", "&9Blue"));

        RenderedScoreboard rendered = new ScoreboardRenderer()
                .render(layout, model);

        assertEquals(ChatColor.GOLD + "Chicken Wars", rendered.getTitle());
        assertEquals(4, rendered.getLines().size());
        assertEquals(ChatColor.RED + "Red", rendered.getLines().get(1));
        assertFalse(rendered.getLines().get(3).contains("{value}"));
    }

    @Test
    void sidebarIsBoundedToFifteenLinesAndEveryLineFitsLegacySegments() {
        String[] values = new String[20];
        Arrays.fill(values, "&aA very long scoreboard line for Minecraft 1.8");
        ScoreboardLayout layout = new ScoreboardLayout("bounded", "title",
                Arrays.asList("{rows}"));
        RenderedScoreboard rendered = new ScoreboardRenderer().render(layout,
                new ScoreboardPlaceholderModel().lines("rows",
                        Arrays.asList(values)));

        assertEquals(15, rendered.getLines().size());
        for (int index = 0; index < rendered.getLines().size(); index++) {
            ScoreboardLine line = ScoreboardLine.of(index,
                    rendered.getLines().get(index));
            assertTrue(line.getPrefix().length() <= 16);
            assertTrue(line.getSuffix().length() <= 16);
        }
    }
}
