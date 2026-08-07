package it.legacynetwork.chickenwars.trap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rileva gli ingressi nelle basi con logica edge-triggered.
 *
 * <p>Un solo passaggio da fuori a dentro produce un evento: restare dentro non
 * ne genera altri, e uscire riarma il rilevamento. E' cosi' che una trappola
 * viene consumata al massimo una volta per ingresso.</p>
 *
 * <p>Classe pura: nessuna dipendenza da Bukkit, interamente testabile.</p>
 */
public final class BaseEntryTracker {

    private final Map<UUID, Set<String>> inside =
            new HashMap<UUID, Set<String>>();

    private static String key(String arenaId, String teamId) {
        return String.valueOf(arenaId).toLowerCase(Locale.ROOT) + '/'
                + String.valueOf(teamId).toLowerCase(Locale.ROOT);
    }

    /**
     * Registra la posizione corrente rispetto a una base.
     *
     * @param playerId giocatore osservato
     * @param arenaId  arena della base
     * @param teamId   squadra proprietaria della base
     * @param within   indica se il giocatore si trova ora dentro la regione
     * @return {@code true} soltanto sul fronte di ingresso
     */
    public boolean update(UUID playerId, String arenaId, String teamId,
                          boolean within) {
        if (playerId == null || arenaId == null || teamId == null) {
            return false;
        }
        String composite = key(arenaId, teamId);
        Set<String> current = inside.get(playerId);

        if (!within) {
            if (current != null && current.remove(composite) && current.isEmpty()) {
                inside.remove(playerId);
            }
            return false;
        }

        if (current == null) {
            current = new HashSet<String>();
            inside.put(playerId, current);
        }
        // add restituisce false se era gia' dentro: nessun nuovo fronte.
        return current.add(composite);
    }

    /**
     * Indica se il giocatore risulta attualmente dentro la base.
     */
    public boolean isInside(UUID playerId, String arenaId, String teamId) {
        Set<String> current = inside.get(playerId);
        return current != null && current.contains(key(arenaId, teamId));
    }

    /**
     * Dimentica un giocatore, al disconnect o all'uscita dalla partita.
     *
     * <p>Al rientro il rilevamento riparte da fuori, quindi un reconnect dentro
     * la base produce un nuovo ingresso legittimo.</p>
     */
    public void forget(UUID playerId) {
        if (playerId != null) {
            inside.remove(playerId);
        }
    }

    /**
     * Dimentica ogni presenza registrata in un'arena.
     *
     * @return il numero di presenze rimosse
     */
    public int clearArena(String arenaId) {
        if (arenaId == null) {
            return 0;
        }
        String prefix = arenaId.toLowerCase(Locale.ROOT) + '/';
        int removed = 0;
        for (UUID playerId : new HashSet<UUID>(inside.keySet())) {
            Set<String> current = inside.get(playerId);
            for (String composite : new HashSet<String>(current)) {
                if (composite.startsWith(prefix) && current.remove(composite)) {
                    removed++;
                }
            }
            if (current.isEmpty()) {
                inside.remove(playerId);
            }
        }
        return removed;
    }

    public void clearAll() {
        inside.clear();
    }

    public int trackedPlayers() {
        return inside.size();
    }
}
