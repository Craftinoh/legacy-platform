package it.legacynetwork.chickenwars.progression;

/**
 * Ricompense permanenti maturate in una partita.
 */
public final class MatchRewards {

    private final long experience;
    private final long coins;

    public MatchRewards(long experience, long coins) {
        this.experience = Math.max(0L, experience);
        this.coins = Math.max(0L, coins);
    }

    public long getExperience() {
        return experience;
    }

    public long getCoins() {
        return coins;
    }

    public boolean isEmpty() {
        return experience == 0L && coins == 0L;
    }
}
