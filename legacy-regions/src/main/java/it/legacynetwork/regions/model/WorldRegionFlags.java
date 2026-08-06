package it.legacynetwork.regions.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class WorldRegionFlags {
    private final String worldName;
    private final String worldUuid;
    private final Map<RegionFlag, FlagState> flags;

    public WorldRegionFlags(String worldName, String worldUuid,
                             Map<RegionFlag, FlagState> flags) {
        this.worldName = worldName;
        this.worldUuid = worldUuid;
        EnumMap<RegionFlag, FlagState> copy = new EnumMap<>(RegionFlag.class);
        if (flags != null) {
            copy.putAll(flags);
        }
        this.flags = Collections.unmodifiableMap(copy);
    }

    public String getWorldName() {
        return worldName;
    }

    public String getWorldUuid() {
        return worldUuid;
    }

    public Map<RegionFlag, FlagState> getFlags() {
        return flags;
    }

    public FlagState getFlag(RegionFlag flag) {
        return flags.getOrDefault(flag, FlagState.INHERIT);
    }

    public String getNormalizedName() {
        return worldName.toLowerCase();
    }

    public static String normalizeName(String name) {
        return name != null ? name.toLowerCase() : null;
    }
}
