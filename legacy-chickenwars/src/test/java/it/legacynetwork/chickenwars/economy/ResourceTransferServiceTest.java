package it.legacynetwork.chickenwars.economy;

import it.legacynetwork.chickenwars.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceTransferServiceTest {

    private ResourceTransferService service;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        service = new ResourceTransferService();
        playerId = UUID.randomUUID();
    }

    @Test
    void queueStartsEmpty() {
        assertFalse(service.hasQueue(playerId));
    }

    @Test
    void getQueueReturnsEmptyForNewPlayer() {
        Map<ResourceType, Integer> queue = service.getQueue(playerId);
        assertTrue(queue.isEmpty());
    }

    @Test
    void clearRemovesQueue() {
        service.clear(playerId);
        assertFalse(service.hasQueue(playerId));
    }

    @Test
    void clearAllRemovesEverything() {
        service.clearAll();
        assertFalse(service.hasQueue(playerId));
    }

    @Test
    void hasQueueReturnsFalseAfterClear() {
        service.clear(playerId);
        assertFalse(service.hasQueue(UUID.randomUUID()));
    }

    @Test
    void flushQueueReturnsEmptyForNullPlayer() {
        Map<ResourceType, Integer> delivered = service.flushQueue(null);
        assertTrue(delivered.isEmpty());
    }

    @Test
    void getQueueReturnsCopy() {
        Map<ResourceType, Integer> q1 = service.getQueue(playerId);
        Map<ResourceType, Integer> q2 = service.getQueue(playerId);
        q1.put(ResourceType.IRON, 42);
        assertFalse(q2.containsKey(ResourceType.IRON));
    }
}
