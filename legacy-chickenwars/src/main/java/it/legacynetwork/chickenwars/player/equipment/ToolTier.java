package it.legacynetwork.chickenwars.player.equipment;

/**
 * Progressione astratta di piccone e ascia.
 *
 * <p>Il materiale reale di ogni livello resta configurabile nello shop.</p>
 */
public enum ToolTier {

    NONE(0),
    TIER_1(1),
    TIER_2(2),
    TIER_3(3),
    TIER_4(4);

    private final int level;

    ToolTier(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean isHigherThan(ToolTier other) {
        return other == null || level > other.level;
    }

    public ToolTier downgradeAfterDeath() {
        if (this == NONE || this == TIER_1) {
            return this;
        }
        return fromLevel(level - 1);
    }

    public static ToolTier fromLevel(int level) {
        for (ToolTier tier : values()) {
            if (tier.level == level) {
                return tier;
            }
        }
        return level <= 0 ? NONE : TIER_4;
    }
}
