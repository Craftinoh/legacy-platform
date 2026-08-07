package it.legacynetwork.chickenwars.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Bilanciamento delle squadre.
 *
 * <p>La regola e' semplice e prevedibile: chi non ha scelto viene inserito nella
 * squadra meno numerosa tra quelle ancora libere, mantenendo l'ordine di
 * definizione dell'arena in caso di parita'.</p>
 *
 * <p>Classe priva di dipendenze Bukkit e quindi verificabile con test unitari.</p>
 */
public final class TeamAssigner {

    private TeamAssigner() {
    }

    /**
     * Individua la squadra piu' adatta a ospitare un nuovo giocatore.
     *
     * @param teams squadre disponibili
     * @return la squadra scelta, oppure {@code null} se sono tutte piene
     */
    public static GameTeam findBestTeam(Collection<GameTeam> teams) {
        GameTeam best = null;
        for (GameTeam team : teams) {
            if (team == null || team.isFull()) {
                continue;
            }
            if (best == null || team.getMemberCount() < best.getMemberCount()) {
                best = team;
            }
        }
        return best;
    }

    /**
     * Assegna una squadra a ogni giocatore ancora privo di assegnazione.
     *
     * <p>I giocatori che hanno gia' scelto una squadra non vengono spostati.</p>
     *
     * @param teams     squadre disponibili
     * @param unassigned giocatori da distribuire
     * @return i giocatori rimasti senza squadra perche' tutte piene
     */
    public static List<UUID> distribute(Collection<GameTeam> teams,
                                        Collection<UUID> unassigned) {
        List<UUID> leftovers = new ArrayList<UUID>();
        if (teams == null || unassigned == null) {
            return leftovers;
        }
        for (UUID playerId : unassigned) {
            if (playerId == null) {
                continue;
            }
            GameTeam target = findBestTeam(teams);
            if (target == null || !target.addMember(playerId)) {
                leftovers.add(playerId);
                continue;
            }
        }
        return leftovers;
    }

    /**
     * Verifica se la distribuzione corrente consente una partita sensata.
     *
     * <p>Serve almeno una squadra con giocatori oltre alla prima, altrimenti la
     * partita finirebbe immediatamente.</p>
     *
     * @param teams squadre da controllare
     * @return {@code true} se almeno due squadre hanno membri
     */
    public static boolean hasEnoughOccupiedTeams(Collection<GameTeam> teams) {
        int occupied = 0;
        for (GameTeam team : teams) {
            if (team != null && team.getMemberCount() > 0) {
                occupied++;
            }
            if (occupied >= 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rimuove dalle squadre le assegnazioni dei giocatori indicati.
     */
    public static void release(Collection<GameTeam> teams, UUID playerId) {
        if (teams == null || playerId == null) {
            return;
        }
        for (GameTeam team : teams) {
            if (team != null) {
                team.removeMember(playerId);
            }
        }
    }
}
