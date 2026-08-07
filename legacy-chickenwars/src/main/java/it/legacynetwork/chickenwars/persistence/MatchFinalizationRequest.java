package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Input completo e immutabile della singola finalizzazione. */
public final class MatchFinalizationRequest {
    private final String matchId;
    private final MatchMode mode;
    private final String winnerTeamId;
    private final List<MatchParticipantRecord> participants;
    private final long finishedAtEpochMillis;
    private final long maximumExperience;

    public MatchFinalizationRequest(String matchId, MatchMode mode,
            String winnerTeamId, List<MatchParticipantRecord> participants,
            long finishedAtEpochMillis) {
        this(matchId, mode, winnerTeamId, participants,
                finishedAtEpochMillis, Long.MAX_VALUE);
    }

    public MatchFinalizationRequest(String matchId, MatchMode mode,
            String winnerTeamId, List<MatchParticipantRecord> participants,
            long finishedAtEpochMillis, long maximumExperience) {
        if (matchId == null || matchId.trim().isEmpty() || mode == null
                || participants == null) {
            throw new IllegalArgumentException("Finalizzazione non valida");
        }
        this.matchId = matchId; this.mode = mode;
        this.winnerTeamId = winnerTeamId;
        this.participants = Collections.unmodifiableList(
                new ArrayList<MatchParticipantRecord>(participants));
        this.finishedAtEpochMillis = finishedAtEpochMillis;
        this.maximumExperience = Math.max(0L, maximumExperience);
    }
    public String getMatchId() { return matchId; }
    public MatchMode getMode() { return mode; }
    public String getWinnerTeamId() { return winnerTeamId; }
    public List<MatchParticipantRecord> getParticipants() { return participants; }
    public long getFinishedAtEpochMillis() { return finishedAtEpochMillis; }
    public long getMaximumExperience() { return maximumExperience; }
}
