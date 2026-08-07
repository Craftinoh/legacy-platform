package it.legacynetwork.chickenwars.scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RenderedScoreboard {
    private final String title;private final List<String> lines;
    public RenderedScoreboard(String title,List<String> lines){this.title=title;this.lines=Collections.unmodifiableList(new ArrayList<String>(lines));}
    public String getTitle(){return title;}public List<String> getLines(){return lines;}
}
