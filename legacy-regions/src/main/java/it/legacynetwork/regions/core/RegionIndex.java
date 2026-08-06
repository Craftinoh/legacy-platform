package it.legacynetwork.regions.core;

import it.legacynetwork.regions.model.CuboidRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RegionIndex {

    private static final Comparator<CuboidRegion> REGION_ORDER =
            new Comparator<CuboidRegion>() {
                @Override
                public int compare(CuboidRegion first, CuboidRegion second) {
                    int priority = Integer.compare(
                            second.getPriority(), first.getPriority());
                    if (priority != 0) {
                        return priority;
                    }
                    return first.getId().compareTo(second.getId());
                }
            };

    private volatile Map<String, Map<Long, List<CuboidRegion>>> index =
            Collections.emptyMap();

    public void build(List<CuboidRegion> regions) {
        Map<String, Map<Long, List<CuboidRegion>>> mutable =
                new HashMap<String, Map<Long, List<CuboidRegion>>>();
        if (regions != null) {
            for (CuboidRegion region : regions) {
                String worldKey = worldNameKey(region.getWorldName());
                if (worldKey == null) {
                    worldKey = worldUuidKey(region.getWorldUuid());
                }
                indexRegion(mutable, worldKey, region);
            }
        }
        this.index = freeze(mutable);
    }

    private void indexRegion(Map<String, Map<Long, List<CuboidRegion>>> target,
                             String worldKey, CuboidRegion region) {
        if (worldKey == null) {
            return;
        }
        Map<Long, List<CuboidRegion>> worldIndex = target.get(worldKey);
        if (worldIndex == null) {
            worldIndex = new HashMap<Long, List<CuboidRegion>>();
            target.put(worldKey, worldIndex);
        }

        int minChunkX = region.getMinX() >> 4;
        int minChunkZ = region.getMinZ() >> 4;
        int maxChunkX = region.getMaxX() >> 4;
        int maxChunkZ = region.getMaxZ() >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long key = chunkKey(chunkX, chunkZ);
                List<CuboidRegion> candidates = worldIndex.get(key);
                if (candidates == null) {
                    candidates = new ArrayList<CuboidRegion>();
                    worldIndex.put(key, candidates);
                }
                candidates.add(region);
            }
        }
    }

    private Map<String, Map<Long, List<CuboidRegion>>> freeze(
            Map<String, Map<Long, List<CuboidRegion>>> mutable) {
        Map<String, Map<Long, List<CuboidRegion>>> frozenWorlds =
                new HashMap<String, Map<Long, List<CuboidRegion>>>();
        for (Map.Entry<String, Map<Long, List<CuboidRegion>>> worldEntry
                : mutable.entrySet()) {
            Map<Long, List<CuboidRegion>> frozenChunks =
                    new HashMap<Long, List<CuboidRegion>>();
            for (Map.Entry<Long, List<CuboidRegion>> chunkEntry
                    : worldEntry.getValue().entrySet()) {
                List<CuboidRegion> ordered =
                        new ArrayList<CuboidRegion>(chunkEntry.getValue());
                Collections.sort(ordered, REGION_ORDER);
                frozenChunks.put(chunkEntry.getKey(),
                        Collections.unmodifiableList(ordered));
            }
            frozenWorlds.put(worldEntry.getKey(),
                    Collections.unmodifiableMap(frozenChunks));
        }
        return Collections.unmodifiableMap(frozenWorlds);
    }

    public List<CuboidRegion> getCandidates(String worldName, String worldUuid,
                                             int blockX, int blockZ) {
        long key = chunkKey(blockX >> 4, blockZ >> 4);
        List<CuboidRegion> candidates = candidatesFor(
                worldNameKey(worldName), key);
        if (candidates != null) {
            return candidates;
        }
        candidates = candidatesFor(worldUuidKey(worldUuid), key);
        return candidates == null ? Collections.<CuboidRegion>emptyList()
                : candidates;
    }

    private List<CuboidRegion> candidatesFor(String worldKey, long chunkKey) {
        if (worldKey == null) {
            return null;
        }
        Map<Long, List<CuboidRegion>> worldIndex = index.get(worldKey);
        return worldIndex == null ? null : worldIndex.get(chunkKey);
    }

    public int size() {
        int chunks = 0;
        for (Map<Long, List<CuboidRegion>> worldIndex : index.values()) {
            chunks += worldIndex.size();
        }
        return chunks;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static String worldNameKey(String worldName) {
        if (worldName == null || worldName.trim().isEmpty()) {
            return null;
        }
        return "name:" + worldName.trim().toLowerCase(Locale.ROOT);
    }

    private static String worldUuidKey(String worldUuid) {
        if (worldUuid == null || worldUuid.trim().isEmpty()) {
            return null;
        }
        return "uuid:" + worldUuid.trim().toLowerCase(Locale.ROOT);
    }
}
