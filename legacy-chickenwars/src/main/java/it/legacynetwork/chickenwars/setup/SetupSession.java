package it.legacynetwork.chickenwars.setup;

import it.legacynetwork.chickenwars.player.InventorySnapshot;

import java.util.UUID;

/**
 * Sessione di editor aperta da un amministratore su una singola arena.
 */
public final class SetupSession {

    private final UUID playerId;
    private final String arenaId;
    private final InventorySnapshot snapshot;

    private String selectedTeamId;

    public SetupSession(UUID playerId, String arenaId,
                        InventorySnapshot snapshot) {
        if (playerId == null || arenaId == null) {
            throw new IllegalArgumentException("Sessione editor non valida");
        }
        this.playerId = playerId;
        this.arenaId = arenaId;
        this.snapshot = snapshot;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getArenaId() {
        return arenaId;
    }

    /**
     * @return la copia dello stato precedente, eventualmente nulla
     */
    public InventorySnapshot getSnapshot() {
        return snapshot;
    }

    /**
     * @return la squadra su cui agiscono gli strumenti, oppure {@code null}
     */
    public String getSelectedTeamId() {
        return selectedTeamId;
    }

    public void setSelectedTeamId(String selectedTeamId) {
        this.selectedTeamId = selectedTeamId;
    }

    public boolean hasSelectedTeam() {
        return selectedTeamId != null;
    }
}
