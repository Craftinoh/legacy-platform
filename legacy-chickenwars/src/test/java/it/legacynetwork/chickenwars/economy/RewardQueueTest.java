package it.legacynetwork.chickenwars.economy;

import it.legacynetwork.chickenwars.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardQueueTest {

    private ResourceTransferService service;
    private UUID killerId;
    private UUID victimId;

    @BeforeEach
    void setUp() {
        service = new ResourceTransferService();
        killerId = UUID.randomUUID();
        victimId = UUID.randomUUID();
    }

    private void transferAndQueue() {
        ResourceInventory.MapInventory vi = (ResourceInventory.MapInventory)
                ResourceInventory.with(resourceMap(ResourceType.IRON, 10));
        ResourceInventory.MapInventory ki = (ResourceInventory.MapInventory)
                ResourceInventory.empty();
        ki.setMaxSlots(0);
        AtomicLong seq = new AtomicLong(1);
        service.transferAdapters(victimId, killerId, vi, ki, () -> seq.getAndIncrement());
    }

    @Test
    void queueStartsWithCorrectAmount() {
        transferAndQueue();
        Map<ResourceType, Integer> q = service.getQueue(killerId);
        assertEquals(10, q.getOrDefault(ResourceType.IRON, 0));
    }

    @Test
    void flushQueueEmptyWhenNoSpace() {
        transferAndQueue();
        ResourceInventory.MapInventory ki = (ResourceInventory.MapInventory)
                ResourceInventory.empty();
        ki.setMaxSlots(0);
        Map<ResourceType, Integer> delivered = service.flushQueueAdapters(killerId, ki);
        assertTrue(delivered.isEmpty());
        assertTrue(service.hasQueue(killerId));
    }

    @Test
    void flushQueueDeliversWhenSpaceAvailable() {
        transferAndQueue();
        ResourceInventory ki = ResourceInventory.empty();
        Map<ResourceType, Integer> delivered = service.flushQueueAdapters(killerId, ki);
        assertEquals(10, delivered.getOrDefault(ResourceType.IRON, 0));
        assertFalse(service.hasQueue(killerId));
    }

    @Test
    void partialFlushLeavesResidue() {
        service.enqueue(killerId, ResourceType.IRON, 10);
        ResourceInventory.MapInventory ki = (ResourceInventory.MapInventory)
                ResourceInventory.empty();
        ki.setMaxSlots(0);
        Map<ResourceType, Integer> delivered = service.flushQueueAdapters(killerId, ki);
        assertEquals(0, delivered.getOrDefault(ResourceType.IRON, 0));
        assertEquals(10, service.getQueue(killerId).getOrDefault(ResourceType.IRON, 0));
    }

    @Test
    void secondFlushAfterSpaceFreed() {
        service.enqueue(killerId, ResourceType.IRON, 10);
        ResourceInventory.MapInventory ki = (ResourceInventory.MapInventory)
                ResourceInventory.empty();
        ki.setMaxSlots(0);
        service.flushQueueAdapters(killerId, ki);
        ki.setMaxSlots(36);
        Map<ResourceType, Integer> d2 = service.flushQueueAdapters(killerId, ki);
        assertEquals(10, d2.getOrDefault(ResourceType.IRON, 0));
        assertFalse(service.hasQueue(killerId));
    }

    @Test
    void conservationInvariantHolds() {
        service.enqueue(killerId, ResourceType.IRON, 15);
        ResourceInventory.MapInventory ki = (ResourceInventory.MapInventory)
                ResourceInventory.empty();
        ki.setMaxSlots(1);
        Map<ResourceType, Integer> d1 = service.flushQueueAdapters(killerId, ki);
        int delivered = d1.getOrDefault(ResourceType.IRON, 0);
        int remaining = service.getQueue(killerId).getOrDefault(ResourceType.IRON, 0);
        assertEquals(15, delivered + remaining);
    }

    @Test
    void noDuplicationOnRepeatFlush() {
        service.enqueue(killerId, ResourceType.IRON, 5);
        ResourceInventory ki = ResourceInventory.empty();
        service.flushQueueAdapters(killerId, ki);
        Map<ResourceType, Integer> d2 = service.flushQueueAdapters(killerId, ki);
        assertTrue(d2.isEmpty());
    }

    @Test
    void separateQueuePerPlayer() {
        UUID other = UUID.randomUUID();
        service.enqueue(killerId, ResourceType.IRON, 10);
        assertFalse(service.hasQueue(other));
    }

    private Map<ResourceType, Integer> resourceMap(ResourceType t, int a) {
        Map<ResourceType, Integer> m = new java.util.HashMap<>();
        m.put(t, a);
        return m;
    }
}
