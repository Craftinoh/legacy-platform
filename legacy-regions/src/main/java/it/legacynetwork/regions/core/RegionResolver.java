package it.legacynetwork.regions.core;

import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionDecision;
import it.legacynetwork.regions.model.RegionFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class RegionResolver {

    private final RegionIndex regionIndex;
    private final Map<RegionFlag, FlagState> defaultFlags;

    public RegionResolver(RegionIndex regionIndex, Map<RegionFlag, FlagState> defaultFlags) {
        this.regionIndex = regionIndex;
        this.defaultFlags = defaultFlags;
    }

    public RegionDecision resolve(int x, int y, int z, RegionFlag flag) {
        List<CuboidRegion> candidates = regionIndex.getCandidates(x, z);
        if (candidates.isEmpty()) {
            FlagState defaultState = defaultFlags.get(flag);
            if (defaultState == null || defaultState == FlagState.ALLOW) {
                return RegionDecision.allowed();
            }
            if (defaultState == FlagState.DENY) {
                return RegionDecision.denied(null, 0, flag);
            }
            return RegionDecision.undecided(flag);
        }

        List<CuboidRegion> containing = new ArrayList<CuboidRegion>();
        for (int i = 0; i < candidates.size(); i++) {
            CuboidRegion region = candidates.get(i);
            if (region.contains(x, y, z)) {
                containing.add(region);
            }
        }

        if (containing.isEmpty()) {
            FlagState defaultState = defaultFlags.get(flag);
            if (defaultState == null || defaultState == FlagState.ALLOW) {
                return RegionDecision.allowed();
            }
            if (defaultState == FlagState.DENY) {
                return RegionDecision.denied(null, 0, flag);
            }
            return RegionDecision.undecided(flag);
        }

        Collections.sort(containing, new Comparator<CuboidRegion>() {
            @Override
            public int compare(CuboidRegion a, CuboidRegion b) {
                int priorityDiff = b.getPriority() - a.getPriority();
                if (priorityDiff != 0) {
                    return priorityDiff;
                }
                return a.getId().compareTo(b.getId());
            }
        });

        for (int i = 0; i < containing.size(); i++) {
            CuboidRegion region = containing.get(i);
            FlagState state = region.getFlag(flag);
            if (state == FlagState.ALLOW) {
                return RegionDecision.allowed();
            }
            if (state == FlagState.DENY) {
                return RegionDecision.denied(region.getId(), region.getPriority(), flag);
            }
        }

        FlagState defaultState = defaultFlags.get(flag);
        if (defaultState == FlagState.DENY) {
            return RegionDecision.denied(null, 0, flag);
        }
        if (defaultState == FlagState.ALLOW) {
            return RegionDecision.allowed();
        }
        return RegionDecision.undecided(flag);
    }

    public boolean isAllowed(int x, int y, int z, RegionFlag flag) {
        return resolve(x, y, z, flag).getFinalState() == FlagState.ALLOW;
    }

    public RegionIndex getRegionIndex() {
        return regionIndex;
    }

    public Map<RegionFlag, FlagState> getDefaultFlags() {
        return defaultFlags;
    }
}
