package it.legacynetwork.regions.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class CuboidRegion {

    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9._-]+");

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
        this.id = normalizeId(id);
        this.worldName = normalizeOptional(worldName);
        this.worldUuid = normalizeWorldUuid(worldUuid);
        if (this.worldName == null && this.worldUuid == null) {
            throw new IllegalArgumentException("La regione deve avere un mondo valido");
        }
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.priority = priority;
        Map<RegionFlag, FlagState> safeFlags = flags == null
                ? Collections.<RegionFlag, FlagState>emptyMap()
                : flags;
        this.flags = Collections.unmodifiableMap(
                new HashMap<RegionFlag, FlagState>(safeFlags));
    }

    public static String normalizeId(String rawId) {
        if (rawId == null) {
            throw new IllegalArgumentException("ID regione mancante");
        }
        String normalized = rawId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || !VALID_ID.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "ID regione non valido: usa solo lettere, numeri, punto, trattino e underscore");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeWorldUuid(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        try {
            return UUID.fromString(normalized).toString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("UUID mondo non valido: " + normalized);
        }
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean matchesWorld(String candidateWorldName, String candidateWorldUuid) {
        String normalizedUuid = normalizeOptional(candidateWorldUuid);
        if (worldUuid != null && normalizedUuid != null
                && worldUuid.equalsIgnoreCase(normalizedUuid)) {
            return true;
        }
        String normalizedName = normalizeOptional(candidateWorldName);
        return worldName != null && normalizedName != null
                && worldName.equalsIgnoreCase(normalizedName);
    }

    public FlagState getFlag(RegionFlag flag) {
        FlagState state = flags.get(flag);
        return state == null ? FlagState.INHERIT : state;
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
