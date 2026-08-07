package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.progression.MatchRewardPolicy;
import it.legacynetwork.chickenwars.progression.ExperiencePolicy;

/** Dipendenze Opus 3 condivise da partite e listener. */
public final class ProgressionServices {
    private final ProfileLifecycleService profiles; private final MatchPersistence matches;
    private final MatchRewardPolicy rewards;
    private final ExperiencePolicy experience;
    private final CoinTransactionRepository coins;
    public ProgressionServices(ProfileLifecycleService profiles,MatchPersistence matches,CoinTransactionRepository coins,MatchRewardPolicy rewards,ExperiencePolicy experience){if(profiles==null||matches==null||coins==null||rewards==null||experience==null)throw new IllegalArgumentException("Servizi progressione incompleti");this.profiles=profiles;this.matches=matches;this.coins=coins;this.rewards=rewards;this.experience=experience;}
    public ProfileLifecycleService getProfiles(){return profiles;}
    public MatchPersistence getMatches(){return matches;}
    public MatchRewardPolicy getRewards(){return rewards;}
    public ExperiencePolicy getExperience(){return experience;}
    public CoinTransactionRepository getCoins(){return coins;}
}
