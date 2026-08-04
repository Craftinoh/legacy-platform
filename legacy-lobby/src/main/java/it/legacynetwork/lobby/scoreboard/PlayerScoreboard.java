package it.legacynetwork.lobby.scoreboard;

import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PlayerScoreboard {
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<Integer, Team> teams = new HashMap<>();
    private int lastLineCount = -1;
    private String lastTitle = "";

    public PlayerScoreboard(Scoreboard scoreboard, String title) {
        this.scoreboard = scoreboard;
        this.objective = scoreboard.registerNewObjective("lobbyBoard", "dummy");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.objective.setDisplayName(title);
        this.lastTitle = title;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void update(String title, List<ScoreboardLine> lines) {
        boolean structureChanged = lastLineCount != lines.size()
                || !lastTitle.equals(title);
        if (structureChanged) {
            rebuild(title, lines);
            lastLineCount = lines.size();
            lastTitle = title;
            return;
        }
        if (!objective.getDisplayName().equals(title)) {
            objective.setDisplayName(title);
        }
        for (int i = 0; i < lines.size(); i++) {
            ScoreboardLine line = lines.get(i);
            Team team = teams.get(i);
            if (team == null) {
                continue;
            }
            if (!team.getPrefix().equals(line.getPrefix())) {
                team.setPrefix(line.getPrefix());
            }
            if (!team.getSuffix().equals(line.getSuffix())) {
                team.setSuffix(line.getSuffix());
            }
        }
    }

    private void rebuild(String title, List<ScoreboardLine> lines) {
        for (Team team : teams.values()) {
            team.unregister();
        }
        teams.clear();
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        objective.setDisplayName(title);
        for (int i = 0; i < lines.size(); i++) {
            ScoreboardLine line = lines.get(i);
            String teamName = "l" + i;
            Team team = scoreboard.registerNewTeam(teamName);
            team.addEntry(line.getEntry());
            team.setPrefix(line.getPrefix());
            team.setSuffix(line.getSuffix());
            teams.put(i, team);
            objective.getScore(line.getEntry()).setScore(lines.size() - i);
        }
    }
}
