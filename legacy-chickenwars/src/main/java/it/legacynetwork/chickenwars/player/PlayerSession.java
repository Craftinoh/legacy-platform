package it.legacynetwork.chickenwars.player;

import java.util.UUID;

/**
 * Stato di un giocatore all'interno di una partita.
 *
 * <p>Viene creato all'ingresso nell'arena e distrutto all'uscita. Le statistiche
 * qui contenute alimentano scoreboard e riepilogo finale.</p>
 */
public final class PlayerSession {

    private final UUID playerId;
    private final String playerName;
    private final String arenaId;
    private final InventorySnapshot snapshot;

    private PlayerState state = PlayerState.LOBBY;
    private String teamId;
    private int respawnSecondsLeft;
    private long invulnerableUntil;

    private UUID lastDamager;
    private long lastDamageMillis;

    private int kills;
    private int finalKills;
    private int deaths;
    private int chickensKilled;
    private int resourcesCollected;

    public PlayerSession(UUID playerId, String playerName, String arenaId,
                         InventorySnapshot snapshot) {
        if (playerId == null) {
            throw new IllegalArgumentException("UUID giocatore mancante");
        }
        this.playerId = playerId;
        this.playerName = playerName == null ? playerId.toString() : playerName;
        this.arenaId = arenaId;
        this.snapshot = snapshot;
    }

    /**
     * Registra chi ha colpito il giocatore, per l'attribuzione delle uccisioni.
     */
    public void recordDamager(UUID damager) {
        this.lastDamager = damager;
        this.lastDamageMillis = System.currentTimeMillis();
    }

    /**
     * Restituisce l'ultimo aggressore se il colpo e' ancora valido.
     *
     * @param creditSeconds finestra di validita' in secondi
     * @return l'aggressore, oppure {@code null} se il colpo e' scaduto
     */
    public UUID getValidDamager(int creditSeconds) {
        if (lastDamager == null) {
            return null;
        }
        long elapsed = System.currentTimeMillis() - lastDamageMillis;
        return elapsed <= creditSeconds * 1000L ? lastDamager : null;
    }

    public void clearDamager() {
        this.lastDamager = null;
        this.lastDamageMillis = 0L;
    }

    /**
     * Applica una protezione temporanea dal danno.
     *
     * @param seconds durata in secondi; valori non positivi la rimuovono
     */
    public void grantInvulnerability(int seconds) {
        this.invulnerableUntil = seconds <= 0
                ? 0L : System.currentTimeMillis() + seconds * 1000L;
    }

    public boolean isInvulnerable() {
        return invulnerableUntil > System.currentTimeMillis();
    }

    public void clearInvulnerability() {
        this.invulnerableUntil = 0L;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getArenaId() {
        return arenaId;
    }

    public InventorySnapshot getSnapshot() {
        return snapshot;
    }

    public PlayerState getState() {
        return state;
    }

    public void setState(PlayerState state) {
        this.state = state == null ? PlayerState.SPECTATOR : state;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public boolean hasTeam() {
        return teamId != null;
    }

    public int getRespawnSecondsLeft() {
        return respawnSecondsLeft;
    }

    public void setRespawnSecondsLeft(int respawnSecondsLeft) {
        this.respawnSecondsLeft = Math.max(0, respawnSecondsLeft);
    }

    /**
     * Decrementa di un secondo il conto alla rovescia del respawn.
     *
     * @return {@code true} quando il conteggio ha raggiunto lo zero
     */
    public boolean tickRespawn() {
        if (respawnSecondsLeft > 0) {
            respawnSecondsLeft--;
        }
        return respawnSecondsLeft <= 0;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        kills++;
    }

    public int getFinalKills() {
        return finalKills;
    }

    public void addFinalKill() {
        finalKills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeath() {
        deaths++;
    }

    public int getChickensKilled() {
        return chickensKilled;
    }

    public void addChickenKill() {
        chickensKilled++;
    }

    public int getResourcesCollected() {
        return resourcesCollected;
    }

    public void addResources(int amount) {
        if (amount > 0) {
            resourcesCollected += amount;
        }
    }
}
