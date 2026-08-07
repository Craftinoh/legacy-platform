package it.legacynetwork.chickenwars.chicken;

import java.util.UUID;

/**
 * Sconfitta di una Gallina Reale, descritta senza dipendere da Bukkit.
 *
 * <p>E' il dato consegnato al punto di innesto del Royal Collapse: contiene
 * tutto cio' che serve a decidere le conseguenze, ma non ne applica
 * nessuna.</p>
 */
public final class RoyalDefeat {

    private final String arenaId;
    private final String teamId;
    private final UUID entityId;
    private final UUID attackerId;
    private final long millis;

    public RoyalDefeat(String arenaId, String teamId, UUID entityId,
                       UUID attackerId, long millis) {
        if (arenaId == null || teamId == null) {
            throw new IllegalArgumentException("Sconfitta senza arena o squadra");
        }
        this.arenaId = arenaId;
        this.teamId = teamId;
        this.entityId = entityId;
        this.attackerId = attackerId;
        this.millis = millis;
    }

    public String getArenaId() {
        return arenaId;
    }

    public String getTeamId() {
        return teamId;
    }

    /**
     * @return l'entita' sconfitta, oppure {@code null} se gia' rimossa
     */
    public UUID getEntityId() {
        return entityId;
    }

    /**
     * @return l'autore del colpo fatale, oppure {@code null}
     */
    public UUID getAttackerId() {
        return attackerId;
    }

    public long getMillis() {
        return millis;
    }

    @Override
    public String toString() {
        return "RoyalDefeat{" + arenaId + '/' + teamId
                + ", entita'=" + entityId + ", aggressore=" + attackerId + '}';
    }
}
