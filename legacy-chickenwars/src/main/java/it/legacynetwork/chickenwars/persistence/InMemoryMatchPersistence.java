package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.mode.ModeProfile;
import it.legacynetwork.chickenwars.mode.ModeProfileRegistry;
import it.legacynetwork.chickenwars.statistics.ModeStatistics;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Implementazione atomica in-memory con copy-on-write per rollback reale. */
public final class InMemoryMatchPersistence implements MatchPersistence {
    private final Object lock = new Object();
    private Map<UUID, PlayerProgressRecord> progress =
            new HashMap<UUID, PlayerProgressRecord>();
    private Map<String, ModeStatistics> statistics =
            new HashMap<String, ModeStatistics>();
    private Map<String, MatchResultRecord> matches =
            new HashMap<String, MatchResultRecord>();
    private volatile boolean failBeforeCommit;

    @Override
    public CompletionStage<MatchFinalizationResult> finalizeMatch(
            MatchFinalizationRequest request) {
        synchronized (lock) {
            if (matches.containsKey(request.getMatchId())) {
                return completed(false);
            }
            Map<UUID, PlayerProgressRecord> nextProgress =
                    new HashMap<UUID, PlayerProgressRecord>(progress);
            Map<String, ModeStatistics> nextStats =
                    new HashMap<String, ModeStatistics>(statistics);
            Map<String, MatchResultRecord> nextMatches =
                    new HashMap<String, MatchResultRecord>(matches);
            ModeProfile profile = ModeProfileRegistry.defaults().get(request.getMode());
            for (MatchParticipantRecord participant : request.getParticipants()) {
                if (profile.isRewardsEnabled()) {
                    PlayerProgressRecord old = nextProgress.get(participant.getPlayerId());
                    long xp = add(old == null ? 0L : old.getTotalExperience(),
                            participant.getExperience());
                    long coins = add(old == null ? 0L : old.getCoins(),
                            participant.getCoins());
                    nextProgress.put(participant.getPlayerId(), new PlayerProgressRecord(
                            participant.getPlayerId(), xp, coins,
                            request.getFinishedAtEpochMillis()));
                }
                if (profile.isTracked()) {
                    String key = participant.getPlayerId() + ":" + request.getMode().name();
                    ModeStatistics old = nextStats.get(key);
                    nextStats.put(key, merge(old, participant, request));
                }
            }
            nextMatches.put(request.getMatchId(), new MatchResultRecord(
                    request.getMatchId(), request.getMode(), request.getWinnerTeamId(),
                    request.getFinishedAtEpochMillis()));
            if (failBeforeCommit) {
                return failed(new IllegalStateException("Errore transazione simulato"));
            }
            progress = nextProgress; statistics = nextStats; matches = nextMatches;
            return completed(true);
        }
    }

    private ModeStatistics merge(ModeStatistics old, MatchParticipantRecord value,
                                 MatchFinalizationRequest request) {
        return new ModeStatistics(value.getPlayerId(), request.getMode(),
                add(old == null ? 0L : old.getGames(), 1L),
                add(old == null ? 0L : old.getWins(), value.isWinner() ? 1L : 0L),
                add(old == null ? 0L : old.getKills(), value.getKills()),
                add(old == null ? 0L : old.getDeaths(), value.getDeaths()),
                add(old == null ? 0L : old.getFinalKills(), value.getFinalKills()),
                old == null ? 0L : old.getChickenKills(),
                add(old == null ? 0L : old.getEliminations(), value.getEliminations()),
                add(old == null ? 0L : old.getPlaySeconds(), value.getPlaySeconds()),
                add(old == null ? 0L : old.getResourcesCollected(), value.getResources()));
    }

    private long add(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
    private CompletionStage<MatchFinalizationResult> completed(boolean applied) {
        return CompletableFuture.completedFuture(new MatchFinalizationResult(applied));
    }
    private CompletionStage<MatchFinalizationResult> failed(Throwable throwable) {
        CompletableFuture<MatchFinalizationResult> future =
                new CompletableFuture<MatchFinalizationResult>();
        future.completeExceptionally(throwable); return future;
    }
    public void setFailBeforeCommit(boolean fail) { this.failBeforeCommit = fail; }
    public PlayerProgressRecord getProgress(UUID playerId) { return progress.get(playerId); }
    public ModeStatistics getStatistics(UUID playerId, String mode) {
        return statistics.get(playerId + ":" + mode);
    }
    public boolean hasMatch(String matchId) { return matches.containsKey(matchId); }
    @Override public CompletionStage<Void> close() {
        synchronized (lock) { progress.clear(); statistics.clear(); matches.clear(); }
        return CompletableFuture.completedFuture(null);
    }
}
