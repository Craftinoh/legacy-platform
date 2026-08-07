package it.legacynetwork.chickenwars.trap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Contesto gia' risolto di un ingresso in una base nemica.
 *
 * <p>Contiene soltanto identificatori e booleani: la decisione di attivare una
 * trappola resta quindi verificabile senza un server in esecuzione.</p>
 */
public final class TrapTriggerRequest {

    private final String arenaId;
    private final String ownerTeamId;
    private final String intruderTeamId;
    private final UUID intruderId;
    private final boolean gameRunning;
    private final boolean intruderEligible;
    private final List<UUID> defenders;

    private TrapTriggerRequest(Builder builder) {
        this.arenaId = builder.arenaId;
        this.ownerTeamId = builder.ownerTeamId;
        this.intruderTeamId = builder.intruderTeamId;
        this.intruderId = builder.intruderId;
        this.gameRunning = builder.gameRunning;
        this.intruderEligible = builder.intruderEligible;
        this.defenders = Collections.unmodifiableList(
                new ArrayList<UUID>(builder.defenders));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getArenaId() {
        return arenaId;
    }

    public String getOwnerTeamId() {
        return ownerTeamId;
    }

    public String getIntruderTeamId() {
        return intruderTeamId;
    }

    public UUID getIntruderId() {
        return intruderId;
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    /**
     * Indica se l'intruso partecipa attivamente: spettatori, eliminati e
     * giocatori estranei alla partita vanno dichiarati non eleggibili.
     */
    public boolean isIntruderEligible() {
        return intruderEligible;
    }

    /**
     * Difensori eleggibili presenti nella base, unici destinatari degli effetti
     * di Contro-Offensiva.
     */
    public List<UUID> getDefenders() {
        return defenders;
    }

    /**
     * Indica se chi e' entrato appartiene alla squadra proprietaria.
     */
    public boolean isFriendly() {
        return ownerTeamId != null && ownerTeamId.equals(intruderTeamId);
    }

    /**
     * Costruttore incrementale del contesto.
     */
    public static final class Builder {

        private String arenaId;
        private String ownerTeamId;
        private String intruderTeamId;
        private UUID intruderId;
        private boolean gameRunning = true;
        private boolean intruderEligible = true;
        private final List<UUID> defenders = new ArrayList<UUID>();

        public Builder base(String arenaId, String ownerTeamId) {
            this.arenaId = arenaId;
            this.ownerTeamId = ownerTeamId;
            return this;
        }

        public Builder intruder(UUID intruderId, String intruderTeamId) {
            this.intruderId = intruderId;
            this.intruderTeamId = intruderTeamId;
            return this;
        }

        public Builder gameRunning(boolean gameRunning) {
            this.gameRunning = gameRunning;
            return this;
        }

        public Builder intruderEligible(boolean intruderEligible) {
            this.intruderEligible = intruderEligible;
            return this;
        }

        public Builder defenders(Collection<UUID> members) {
            this.defenders.clear();
            if (members != null) {
                for (UUID member : members) {
                    if (member != null && !defenders.contains(member)) {
                        defenders.add(member);
                    }
                }
            }
            return this;
        }

        public TrapTriggerRequest build() {
            return new TrapTriggerRequest(this);
        }
    }
}
