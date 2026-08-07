package it.legacynetwork.chickenwars.player.equipment;

/**
 * Tier runtime della spada. Le spade oltre WOOD non persistono dopo la morte.
 */
public enum SwordTier {

    WOOD(0),
    STONE(1),
    IRON(2),
    DIAMOND(3);

    private final int level;

    SwordTier(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean isHigherThan(SwordTier other) {
        return other == null || level > other.level;
    }
}
