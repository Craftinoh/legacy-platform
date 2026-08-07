package it.legacynetwork.chickenwars.player.equipment;

/**
 * Tier permanente di leggings e stivali acquistato nella partita.
 */
public enum ArmorTier {

    LEATHER(0),
    CHAINMAIL(1),
    IRON(2),
    DIAMOND(3);

    private final int level;

    ArmorTier(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean isHigherThan(ArmorTier other) {
        return other == null || level > other.level;
    }
}
