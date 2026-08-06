package it.legacynetwork.regions.core;

import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionDecision;
import it.legacynetwork.regions.model.RegionFlag;
import it.legacynetwork.regions.model.WorldRegionFlags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RegionResolver {

    private final RegionIndex regionIndex;
    private final Map<RegionFlag, FlagState> defaultFlags;
    private final Map<String, WorldRegionFlags> worldFlags;

    public RegionResolver(RegionIndex regionIndex,
                           Map<RegionFlag, FlagState> defaultFlags) {
        this(regionIndex, defaultFlags, new ArrayList<WorldRegionFlags>());
    }

    public RegionResolver(RegionIndex regionIndex,
                           Map<RegionFlag, FlagState> defaultFlags,
                           List<WorldRegionFlags> worldFlagsList) {
        if (regionIndex == null) {
            throw new IllegalArgumentException("RegionIndex mancante");
        }
        this.regionIndex = regionIndex;
        EnumMap<RegionFlag, FlagState> defaults =
                new EnumMap<RegionFlag, FlagState>(RegionFlag.class);
        if (defaultFlags != null) {
            defaults.putAll(defaultFlags);
        }
        this.defaultFlags = Collections.unmodifiableMap(defaults);

        Map<String, WorldRegionFlags> wf = new HashMap<String, WorldRegionFlags>();
        if (worldFlagsList != null) {
            for (WorldRegionFlags flags : worldFlagsList) {
                if (flags != null) {
                    wf.put(flags.getNormalizedName(), flags);
                }
            }
        }
        this.worldFlags = Collections.unmodifiableMap(wf);
    }

    public RegionDecision resolve(String worldName, String worldUuid,
                                   int x, int y, int z, RegionFlag flag) {
        RegionDecision regionDecision = resolveFromRegions(
                worldName, worldUuid, x, y, z, flag);
        if (regionDecision.getFinalState() != FlagState.INHERIT) {
            return regionDecision;
        }
        RegionDecision worldDecision = resolveFromWorld(worldName, flag);
        if (worldDecision.getFinalState() != FlagState.INHERIT) {
            return worldDecision;
        }
        return defaultDecision(flag);
    }

    public RegionDecision resolveEffective(String worldName, String worldUuid,
                                            int x, int y, int z,
                                            RegionFlag flag) {
        RegionDecision specificRegion = resolveFromRegions(
                worldName, worldUuid, x, y, z, flag);
        if (specificRegion.getFinalState() != FlagState.INHERIT) {
            return specificRegion;
        }

        RegionFlag generalFlag = flag.getGeneralFlag();
        if (generalFlag != null) {
            RegionDecision generalRegion = resolveFromRegions(
                    worldName, worldUuid, x, y, z, generalFlag);
            if (generalRegion.getFinalState() != FlagState.INHERIT) {
                return generalRegion;
            }
        }

        RegionDecision specificWorld = resolveFromWorld(worldName, flag);
        if (specificWorld.getFinalState() != FlagState.INHERIT) {
            return specificWorld;
        }

        if (generalFlag != null) {
            RegionDecision generalWorld = resolveFromWorld(worldName, generalFlag);
            if (generalWorld.getFinalState() != FlagState.INHERIT) {
                return generalWorld;
            }
        }

        FlagState specificDefault = defaultFlags.get(flag);
        if (specificDefault != null && specificDefault != FlagState.INHERIT) {
            return decisionFromDefault(flag, specificDefault);
        }

        if (generalFlag != null) {
            return defaultDecision(generalFlag);
        }
        return RegionDecision.allowed(null, 0, flag);
    }

    private RegionDecision resolveFromRegions(String worldName, String worldUuid,
                                               int x, int y, int z,
                                               RegionFlag flag) {
        if (flag == null) {
            throw new IllegalArgumentException("Flag mancante");
        }
        List<CuboidRegion> candidates = regionIndex.getCandidates(
                worldName, worldUuid, x, z);
        for (CuboidRegion region : candidates) {
            if (!region.matchesWorld(worldName, worldUuid)
                    || !region.contains(x, y, z)) {
                continue;
            }
            FlagState state = region.getFlag(flag);
            if (state == FlagState.ALLOW) {
                return RegionDecision.allowed(
                        region.getId(), region.getPriority(), flag);
            }
            if (state == FlagState.DENY) {
                return RegionDecision.denied(
                        region.getId(), region.getPriority(), flag);
            }
        }
        return RegionDecision.undecided(flag);
    }

    private RegionDecision resolveFromWorld(String worldName, RegionFlag flag) {
        if (worldName == null) {
            return RegionDecision.undecided(flag);
        }
        WorldRegionFlags wf = worldFlags.get(WorldRegionFlags.normalizeName(worldName));
        if (wf == null) {
            return RegionDecision.undecided(flag);
        }
        FlagState state = wf.getFlag(flag);
        if (state == FlagState.ALLOW) {
            return RegionDecision.fromWorld(wf.getWorldName(), flag, FlagState.ALLOW);
        }
        if (state == FlagState.DENY) {
            return RegionDecision.fromWorld(wf.getWorldName(), flag, FlagState.DENY);
        }
        return RegionDecision.undecided(flag);
    }

    public boolean isAllowed(String worldName, String worldUuid,
                              int x, int y, int z, RegionFlag flag) {
        return resolveEffective(worldName, worldUuid, x, y, z, flag).isAllowed();
    }

    private RegionDecision defaultDecision(RegionFlag flag) {
        FlagState state = defaultFlags.get(flag);
        if (state == null || state == FlagState.INHERIT || state == FlagState.ALLOW) {
            return RegionDecision.allowed(null, 0, flag);
        }
        return RegionDecision.denied(null, 0, flag);
    }

    private RegionDecision decisionFromDefault(RegionFlag flag, FlagState state) {
        if (state == FlagState.DENY) {
            return RegionDecision.fromDefault(flag, FlagState.DENY);
        }
        if (state == FlagState.ALLOW) {
            return RegionDecision.fromDefault(flag, FlagState.ALLOW);
        }
        return RegionDecision.undecided(flag);
    }

    public RegionIndex getRegionIndex() {
        return regionIndex;
    }

    public Map<RegionFlag, FlagState> getDefaultFlags() {
        return defaultFlags;
    }

    public Map<String, WorldRegionFlags> getWorldFlags() {
        return worldFlags;
    }

    public WorldRegionFlags getWorldFlags(String worldName) {
        return worldFlags.get(WorldRegionFlags.normalizeName(worldName));
    }
}
