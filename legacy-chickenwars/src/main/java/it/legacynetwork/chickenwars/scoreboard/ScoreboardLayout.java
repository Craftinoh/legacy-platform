package it.legacynetwork.chickenwars.scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Titolo e righe di un layout scoreboard.
 */
public final class ScoreboardLayout {

    private final String id;
    private final String title;
    private final List<String> lines;

    public ScoreboardLayout(String id, String title, List<String> lines) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID layout mancante");
        }
        this.id = id.trim();
        this.title = title == null ? "" : title;
        this.lines = Collections.unmodifiableList(lines == null
                ? new ArrayList<String>() : new ArrayList<String>(lines));
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getLines() {
        return lines;
    }
}
