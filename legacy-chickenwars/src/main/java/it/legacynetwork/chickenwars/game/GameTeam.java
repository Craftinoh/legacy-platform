package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.arena.TeamDefinition;
import it.legacynetwork.chickenwars.chicken.RoyalChicken;
import it.legacynetwork.chickenwars.model.TeamColor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stato runtime di una squadra durante la partita.
 *
 * <p>Una squadra e' eliminata quando la sua Gallina Reale e' morta e nessun
 * membro resta in vita.</p>
 */
public final class GameTeam {

    private final TeamDefinition definition;
    private final Set<UUID> members = new LinkedHashSet<UUID>();
    private final Set<UUID> aliveMembers = new LinkedHashSet<UUID>();

    private RoyalChicken chicken;
    private boolean eliminated;

    public GameTeam(TeamDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Definizione squadra mancante");
        }
        this.definition = definition;
    }

    public boolean addMember(UUID playerId) {
        if (playerId == null || members.size() >= definition.getMaxPlayers()) {
            return false;
        }
        aliveMembers.add(playerId);
        return members.add(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
        aliveMembers.remove(playerId);
    }

    /**
     * Segna un membro come definitivamente eliminato.
     *
     * @return {@code true} se il membro era ancora in vita
     */
    public boolean eliminateMember(UUID playerId) {
        return aliveMembers.remove(playerId);
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean isAlive(UUID playerId) {
        return aliveMembers.contains(playerId);
    }

    public boolean isFull() {
        return members.size() >= definition.getMaxPlayers();
    }

    public int getFreeSlots() {
        return Math.max(0, definition.getMaxPlayers() - members.size());
    }

    /**
     * Indica se la squadra puo' ancora fare respawn.
     */
    public boolean hasChicken() {
        return chicken != null && chicken.isAlive();
    }

    /**
     * Indica se la squadra e' fuori dalla partita.
     *
     * <p>Vero quando e' stata eliminata esplicitamente oppure quando ha perso la
     * gallina e non ha piu' membri vivi.</p>
     */
    public boolean isOut() {
        return eliminated || (!hasChicken() && aliveMembers.isEmpty());
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public Set<UUID> getAliveMembers() {
        return Collections.unmodifiableSet(aliveMembers);
    }

    public int getAliveCount() {
        return aliveMembers.size();
    }

    public int getMemberCount() {
        return members.size();
    }

    public TeamDefinition getDefinition() {
        return definition;
    }

    public String getId() {
        return definition.getId();
    }

    public TeamColor getColor() {
        return definition.getColor();
    }

    public String getColoredName() {
        return definition.getColoredName();
    }

    public RoyalChicken getChicken() {
        return chicken;
    }

    public void setChicken(RoyalChicken chicken) {
        this.chicken = chicken;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
        if (eliminated) {
            aliveMembers.clear();
        }
    }
}
