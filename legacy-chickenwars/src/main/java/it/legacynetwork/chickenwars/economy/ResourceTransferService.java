package it.legacynetwork.chickenwars.economy;

import it.legacynetwork.chickenwars.model.ResourceType;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Trasferimento delle valute di partita dalla vittima al suo uccisore.
 *
 * <p>La rimozione dalla vittima e' una singola operazione logica: tutte e
 * cinque le valute vengono tolte insieme. Nulla viene lasciato a terra, ne'
 * quando esiste un uccisore ne' quando non esiste.</p>
 *
 * <p>Le risorse entrano direttamente nell'inventario dell'uccisore: non
 * attraversano un {@code PlayerPickupItemEvent}, quindi non producono
 * l'esperienza prevista per la raccolta naturale.</p>
 */
public final class ResourceTransferService {

    private final Map<UUID, Map<ResourceType, Integer>> rewardQueue =
            new HashMap<UUID, Map<ResourceType, Integer>>();
    private final Map<UUID, Long> processedDeaths = new HashMap<UUID, Long>();

    /**
     * Consegna quanto possibile della coda premio del giocatore.
     *
     * <p>Va richiamata quando lo spazio puo' essersi liberato: al respawn e
     * dopo ogni consegna riuscita.</p>
     *
     * @return le quantita' effettivamente consegnate ora
     */
    public Map<ResourceType, Integer> flushQueue(Player player) {
        if (player == null || !player.isOnline()) return new EnumMap<>(ResourceType.class);
        return flushQueueAdapters(player.getUniqueId(), ResourceWallet.adapterFor(player));
    }

    public Map<ResourceType, Integer> flushQueueAdapters(UUID playerId, ResourceInventory inv) {
        Map<ResourceType, Integer> delivered = new EnumMap<>(ResourceType.class);
        if (playerId == null || inv == null) return delivered;
        Map<ResourceType, Integer> pending = rewardQueue.get(playerId);
        if (pending == null || pending.isEmpty()) return delivered;
        for (Map.Entry<ResourceType, Integer> e : new java.util.ArrayList<>(pending.entrySet())) {
            int amount = e.getValue();
            if (amount <= 0) { pending.remove(e.getKey()); continue; }
            int leftover = inv.deposit(e.getKey(), amount);
            int given = amount - leftover;
            if (given > 0) delivered.put(e.getKey(), given);
            if (leftover > 0) pending.put(e.getKey(), leftover);
            else pending.remove(e.getKey());
        }
        if (pending.isEmpty()) rewardQueue.remove(playerId);
        return delivered;
    }

    /**
     * Indica se il giocatore ha risorse in attesa di consegna.
     */
    public boolean hasQueue(UUID playerId) {
        Map<ResourceType, Integer> pending = rewardQueue.get(playerId);
        return pending != null && !pending.isEmpty();
    }

    /**
     * Quantita' attualmente in coda per il giocatore indicato.
     */
    public Map<ResourceType, Integer> getQueue(UUID playerId) {
        Map<ResourceType, Integer> pending = rewardQueue.get(playerId);
        if (pending == null) {
            return new EnumMap<ResourceType, Integer>(ResourceType.class);
        }
        return new EnumMap<ResourceType, Integer>(pending);
    }

    void enqueue(UUID playerId, ResourceType type, int amount) {
        Map<ResourceType, Integer> pending = rewardQueue.get(playerId);
        if (pending == null) {
            pending = new EnumMap<ResourceType, Integer>(ResourceType.class);
            rewardQueue.put(playerId, pending);
        }
        Integer existing = pending.get(type);
        pending.put(type, Integer.valueOf(
                (existing == null ? 0 : existing.intValue()) + amount));
    }

    // Test entry-point: trasferimento tra adapter, senza dipendere da Bukkit.
    public ResourceTransfer transferAdapters(UUID victimId, UUID killerId,
                                               ResourceInventory victimInv,
                                               ResourceInventory killerInv,
                                               DeathSequence sequencer) {
        if (victimId == null || victimInv == null) return ResourceTransfer.empty();
        long sequence = sequencer.nextSequence();
        Long processed = processedDeaths.get(victimId);
        if (processed != null && processed >= sequence) return ResourceTransfer.empty();
        processedDeaths.put(victimId, sequence);

        Map<ResourceType, Integer> removed = victimInv.withdrawAll();
        if (killerId == null || killerId.equals(victimId) || killerInv == null) {
            return ResourceTransfer.empty();
        }
        if (removed.isEmpty()) return ResourceTransfer.empty();

        Map<ResourceType, Integer> delivered = new EnumMap<>(ResourceType.class);
        Map<ResourceType, Integer> queued = new EnumMap<>(ResourceType.class);
        for (Map.Entry<ResourceType, Integer> e : removed.entrySet()) {
            int amount = e.getValue();
            if (amount <= 0) continue;
            int leftover = killerInv.deposit(e.getKey(), amount);
            int given = amount - leftover;
            if (given > 0) delivered.put(e.getKey(), given);
            if (leftover > 0) {
                queued.put(e.getKey(), leftover);
                enqueue(killerId, e.getKey(), leftover);
            }
        }
        return new ResourceTransfer(delivered, queued);
    }

    public interface DeathSequence {
        long nextSequence();
    }
    public void clear(UUID playerId) {
        rewardQueue.remove(playerId);
        processedDeaths.remove(playerId);
    }

    /**
     * Azzera l'intero stato, a fine partita o allo spegnimento.
     */
    public void clearAll() {
        rewardQueue.clear();
        processedDeaths.clear();
    }
}
