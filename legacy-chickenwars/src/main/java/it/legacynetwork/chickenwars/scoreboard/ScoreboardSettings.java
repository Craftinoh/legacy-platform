package it.legacynetwork.chickenwars.scoreboard;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable scoreboard configuration. */
public final class ScoreboardSettings {

    private final boolean enabled;
    private final int updateTicks;
    private final String footer;
    private final Map<String, ScoreboardLayout> layouts;

    public ScoreboardSettings(boolean enabled, int updateTicks, String footer,
                              Map<String, ScoreboardLayout> layouts) {
        this.enabled = enabled;
        this.updateTicks = Math.max(1, updateTicks);
        this.footer = footer == null ? "" : footer.trim();
        Map<String, ScoreboardLayout> copy =
                new LinkedHashMap<String, ScoreboardLayout>();
        if (layouts != null) {
            copy.putAll(layouts);
        }
        this.layouts = Collections.unmodifiableMap(copy);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getUpdateTicks() {
        return updateTicks;
    }

    public String getFooter() {
        return footer;
    }

    public ScoreboardLayout getLayout(String id) {
        return id == null ? null : layouts.get(id);
    }

    public Map<String, ScoreboardLayout> getLayouts() {
        return layouts;
    }
}
