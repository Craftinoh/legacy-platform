package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.progression.MatchRewardPolicy;
import it.legacynetwork.chickenwars.progression.ExperiencePolicy;

/** Dipendenze Opus 3 condivise da partite e listener. */
public final class ProgressionServices {
    private final ProfileLifecycleService profiles; private final MatchPersistence matches;
    private final MatchRewardPolicy rewards;
    private final ExperiencePolicy experience;
    private final CoinTransactionRepository coins;
    private final MatchFinalizationService finalizer;
    public ProgressionServices(ProfileLifecycleService profiles,MatchPersistence matches,CoinTransactionRepository coins,MatchRewardPolicy rewards,ExperiencePolicy experience){this(profiles,matches,coins,rewards,experience,0);}
    public ProgressionServices(ProfileLifecycleService profiles,MatchPersistence matches,CoinTransactionRepository coins,MatchRewardPolicy rewards,ExperiencePolicy experience,int maximumRetries){if(profiles==null||matches==null||coins==null||rewards==null||experience==null)throw new IllegalArgumentException("Servizi progressione incompleti");this.profiles=profiles;this.matches=matches;this.coins=coins;this.rewards=rewards;this.experience=experience;this.finalizer=new MatchFinalizationService(matches,maximumRetries);}
    public ProfileLifecycleService getProfiles(){return profiles;}
    public MatchPersistence getMatches(){return matches;}
    public MatchRewardPolicy getRewards(){return rewards;}
    public ExperiencePolicy getExperience(){return experience;}
    public CoinTransactionRepository getCoins(){return coins;}
    public MatchFinalizationService getFinalizer(){return finalizer;}
    public void applyFinalized(MatchFinalizationRequest request){for(MatchParticipantRecord participant:request.getParticipants()){PlayerProfile profile=profiles.get(participant.getPlayerId());if(profile==null)continue;it.legacynetwork.chickenwars.progression.ChickenWarsProgress progress=profile.getProgress().toProgress(experience);progress.addExperience(participant.getExperience());progress.addCoins(participant.getCoins());profile.updateProgress(PlayerProgressRecord.from(progress,request.getFinishedAtEpochMillis()));}}
}
