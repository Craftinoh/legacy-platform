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
    private final Map<Integer, Team> teams = new HashMap<Integer, Team>();

    public PlayerScoreboard(Scoreboard scoreboard, String title) {
        this.scoreboard = scoreboard;
        this.objective = scoreboard.registerNewObjective("legacyLobby", "dummy");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.objective.setDisplayName(title);
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void update(String title, List<ScoreboardLine> lines) {
        if (!objective.getDisplayName().equals(title)) {
            objective.setDisplayName(title);
        }
        for (int index = 0; index < lines.size(); index++) {
            ScoreboardLine line = lines.get(index);
            Team team = teams.get(index);
            if (team == null) {
                team = scoreboard.registerNewTeam("line" + index);
                team.addEntry(line.getEntry());
                teams.put(index, team);
                objective.getScore(line.getEntry()).setScore(lines.size() - index);
            }
            if (!team.getPrefix().equals(line.getPrefix())) {
                team.setPrefix(line.getPrefix());
            }
            if (!team.getSuffix().equals(line.getSuffix())) {
                team.setSuffix(line.getSuffix());
            }
        }
    }
}
