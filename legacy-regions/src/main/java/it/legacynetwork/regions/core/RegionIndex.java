package it.legacynetwork.regions.core;

import it.legacynetwork.regions.model.CuboidRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionIndex {

    private volatile Map<Long, List<CuboidRegion>> index = new ConcurrentHashMap<Long, List<CuboidRegion>>();

    public void build(List<CuboidRegion> regions) {
        Map<Long, List<CuboidRegion>> newIndex = new ConcurrentHashMap<Long, List<CuboidRegion>>();
        if (regions == null) {
            this.index = newIndex;
            return;
        }
        for (int i = 0; i < regions.size(); i++) {
            CuboidRegion region = regions.get(i);
            int minChunkX = region.getMinX() >> 4;
            int minChunkZ = region.getMinZ() >> 4;
            int maxChunkX = region.getMaxX() >> 4;
            int maxChunkZ = region.getMaxZ() >> 4;
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
                    List<CuboidRegion> list = newIndex.get(key);
                    if (list == null) {
                        list = new ArrayList<CuboidRegion>();
                        newIndex.put(key, list);
                    }
                    list.add(region);
                }
            }
        }
        this.index = newIndex;
    }

    public List<CuboidRegion> getCandidates(int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        List<CuboidRegion> candidates = index.get(key);
        if (candidates != null) {
            return candidates;
        }
        return Collections.emptyList();
    }

    public int size() {
        return index.size();
    }
}
