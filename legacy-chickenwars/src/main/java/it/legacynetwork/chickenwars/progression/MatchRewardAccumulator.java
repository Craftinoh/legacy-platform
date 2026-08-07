package it.legacynetwork.chickenwars.progression;

import it.legacynetwork.chickenwars.mode.ModeProfile;

/**
 * Accumula XP e coins durante una partita senza effettuare scritture database.
 *
 * <p>Le modalita' con ricompense disabilitate, come DUEL, ignorano ogni
 * assegnazione permanente pur continuando a permettere statistiche runtime.</p>
 */
public final class MatchRewardAccumulator {

    private final ModeProfile profile;
    private long experience;
    private long coins;

    public MatchRewardAccumulator(ModeProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profilo modalita' mancante");
        }
        this.profile = profile;
    }

    public synchronized void awardExperience(long amount) {
        if (!profile.isRewardsEnabled() || amount <= 0L) {
            return;
        }
        experience = safeAdd(experience, amount);
    }

    public synchronized void awardCoins(long amount) {
        if (!profile.isRewardsEnabled() || amount <= 0L) {
            return;
        }
        coins = safeAdd(coins, amount);
    }

    public synchronized MatchRewards snapshot() {
        return new MatchRewards(experience, coins);
    }

    public synchronized MatchRewards drain() {
        MatchRewards rewards = snapshot();
        experience = 0L;
        coins = 0L;
        return rewards;
    }

    private long safeAdd(long current, long amount) {
        return Long.MAX_VALUE - current < amount
                ? Long.MAX_VALUE : current + amount;
    }

    public ModeProfile getProfile() {
        return profile;
    }
}
