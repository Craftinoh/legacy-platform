package it.legacynetwork.chickenwars.scoreboard;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/** Renderer puro di layout e placeholder, con limiti sidebar 1.8.8. */
public final class ScoreboardRenderer {

    public RenderedScoreboard render(ScoreboardLayout layout,
                                     ScoreboardPlaceholderModel model) {
        if (layout == null || model == null) {
            throw new IllegalArgumentException("Scoreboard incompleta");
        }
        List<String> lines = new ArrayList<String>();
        for (String raw : layout.getLines()) {
            List<String> expansion = model.expansion(raw);
            if (expansion == null) {
                lines.add(color(model.render(raw)));
            } else {
                for (String line : expansion) {
                    if (lines.size() >= 15) {
                        break;
                    }
                    lines.add(color(model.render(line)));
                }
            }
            if (lines.size() >= 15) {
                break;
            }
        }
        return new RenderedScoreboard(color(model.render(layout.getTitle())),
                lines);
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&',
                value == null ? "" : value);
    }
}
