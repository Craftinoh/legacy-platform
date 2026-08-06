package it.legacynetwork.regions.model;

public final class RegionDecision {
    public enum Source { REGION, WORLD, DEFAULT }

    private final FlagState finalState;
    private final String decidingRegion;
    private final int priority;
    private final RegionFlag flag;
    private final Source source;

    private RegionDecision(FlagState finalState, String decidingRegion,
                            int priority, RegionFlag flag, Source source) {
        this.finalState = finalState;
        this.decidingRegion = decidingRegion;
        this.priority = priority;
        this.flag = flag;
        this.source = source;
    }

    public static RegionDecision allowed() {
        return new RegionDecision(FlagState.ALLOW, null, 0, null, Source.DEFAULT);
    }

    public static RegionDecision allowed(String regionId, int priority, RegionFlag flag) {
        return new RegionDecision(FlagState.ALLOW, regionId, priority, flag,
                regionId != null ? Source.REGION : Source.DEFAULT);
    }

    public static RegionDecision denied(String regionId, int priority, RegionFlag flag) {
        return new RegionDecision(FlagState.DENY, regionId, priority, flag,
                regionId != null ? Source.REGION : Source.DEFAULT);
    }

    public static RegionDecision undecided(RegionFlag flag) {
        return new RegionDecision(FlagState.INHERIT, null, 0, flag, Source.DEFAULT);
    }

    public static RegionDecision fromWorld(String worldName, RegionFlag flag,
                                            FlagState state) {
        return new RegionDecision(state, "world:" + worldName, 0, flag, Source.WORLD);
    }

    public static RegionDecision fromDefault(RegionFlag flag, FlagState state) {
        return new RegionDecision(state, "global", 0, flag, Source.DEFAULT);
    }

    public FlagState getFinalState() {
        return finalState;
    }

    public String getDecidingRegion() {
        return decidingRegion;
    }

    public int getPriority() {
        return priority;
    }

    public RegionFlag getFlag() {
        return flag;
    }

    public Source getSource() {
        return source;
    }

    public boolean isAllowed() {
        return finalState == FlagState.ALLOW;
    }
}
