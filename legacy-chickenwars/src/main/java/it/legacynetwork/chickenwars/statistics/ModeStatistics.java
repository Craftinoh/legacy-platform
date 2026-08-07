package it.legacynetwork.chickenwars.statistics;

import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.mode.ModeProfileRegistry;

import java.util.UUID;

/**
 * Statistiche permanenti di una singola modalita' tracciata.
 */
public final class ModeStatistics {

    private final UUID playerId;
    private final MatchMode mode;
    private final long games;
    private final long wins;
    private final long kills;
    private final long deaths;
    private final long finalKills;
    private final long chickenKills;
    private final long playSeconds;
    private final long resourcesCollected;

    public ModeStatistics(UUID playerId, MatchMode mode, long games, long wins,
                          long kills, long deaths, long finalKills,
                          long chickenKills, long playSeconds,
                          long resourcesCollected) {
        if (playerId == null) {
            throw new IllegalArgumentException("UUID giocatore mancante");
        }
        if (mode == null || !ModeProfileRegistry.defaults().get(mode).isTracked()) {
            throw new IllegalArgumentException(
                    "La modalita' non produce statistiche permanenti: " + mode);
        }
        this.playerId = playerId;
        this.mode = mode;
        this.games = nonNegative(games);
        this.wins = Math.min(this.games, nonNegative(wins));
        this.kills = nonNegative(kills);
        this.deaths = nonNegative(deaths);
        this.finalKills = nonNegative(finalKills);
        this.chickenKills = nonNegative(chickenKills);
        this.playSeconds = nonNegative(playSeconds);
        this.resourcesCollected = nonNegative(resourcesCollected);
    }

    private long nonNegative(long value) {
        return Math.max(0L, value);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public MatchMode getMode() {
        return mode;
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
