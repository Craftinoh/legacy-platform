package it.legacynetwork.regions.selection;

public final class RegionSelection {
    private final String worldName;
    private final String worldUuid;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public RegionSelection(String worldName, String worldUuid,
                           int minX, int minY, int minZ,
                           int maxX, int maxY, int maxZ) {
        this.worldName = worldName;
        this.worldUuid = worldUuid;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
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
}
