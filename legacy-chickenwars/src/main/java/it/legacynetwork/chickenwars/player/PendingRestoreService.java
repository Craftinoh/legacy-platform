package it.legacynetwork.chickenwars.player;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ripristini rimandati per i giocatori disconnessi durante una partita.
 *
 * <p>Chi esce mentre gioca non puo' ricevere subito il proprio inventario
 * originale: la copia viene conservata qui e riapplicata al rientro, cosi' che
 * una disconnessione non comporti la perdita degli oggetti personali.</p>
 */
public final class PendingRestoreService {

    private final Map<UUID, InventorySnapshot> pending =
            new ConcurrentHashMap<UUID, InventorySnapshot>();

    /**
     * Registra una copia da riapplicare al prossimo ingresso.
     */
    public void register(UUID playerId, InventorySnapshot snapshot) {
        if (playerId != null && snapshot != null) {
            pending.put(playerId, snapshot);
        }
    }

    /**
     * Riapplica la copia salvata, se presente.
     *
     * @return {@code true} se un ripristino e' stato effettuato
     */
    public boolean restore(Player player) {
        if (player == null) {
            return false;
        }
        InventorySnapshot snapshot = pending.remove(player.getUniqueId());
        if (snapshot == null) {
            return false;
        }
        snapshot.restore(player);
        return true;
    }

    public boolean hasPending(UUID playerId) {
        return playerId != null && pending.containsKey(playerId);
    }

    public int size() {
        return pending.size();
    }

    public void clear() {
        pending.clear();
    }
}
