package it.legacynetwork.chickenwars.generator;

import it.legacynetwork.chickenwars.arena.GeneratorDefinition;
import it.legacynetwork.chickenwars.config.GeneratorSettings;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BukkitGeneratorDropSinkTest {
    @Test
    void unloadedChunkIsSkippedByRequireLoadedPolicy() throws Exception {
        Fixture fixture = fixture("REQUIRE_LOADED", 32);
        when(fixture.world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);

        assertFalse(fixture.sink.drop(fixture.state, 1));
        verify(fixture.world, never()).loadChunk(anyInt(), anyInt());
        verify(fixture.world, never()).dropItem(any(Location.class),
                any(ItemStack.class));
    }

    @Test
    void loadPolicyDropsRegistersAndCleansItem() throws Exception {
        Fixture fixture = fixture("LOAD", 32);
        Item item = mock(Item.class);
        UUID itemId = UUID.randomUUID();
        when(item.getUniqueId()).thenReturn(itemId);
        when(item.isDead()).thenReturn(false);
        when(fixture.world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        when(fixture.world.getNearbyEntities(any(Location.class), anyDouble(),
                anyDouble(), anyDouble()))
                .thenReturn(Collections.<Entity>emptyList());
        when(fixture.world.dropItem(any(Location.class), any(ItemStack.class)))
                .thenReturn(item);

        assertTrue(fixture.sink.drop(fixture.state, 2));
        verify(fixture.world).loadChunk(1, 1);
        assertTrue(fixture.registry.contains(itemId, "match"));
        fixture.sink.cleanup();
        verify(item).remove();
    }

    @Test
    void equivalentGroundItemsEnforceCap() throws Exception {
        Fixture fixture = fixture("REQUIRE_LOADED", 2);
        Item existing = mock(Item.class);
        when(existing.getItemStack()).thenReturn(
                new ItemStack(ResourceType.IRON.getMaterial(), 2));
        when(fixture.world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(fixture.world.getNearbyEntities(any(Location.class), anyDouble(),
                anyDouble(), anyDouble()))
                .thenReturn(Collections.<Entity>singletonList(existing));

        assertFalse(fixture.sink.drop(fixture.state, 1));
        verify(fixture.world, never()).dropItem(any(Location.class),
                any(ItemStack.class));
    }

    @Test
    void worldAccessOutsideMainThreadIsRejected() throws Exception {
        Fixture fixture = fixture("REQUIRE_LOADED", 2, false);
        assertThrows(IllegalStateException.class, () ->
                fixture.sink.drop(fixture.state, 1));
    }

    private Fixture fixture(String chunkPolicy, int cap) throws Exception {
        return fixture(chunkPolicy, cap, true);
    }

    private Fixture fixture(String chunkPolicy, int cap, boolean mainThread)
            throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("generators:\n  chunk-policy: " + chunkPolicy
                + "\n  maximum-ground-items: " + cap + "\n");
        GeneratorSettings settings = GeneratorSettings.fromSection(
                yaml.getConfigurationSection("generators"));
        World world = mock(World.class);
        Location location = new Location(world, 16, 64, 16);
        Map<String, Location> locations = new HashMap<String, Location>();
        locations.put("iron", location);
        GeneratedResourceRegistry registry = new GeneratedResourceRegistry();
        GeneratorState state = new GeneratorState("match",
                new GeneratorDefinition("iron", ResourceType.IRON,
                        new SimpleLocation("arena", 16, 64, 16, 0, 0),
                        null, 1, false));
        BukkitGeneratorDropSink sink = new BukkitGeneratorDropSink(settings,
                locations, registry, () -> mainThread);
        return new Fixture(world, registry, state, sink);
    }

    private static final class Fixture {
        private final World world;
        private final GeneratedResourceRegistry registry;
        private final GeneratorState state;
        private final BukkitGeneratorDropSink sink;
        private Fixture(World world, GeneratedResourceRegistry registry,
                        GeneratorState state, BukkitGeneratorDropSink sink) {
            this.world = world;
            this.registry = registry;
            this.state = state;
            this.sink = sink;
        }
    }
}
