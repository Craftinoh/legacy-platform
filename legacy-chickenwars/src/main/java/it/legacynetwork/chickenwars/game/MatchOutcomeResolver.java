package it.legacynetwork.chickenwars.game;

import java.util.Collection;

/** Unica decisione deterministica per vittoria, pareggio e timeout. */
public final class MatchOutcomeResolver {
    public static final class Result {
        private final boolean terminal;
        private final GameTeam winner;
        private Result(boolean terminal, GameTeam winner) {
            this.terminal = terminal;
            this.winner = winner;
        }
        public boolean isTerminal() { return terminal; }
        public GameTeam getWinner() { return winner; }
        public boolean isDraw() { return terminal && winner == null; }
    }

    public Result afterElimination(Collection<GameTeam> teams) {
        GameTeam survivor = null;
        int remaining = 0;
        if (teams != null) {
            for (GameTeam team : teams) {
                if (team == null || team.getMemberCount() == 0 || team.isOut()) {
                    continue;
                }
                remaining++;
                survivor = team;
            }
        }
        return new Result(remaining <= 1, remaining == 1 ? survivor : null);
    }

    public Result atTimeout(Collection<GameTeam> teams,
                            MatchTimeoutPolicy policy) {
        if (policy == null || policy == MatchTimeoutPolicy.DRAW) {
            return new Result(true, null);
        }
        GameTeam leader = null;
        boolean tied = false;
        if (teams != null) {
            for (GameTeam team : teams) {
                if (team == null || team.getMemberCount() == 0 || team.isOut()) {
                    continue;
                }
                if (leader == null
                        || team.getAliveCount() > leader.getAliveCount()) {
                    leader = team;
                    tied = false;
                } else if (team.getAliveCount() == leader.getAliveCount()) {
                    tied = true;
                }
            }
        }
        return new Result(true, tied ? null : leader);
    }
}
