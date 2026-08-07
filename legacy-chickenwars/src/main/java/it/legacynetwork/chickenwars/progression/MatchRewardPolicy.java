package it.legacynetwork.chickenwars.progression;

/** Bilanciamento YAML delle ricompense persistenti. */
public final class MatchRewardPolicy {
    private final long participationXp,winXp,killXp,finalKillXp;
    private final long participationCoins,winCoins,killCoins,finalKillCoins;
    private final long resourceXp;
    public MatchRewardPolicy(long participationXp,long winXp,long killXp,long finalKillXp,
            long participationCoins,long winCoins,long killCoins,long finalKillCoins){
        this(participationXp,winXp,killXp,finalKillXp,participationCoins,
                winCoins,killCoins,finalKillCoins,0L);
    }
    public MatchRewardPolicy(long participationXp,long winXp,long killXp,long finalKillXp,
            long participationCoins,long winCoins,long killCoins,long finalKillCoins,
            long resourceXp){
        this.participationXp=valid(participationXp);this.winXp=valid(winXp);this.killXp=valid(killXp);this.finalKillXp=valid(finalKillXp);
        this.participationCoins=valid(participationCoins);this.winCoins=valid(winCoins);this.killCoins=valid(killCoins);this.finalKillCoins=valid(finalKillCoins);
        this.resourceXp=valid(resourceXp);
    }
    public MatchRewards calculate(boolean winner,long kills,long finalKills){return new MatchRewards(
            add(add(participationXp,winner?winXp:0),add(multiply(killXp,kills),multiply(finalKillXp,finalKills))),
            add(add(participationCoins,winner?winCoins:0),add(multiply(killCoins,kills),multiply(finalKillCoins,finalKills))));}
    public MatchRewards calculate(boolean winner,long kills,long finalKills,long resources){MatchRewards base=calculate(winner,kills,finalKills);return new MatchRewards(add(base.getExperience(),multiply(resourceXp,resources)),base.getCoins());}
    private long valid(long v){if(v<0)throw new IllegalArgumentException("Ricompensa negativa");return v;}
    private long add(long a,long b){return Long.MAX_VALUE-a<b?Long.MAX_VALUE:a+b;}
    private long multiply(long a,long b){return b<=0?0:a>Long.MAX_VALUE/b?Long.MAX_VALUE:a*b;}
}
