package it.legacynetwork.chickenwars.game;

import java.util.UUID;

/** Snapshot immutabile usato dalla scoreboard finale, mai ricalcolato. */
public final class FinalPlayerSnapshot {
    private final UUID playerId;
    private final int kills;
    private final int finalKills;
    private final int chickenKills;
    private final int deaths;
    private final long resources;
    private final long experience;
    private final long coins;
    private final int level;

    public FinalPlayerSnapshot(UUID playerId, int kills, int finalKills,
                               int chickenKills, int deaths, long resources,
                               long experience, long coins, int level) {
        if (playerId == null) {
            throw new IllegalArgumentException("Giocatore mancante");
        }
        this.playerId = playerId;
        this.kills = Math.max(0, kills);
        this.finalKills = Math.max(0, finalKills);
        this.chickenKills = Math.max(0, chickenKills);
        this.deaths = Math.max(0, deaths);
        this.resources = Math.max(0L, resources);
        this.experience = Math.max(0L, experience);
        this.coins = Math.max(0L, coins);
        this.level = Math.max(0, level);
    }

    public UUID getPlayerId() { return playerId; }
    public int getKills() { return kills; }
    public int getFinalKills() { return finalKills; }
    public int getChickenKills() { return chickenKills; }
    public int getDeaths() { return deaths; }
    public long getResources() { return resources; }
    public long getExperience() { return experience; }
    public long getCoins() { return coins; }
    public int getLevel() { return level; }
}
