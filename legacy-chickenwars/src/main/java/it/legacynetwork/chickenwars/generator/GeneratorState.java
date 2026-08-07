package it.legacynetwork.chickenwars.generator;

import it.legacynetwork.chickenwars.arena.GeneratorDefinition;
import it.legacynetwork.chickenwars.config.GeneratorTier;
import it.legacynetwork.chickenwars.model.ResourceType;

/** Stato temporale puro di un generatore appartenente a una partita. */
public final class GeneratorState {
    private final String matchId;private final GeneratorDefinition definition;
    private int level;private long nextTick;private boolean running;
    public GeneratorState(String matchId,GeneratorDefinition definition){if(matchId==null||definition==null)throw new IllegalArgumentException("Generatore incompleto");this.matchId=matchId;this.definition=definition;this.level=definition.getLevel();}
    public void start(long currentTick,GeneratorSchedule schedule){if(running)return;running=true;nextTick=currentTick+schedule.tier(definition.getType(),level).getIntervalTicks();}
    public int poll(long currentTick,GeneratorSchedule schedule){if(!running||currentTick<nextTick)return 0;GeneratorTier tier=schedule.tier(definition.getType(),level);long due=1L+(currentTick-nextTick)/tier.getIntervalTicks();int cycles=schedule.catchUpPolicy()==CatchUpPolicy.SKIP?1:(int)Math.min(due,schedule.maximumCatchUpDrops());nextTick=currentTick+tier.getIntervalTicks();return cycles*tier.getAmount();}
    public void stop(){running=false;}
    public void setLevel(int level,long currentTick,GeneratorSchedule schedule){this.level=Math.max(1,level);if(running)nextTick=Math.min(nextTick,currentTick+schedule.tier(definition.getType(),this.level).getIntervalTicks());}
    public String getMatchId(){return matchId;}public GeneratorDefinition getDefinition(){return definition;}public String getId(){return definition.getId();}public int getLevel(){return level;}public long getNextTick(){return nextTick;}public boolean isRunning(){return running;}
    public ResourceType getType(){return definition.getType();}
}
