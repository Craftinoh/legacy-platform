package it.legacynetwork.chickenwars.statistics;

import java.util.Collection;

/**
 * Somma globale delle statistiche Solo, Doubles e Trio.
 */
public final class GlobalStatistics {

    private final long games;
    private final long wins;
    private final long kills;
    private final long deaths;
    private final long finalKills;
    private final long chickenKills;
    private final long playSeconds;
    private final long resourcesCollected;

    private GlobalStatistics(long games, long wins, long kills, long deaths,
                             long finalKills, long chickenKills,
                             long playSeconds, long resourcesCollected) {
        this.games = games;
        this.wins = wins;
        this.kills = kills;
        this.deaths = deaths;
        this.finalKills = finalKills;
        this.chickenKills = chickenKills;
        this.playSeconds = playSeconds;
        this.resourcesCollected = resourcesCollected;
    }

    public static GlobalStatistics sum(Collection<ModeStatistics> entries) {
        long games = 0L;
        long wins = 0L;
        long kills = 0L;
        long deaths = 0L;
        long finalKills = 0L;
        long chickenKills = 0L;
        long playSeconds = 0L;
        long resourcesCollected = 0L;
        if (entries != null) {
            for (ModeStatistics entry : entries) {
                if (entry == null) {
                    continue;
                }
                games = safeAdd(games, entry.getGames());
                wins = safeAdd(wins, entry.getWins());
                kills = safeAdd(kills, entry.getKills());
                deaths = safeAdd(deaths, entry.getDeaths());
                finalKills = safeAdd(finalKills, entry.getFinalKills());
                chickenKills = safeAdd(chickenKills, entry.getChickenKills());
                playSeconds = safeAdd(playSeconds, entry.getPlaySeconds());
                resourcesCollected = safeAdd(resourcesCollected,
                        entry.getResourcesCollected());
            }
        }
        return new GlobalStatistics(games, wins, kills, deaths, finalKills,
                chickenKills, playSeconds, resourcesCollected);
    }

    private static long safeAdd(long current, long value) {
        return Long.MAX_VALUE - current < value
                ? Long.MAX_VALUE : current + value;
    }

    public double getWinRate() {
        return games == 0L ? 0.0D : (double) wins / (double) games;
    }

    public double getKillDeathRatio() {
        return deaths == 0L ? kills : (double) kills / (double) deaths;
    }

    public long getGames() {
        return games;
    }

    public long getWins() {
        return wins;
    }

    public long getLosses() {
        return Math.max(0L, games - wins);
    }

    public long getKills() {
        return kills;
    }

    public long getDeaths() {
        return deaths;
    }

    public long getFinalKills() {
        return finalKills;
    }

    public long getChickenKills() {
        return chickenKills;
    }

    public long getPlaySeconds() {
        return playSeconds;
    }

    public long getResourcesCollected() {
        return resourcesCollected;
    }
}
