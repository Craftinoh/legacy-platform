package it.legacynetwork.chickenwars.generator;

import static org.junit.jupiter.api.Assertions.*;
import it.legacynetwork.chickenwars.model.ResourceType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GeneratedResourceRegistryTest {
    @Test void onlyNaturalPickupIsConsumedOnce(){GeneratedResourceRegistry registry=new GeneratedResourceRegistry();UUID id=UUID.randomUUID();registry.register(id,"match",ResourceType.FEATHER);assertNull(registry.consume(id,"other"));assertEquals(ResourceType.FEATHER,registry.consume(id,"match"));assertNull(registry.consume(id,"match"));}
    @Test void matchCleanupDoesNotTouchAnotherArena(){GeneratedResourceRegistry registry=new GeneratedResourceRegistry();UUID one=UUID.randomUUID(),two=UUID.randomUUID();registry.register(one,"one",ResourceType.IRON);registry.register(two,"two",ResourceType.GOLD);registry.clearMatch("one");assertNull(registry.consume(one,"one"));assertEquals(ResourceType.GOLD,registry.consume(two,"two"));}
    @Test void partialPickupRemainsNaturalUntilTheEntityIsConsumed(){GeneratedResourceRegistry registry=new GeneratedResourceRegistry();UUID id=UUID.randomUUID();registry.register(id,"match",ResourceType.IRON);assertEquals(ResourceType.IRON,registry.pickup(id,"match",false));assertEquals(ResourceType.IRON,registry.pickup(id,"match",true));assertNull(registry.pickup(id,"match",true));}
}
