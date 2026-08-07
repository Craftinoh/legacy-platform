package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.mode.MatchMode;

/** Risultato stabile scritto una volta al termine della partita. */
public final class MatchResultRecord {
    private final String matchId;
    private final MatchMode mode;
    private final String winnerTeamId;
    private final long finishedAtEpochMillis;

    public MatchResultRecord(String matchId, MatchMode mode,
                             String winnerTeamId, long finishedAtEpochMillis) {
        if (matchId == null || matchId.trim().isEmpty() || mode == null) {
            throw new IllegalArgumentException("Risultato partita non valido");
        }
        this.matchId = matchId;
        this.mode = mode;
        this.winnerTeamId = winnerTeamId;
        this.finishedAtEpochMillis = finishedAtEpochMillis;
    }
    public String getMatchId() { return matchId; }
    public MatchMode getMode() { return mode; }
    public String getWinnerTeamId() { return winnerTeamId; }
    public long getFinishedAtEpochMillis() { return finishedAtEpochMillis; }
}
