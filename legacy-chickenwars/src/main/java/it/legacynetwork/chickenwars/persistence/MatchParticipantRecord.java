package it.legacynetwork.chickenwars.persistence;

import java.util.UUID;

/** Snapshot autorevole di un partecipante usato dalla finalizzazione. */
public final class MatchParticipantRecord {
    private final UUID playerId;
    private final String teamId;
    private final boolean winner;
    private final long experience;
    private final long coins;
    private final long kills;
    private final long finalKills;
    private final long deaths;
    private final long eliminations;
    private final long resources;
    private final long playSeconds;

    public MatchParticipantRecord(UUID playerId, String teamId, boolean winner,
            long experience, long coins, long kills, long finalKills,
            long deaths, long eliminations, long resources, long playSeconds) {
        if (playerId == null) {
            throw new IllegalArgumentException("Partecipante senza UUID");
        }
        this.playerId = playerId; this.teamId = teamId; this.winner = winner;
        this.experience = nonNegative(experience); this.coins = nonNegative(coins);
        this.kills = nonNegative(kills); this.finalKills = nonNegative(finalKills);
        this.deaths = nonNegative(deaths); this.eliminations = nonNegative(eliminations);
        this.resources = nonNegative(resources); this.playSeconds = nonNegative(playSeconds);
    }
    private long nonNegative(long value) { return Math.max(0L, value); }
    public UUID getPlayerId() { return playerId; }
    public String getTeamId() { return teamId; }
    public boolean isWinner() { return winner; }
    public long getExperience() { return experience; }
    public long getCoins() { return coins; }
    public long getKills() { return kills; }
    public long getFinalKills() { return finalKills; }
    public long getDeaths() { return deaths; }
    public long getEliminations() { return eliminations; }
    public long getResources() { return resources; }
    public long getPlaySeconds() { return playSeconds; }
}
