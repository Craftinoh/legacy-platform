package it.legacynetwork.chickenwars.economy;

import it.legacynetwork.chickenwars.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceTransferOnDeathTest {

    private ResourceTransferService service;
    private UUID victimId;
    private UUID killerId;
    private AtomicLong deathSeq;

    @BeforeEach
    void setUp() {
        service = new ResourceTransferService();
        victimId = UUID.randomUUID();
        killerId = UUID.randomUUID();
        deathSeq = new AtomicLong(1);
    }

    private ResourceTransferService.DeathSequence sequencer() {
        return () -> deathSeq.getAndIncrement();
    }

    private ResourceInventory inv(ResourceType type, int amount) {
        java.util.Map<ResourceType, Integer> m = new java.util.HashMap<>();
        m.put(type, amount);
        return ResourceInventory.with(m);
    }

    private ResourceInventory inv(int iron, int gold, int diamond, int emerald, int feather) {
        java.util.Map<ResourceType, Integer> m = new java.util.HashMap<>();
        if (iron > 0) m.put(ResourceType.IRON, iron);
        if (gold > 0) m.put(ResourceType.GOLD, gold);
        if (diamond > 0) m.put(ResourceType.DIAMOND, diamond);
        if (emerald > 0) m.put(ResourceType.EMERALD, emerald);
        if (feather > 0) m.put(ResourceType.FEATHER, feather);
        return ResourceInventory.with(m);
    }

    @Test
    void transferIron() {
        ResourceInventory vi = inv(ResourceType.IRON, 10);
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertEquals(10, r.getDelivered().getOrDefault(ResourceType.IRON, 0));
    }

    @Test
    void transferGold() {
        ResourceInventory vi = inv(ResourceType.GOLD, 5);
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertEquals(5, r.getDelivered().getOrDefault(ResourceType.GOLD, 0));
    }

    @Test
    void transferDiamond() {
        ResourceInventory vi = inv(ResourceType.DIAMOND, 3);
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertEquals(3, r.getDelivered().getOrDefault(ResourceType.DIAMOND, 0));
    }

    @Test
    void transferEmerald() {
        ResourceInventory vi = inv(ResourceType.EMERALD, 2);
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertEquals(2, r.getDelivered().getOrDefault(ResourceType.EMERALD, 0));
    }

    @Test
    void transferFeather() {
        ResourceInventory vi = inv(ResourceType.FEATHER, 1);
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertEquals(1, r.getDelivered().getOrDefault(ResourceType.FEATHER, 0));
    }

    @Test
    void transferAllFiveSimultaneously() {
        ResourceInventory vi = inv(4, 3, 2, 1, 1);
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertEquals(4, r.getDelivered().getOrDefault(ResourceType.IRON, 0));
        assertEquals(3, r.getDelivered().getOrDefault(ResourceType.GOLD, 0));
        assertEquals(2, r.getDelivered().getOrDefault(ResourceType.DIAMOND, 0));
        assertEquals(1, r.getDelivered().getOrDefault(ResourceType.EMERALD, 0));
        assertEquals(1, r.getDelivered().getOrDefault(ResourceType.FEATHER, 0));
    }

    @Test
    void victimResourcesRemoved() {
        ResourceInventory vi = inv(ResourceType.IRON, 10);
        ResourceInventory ki = ResourceInventory.empty();
        service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertEquals(0, vi.count(ResourceType.IRON));
    }

    @Test
    void noKillerReturnsEmpty() {
        ResourceInventory vi = inv(ResourceType.IRON, 10);
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(victimId, null, vi, ki, sequencer());
        assertTrue(r.isEmpty());
    }

    @Test
    void suicideReturnsEmpty() {
        ResourceInventory vi = inv(ResourceType.IRON, 10);
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(victimId, victimId, vi, ki, sequencer());
        assertTrue(r.isEmpty());
    }

    @Test
    void killerInventoryFull() {
        ResourceInventory vi = inv(ResourceType.IRON, 10);
        ResourceInventory.MapInventory ki = (ResourceInventory.MapInventory) ResourceInventory.empty();
        ki.setMaxSlots(0);
        ResourceTransfer r = service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertTrue(r.getDelivered().isEmpty());
        assertTrue(r.hasQueued());
        assertEquals(10, r.getQueued().getOrDefault(ResourceType.IRON, 0));
    }

    @Test
    void killerPartialInventory() {
        ResourceInventory vi = inv(ResourceType.IRON, 5);
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertEquals(5, r.getDelivered().getOrDefault(ResourceType.IRON, 0));
    }

    @Test
    void sameDeathSequenceProcessedOnce() {
        ResourceInventory vi = inv(ResourceType.IRON, 10);
        ResourceInventory ki = ResourceInventory.empty();
        AtomicLong seq = new AtomicLong(1);
        service.transferAdapters(victimId, killerId, vi, ki, () -> seq.get());
        ResourceTransfer r2 = service.transferAdapters(victimId, killerId, vi, ki, () -> seq.get());
        assertTrue(r2.isEmpty());
    }

    @Test
    void doubleEventSameSequenceIgnored() {
        ResourceInventory vi = inv(ResourceType.IRON, 10);
        ResourceInventory ki = ResourceInventory.empty();
        long seq = deathSeq.getAndIncrement();
        service.transferAdapters(victimId, killerId, vi, ki, () -> seq);
        ResourceTransfer r2 = service.transferAdapters(victimId, killerId, vi, ki, () -> seq);
        assertTrue(r2.isEmpty());
    }

    @Test
    void queueIsPreserved() {
        ResourceInventory vi = inv(ResourceType.IRON, 10);
        ResourceInventory.MapInventory ki = (ResourceInventory.MapInventory) ResourceInventory.empty();
        ki.setMaxSlots(0);
        service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertTrue(service.hasQueue(killerId));
    }

    @Test
    void duplicateDeathDoesNotDoubleQueue() {
        ResourceInventory vi = inv(ResourceType.IRON, 10);
        ResourceInventory.MapInventory ki = (ResourceInventory.MapInventory) ResourceInventory.empty();
        ki.setMaxSlots(0);
        long seq = deathSeq.getAndIncrement();
        service.transferAdapters(victimId, killerId, vi, ki, () -> seq);
        service.transferAdapters(victimId, killerId, vi, ki, () -> seq);
        Map<ResourceType, Integer> queue = service.getQueue(killerId);
        assertEquals(10, queue.getOrDefault(ResourceType.IRON, 0).intValue());
    }

    @Test
    void noXpFromTransfer() {
        ResourceInventory vi = inv(ResourceType.IRON, 10);
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertFalse(r.isEmpty());
    }

    @Test
    void nullVictimReturnsEmpty() {
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(null, killerId, null, ki, sequencer());
        assertTrue(r.isEmpty());
    }

    @Test
    void emptyVictimInventoryReturnsEmpty() {
        ResourceInventory vi = ResourceInventory.empty();
        ResourceInventory ki = ResourceInventory.empty();
        ResourceTransfer r = service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertTrue(r.isEmpty());
    }

    @Test
    void resourcesNotDuplicated() {
        ResourceInventory vi = inv(ResourceType.IRON, 5);
        ResourceInventory ki = ResourceInventory.empty();
        service.transferAdapters(victimId, killerId, vi, ki, sequencer());
        assertFalse(service.hasQueue(killerId));
    }
}
