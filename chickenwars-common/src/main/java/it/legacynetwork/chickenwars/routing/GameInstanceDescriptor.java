package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.mode.MatchMode;

/** Snapshot pubblicato da una singola istanza ChickenWars. */
public final class GameInstanceDescriptor {
    private final String instanceId;
    private final String serverName;
    private final MatchMode mode;
    private final String arenaId;
    private final InstanceStatus status;
    private final int players;
    private final int capacity;
    private final long heartbeatAt;
    private final boolean acceptingJoins;

    public GameInstanceDescriptor(String instanceId, String serverName,
            MatchMode mode, String arenaId, InstanceStatus status, int players,
            int capacity, long heartbeatAt, boolean acceptingJoins) {
        if (blank(instanceId) || blank(serverName) || mode == null
                || blank(arenaId) || status == null || players < 0
                || capacity <= 0 || players > capacity) {
            throw new IllegalArgumentException("Descrittore istanza non valido");
        }
        this.instanceId = instanceId; this.serverName = serverName;
        this.mode = mode; this.arenaId = arenaId; this.status = status;
        this.players = players; this.capacity = capacity;
        this.heartbeatAt = heartbeatAt; this.acceptingJoins = acceptingJoins;
    }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    public boolean isRoutable(long now, long timeoutMillis, int reservedSeats) {
        return acceptingJoins && (status == InstanceStatus.WAITING
                || status == InstanceStatus.STARTING)
                && heartbeatAt >= now - timeoutMillis
                && players + reservedSeats < capacity;
    }
    public String getInstanceId() { return instanceId; }
    public String getServerName() { return serverName; }
    public MatchMode getMode() { return mode; }
    public String getArenaId() { return arenaId; }
    public InstanceStatus getStatus() { return status; }
    public int getPlayers() { return players; }
    public int getCapacity() { return capacity; }
    public long getHeartbeatAt() { return heartbeatAt; }
    public boolean isAcceptingJoins() { return acceptingJoins; }
}
