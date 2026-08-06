package it.legacynetwork.regions.core;

import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionDecision;
import it.legacynetwork.regions.model.RegionFlag;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RegionResolver {

    private final RegionIndex regionIndex;
    private final Map<RegionFlag, FlagState> defaultFlags;

    public RegionResolver(RegionIndex regionIndex,
                          Map<RegionFlag, FlagState> defaultFlags) {
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
    }

    public RegionDecision resolve(String worldName, String worldUuid,
                                  int x, int y, int z, RegionFlag flag) {
        RegionDecision explicit = resolveExplicit(
                worldName, worldUuid, x, y, z, flag);
        if (explicit.getFinalState() != FlagState.INHERIT) {
            return explicit;
        }
        return defaultDecision(flag);
    }

    public RegionDecision resolveEffective(String worldName, String worldUuid,
                                           int x, int y, int z,
                                           RegionFlag flag) {
        RegionDecision specific = resolveExplicit(
                worldName, worldUuid, x, y, z, flag);
        if (specific.getFinalState() != FlagState.INHERIT) {
            return specific;
        }

        RegionFlag generalFlag = flag.getGeneralFlag();
        if (generalFlag != null) {
            RegionDecision general = resolveExplicit(
                    worldName, worldUuid, x, y, z, generalFlag);
            if (general.getFinalState() != FlagState.INHERIT) {
                return general;
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

    public RegionDecision resolveExplicit(String worldName, String worldUuid,
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
            return RegionDecision.denied(null, 0, flag);
        }
        if (state == FlagState.ALLOW) {
            return RegionDecision.allowed(null, 0, flag);
        }
        return RegionDecision.undecided(flag);
    }

    public RegionIndex getRegionIndex() {
        return regionIndex;
    }

    public Map<RegionFlag, FlagState> getDefaultFlags() {
        return defaultFlags;
    }
}
