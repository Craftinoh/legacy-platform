package it.legacynetwork.lobby.bossbar;

public final class BossBarProgress {
    private final BossBarProgressType type;
    private final double start;
    private final double end;
    private final int durationTicks;
    private final String currentPlaceholder;
    private final String maximumPlaceholder;
    private final double staticValue;
    private final double fallback;

    public BossBarProgress(BossBarProgressType type,
                           double start,
                           double end,
                           int durationTicks,
                           String currentPlaceholder,
                           String maximumPlaceholder,
                           double staticValue,
                           double fallback) {
        this.type = type;
        this.start = start;
        this.end = end;
        this.durationTicks = durationTicks;
        this.currentPlaceholder = currentPlaceholder;
        this.maximumPlaceholder = maximumPlaceholder;
        this.staticValue = staticValue;
        this.fallback = fallback;
    }

    public BossBarProgressType getType() {
        return type;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public String getCurrentPlaceholder() {
        return currentPlaceholder;
    }

    public String getMaximumPlaceholder() {
        return maximumPlaceholder;
    }

    public double getStaticValue() {
        return staticValue;
    }

    public double getFallback() {
        return fallback;
    }
}
