package it.legacynetwork.regions.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class CuboidRegion {

    private final String id;
    private final String worldName;
    private final String worldUuid;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final int priority;
    private final Map<RegionFlag, FlagState> flags;

    public CuboidRegion(String id, String worldName, String worldUuid,
                        int x1, int y1, int z1, int x2, int y2, int z2,
                        int priority, Map<RegionFlag, FlagState> flags) {
        this.id = id;
        this.worldName = worldName;
        this.worldUuid = worldUuid;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.priority = priority;
        this.flags = Collections.unmodifiableMap(new HashMap<RegionFlag, FlagState>(flags));
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public FlagState getFlag(RegionFlag flag) {
        FlagState state = flags.get(flag);
        if (state != null) {
            return state;
        }
        return FlagState.INHERIT;
    }

    public String getId() {
        return id;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getWorldUuid() {
        return worldUuid;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getPriority() {
        return priority;
    }

    public Map<RegionFlag, FlagState> getFlags() {
        return flags;
    }
}
