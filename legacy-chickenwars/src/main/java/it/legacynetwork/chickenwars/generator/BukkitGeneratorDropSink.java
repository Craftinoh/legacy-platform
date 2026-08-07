package it.legacynetwork.chickenwars.generator;

import it.legacynetwork.chickenwars.config.GeneratorSettings;
import it.legacynetwork.chickenwars.model.ResourceType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Unico adapter Bukkit per drop, cap, chunk e cleanup dei generatori. */
public final class BukkitGeneratorDropSink implements GeneratorDropSink {
    public interface MainThreadGuard { boolean isPrimaryThread(); }

    private static final Vector ZERO = new Vector(0, 0, 0);
    private final GeneratorSettings settings;
    private final Map<String, Location> locations;
    private final GeneratedResourceRegistry registry;
    private final MainThreadGuard mainThread;
    private final List<Item> spawned = new ArrayList<Item>();

    public BukkitGeneratorDropSink(GeneratorSettings settings,
                                   Map<String, Location> locations,
                                   GeneratedResourceRegistry registry) {
        this(settings, locations, registry, new MainThreadGuard() {
            @Override public boolean isPrimaryThread() {
                return Bukkit.isPrimaryThread();
            }
        });
    }

    public BukkitGeneratorDropSink(GeneratorSettings settings,
                                   Map<String, Location> locations,
                                   GeneratedResourceRegistry registry,
                                   MainThreadGuard mainThread) {
        if (settings == null || locations == null || registry == null
                || mainThread == null) {
            throw new IllegalArgumentException("Drop sink incompleto");
        }
        this.settings = settings;
        this.locations = new HashMap<String, Location>(locations);
        this.registry = registry;
        this.mainThread = mainThread;
    }

    @Override
    public boolean drop(GeneratorState state, int amount) {
        if (!mainThread.isPrimaryThread()) {
            throw new IllegalStateException(
                    "Drop generatore fuori dal main thread");
        }
        if (!settings.isEnabled() || state == null || amount <= 0) return false;
        Location location = locations.get(state.getId());
        if (location == null || location.getWorld() == null) return false;
        World world = location.getWorld();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            if (settings.getChunkPolicy() == ChunkPolicy.REQUIRE_LOADED) {
                return false;
            }
            world.loadChunk(chunkX, chunkZ);
        }

        ResourceType type = state.getDefinition().getType();
        double radius = settings.getMergeRadius();
        int ground = 0;
        List<Item> equivalent = new ArrayList<Item>();
        Collection<Entity> nearby = world.getNearbyEntities(location, radius,
                radius, radius);
        for (Entity entity : nearby) {
            if (!(entity instanceof Item)) continue;
            ItemStack stack = ((Item) entity).getItemStack();
            if (stack != null && stack.getType() == type.getMaterial()) {
                ground += stack.getAmount();
                equivalent.add((Item) entity);
            }
        }
        int remaining = Math.min(amount, Math.max(0,
                settings.getMaximumGroundItems() - ground));
        if (remaining <= 0) return false;

        if (settings.isItemStacking()) {
            remaining = mergeGenerated(state, equivalent, remaining);
            if (remaining == 0) return true;
        }
        while (remaining > 0) {
            int stackAmount = Math.min(type.getMaterial().getMaxStackSize(),
                    remaining);
            Item item = world.dropItem(location,
                    new ItemStack(type.getMaterial(), stackAmount));
            item.setVelocity(ZERO);
            spawned.add(item);
            registry.register(item.getUniqueId(), state.getMatchId(), type);
            remaining -= stackAmount;
        }
        return true;
    }

    private int mergeGenerated(GeneratorState state, List<Item> equivalent,
                               int remaining) {
        for (Item item : equivalent) {
            if (!registry.contains(item.getUniqueId(), state.getMatchId())) {
                continue;
            }
            ItemStack stack = item.getItemStack();
            int moved = Math.min(stack.getMaxStackSize() - stack.getAmount(),
                    remaining);
            if (moved > 0) {
                stack.setAmount(stack.getAmount() + moved);
                item.setItemStack(stack);
                remaining -= moved;
            }
            if (remaining == 0) break;
        }
        return remaining;
    }

    @Override
    public void cleanup() {
        for (Item item : spawned) {
            if (item != null && !item.isDead()) item.remove();
        }
        spawned.clear();
    }
}
