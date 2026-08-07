package it.legacynetwork.chickenwars.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scoreboard laterale personale di un giocatore in partita.
 *
 * <p>Ricostruisce le righe solo quando cambia il numero di voci o il titolo; le
 * variazioni di testo riusano i team esistenti per evitare sfarfallio.</p>
 */
public final class GameScoreboard {

    private static final String OBJECTIVE_NAME = "cwBoard";

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<Integer, Team> teams = new HashMap<Integer, Team>();

    private List<String> lastLines = new ArrayList<String>();
    private String lastTitle;

    public GameScoreboard(String title) {
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.objective.setDisplayName(truncate(title));
        this.lastTitle = truncate(title);
    }

    /**
     * Aggiorna titolo e righe della scoreboard.
     *
     * @param title titolo gia' colorato
     * @param lines righe gia' colorate, dalla prima all'ultima
     */
    public void update(String title, List<String> lines) {
        String safeTitle = truncate(title);
        List<String> safeLines = lines == null
                ? new ArrayList<String>() : new ArrayList<String>(lines);

        if (safeLines.size() != lastLines.size()) {
            rebuild(safeTitle, safeLines);
            return;
        }
        if (!safeTitle.equals(lastTitle)) {
            objective.setDisplayName(safeTitle);
            lastTitle = safeTitle;
        }
        for (int i = 0; i < safeLines.size(); i++) {
            if (safeLines.get(i).equals(lastLines.get(i))) {
                continue;
            }
            Team team = teams.get(i);
            if (team == null) {
                continue;
            }
            ScoreboardLine line = ScoreboardLine.of(i, safeLines.get(i));
            team.setPrefix(line.getPrefix());
            team.setSuffix(line.getSuffix());
        }
        lastLines = safeLines;
    }

    private void rebuild(String title, List<String> lines) {
        for (Team team : teams.values()) {
            try {
                team.unregister();
            } catch (IllegalStateException ignored) {
                // Il team era gia' stato rimosso: nulla da fare.
            }
        }
        teams.clear();
        for (String entry : new ArrayList<String>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        objective.setDisplayName(title);
        lastTitle = title;

        for (int i = 0; i < lines.size(); i++) {
            ScoreboardLine line = ScoreboardLine.of(i, lines.get(i));
            Team team = scoreboard.registerNewTeam("cw" + i);
            team.addEntry(line.getEntry());
            team.setPrefix(line.getPrefix());
            team.setSuffix(line.getSuffix());
            teams.put(i, team);
            objective.getScore(line.getEntry()).setScore(lines.size() - i);
        }
        lastLines = lines;
    }

    /**
     * Applica la scoreboard al giocatore indicato.
     */
    public void apply(Player player) {
        if (player != null && player.isOnline()) {
            player.setScoreboard(scoreboard);
        }
    }

    /**
     * Riporta il giocatore alla scoreboard principale del server.
     */
    public static void clear(Player player) {
        if (player != null && player.isOnline()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private String truncate(String value) {
        String safe = value == null ? "" : value;
        return safe.length() > 32 ? safe.substring(0, 32) : safe;
    }
}
