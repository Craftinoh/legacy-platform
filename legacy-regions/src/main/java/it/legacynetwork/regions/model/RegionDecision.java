package it.legacynetwork.regions.model;

public final class RegionDecision {

    private final FlagState finalState;
    private final String decidingRegion;
    private final int priority;
    private final RegionFlag flag;

    private RegionDecision(FlagState finalState, String decidingRegion,
                           int priority, RegionFlag flag) {
        this.finalState = finalState;
        this.decidingRegion = decidingRegion;
        this.priority = priority;
        this.flag = flag;
    }

    public static RegionDecision allowed() {
        return allowed(null, 0, null);
    }

    public static RegionDecision allowed(String regionId, int priority, RegionFlag flag) {
        return new RegionDecision(FlagState.ALLOW, regionId, priority, flag);
    }

    public static RegionDecision denied(String regionId, int priority, RegionFlag flag) {
        return new RegionDecision(FlagState.DENY, regionId, priority, flag);
    }

    public static RegionDecision undecided(RegionFlag flag) {
        return new RegionDecision(FlagState.INHERIT, null, 0, flag);
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

    public boolean isAllowed() {
        return finalState == FlagState.ALLOW;
    }
}
