package it.legacynetwork.chickenwars.generator;

import static org.junit.jupiter.api.Assertions.*;
import it.legacynetwork.chickenwars.arena.GeneratorDefinition;
import it.legacynetwork.chickenwars.config.GeneratorTier;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeneratorServiceTest {
    private static final class Schedule implements GeneratorSchedule{private final CatchUpPolicy policy;Schedule(CatchUpPolicy policy){this.policy=policy;}public GeneratorTier tier(ResourceType type,int level){return new GeneratorTier(level==1?10:5,level);}public CatchUpPolicy catchUpPolicy(){return policy;}public int maximumCatchUpDrops(){return 2;}}
    private static final class Sink implements GeneratorDropSink{int calls,amount;boolean accept=true;public boolean drop(GeneratorState state,int amount){calls++;this.amount+=amount;return accept;}public void cleanup(){calls=-100;}}
    private GeneratorState state(String id,ResourceType type){return new GeneratorState("match",new GeneratorDefinition(id,type,new SimpleLocation("world",0,64,0,0,0),null,1,false));}
    @Test void startsTicksAndStopsOnce(){Sink sink=new Sink();GeneratorService service=new GeneratorService(new Schedule(CatchUpPolicy.LIMITED),sink);service.add(state("iron",ResourceType.IRON));service.start(0);service.start(0);assertEquals(0,service.tick(9));assertEquals(1,service.tick(10));assertEquals(1,sink.amount);service.stop();assertFalse(service.isRunning());assertEquals(-100,sink.calls);assertEquals(0,service.tick(20));}
    @Test void limitsLagCatchUp(){Sink sink=new Sink();GeneratorService service=new GeneratorService(new Schedule(CatchUpPolicy.LIMITED),sink);service.add(state("gold",ResourceType.GOLD));service.start(0);service.tick(100);assertEquals(2,sink.amount);assertEquals(110,service.states().get(0).getNextTick());}
    @Test void skipPolicyNeverBursts(){Sink sink=new Sink();GeneratorService service=new GeneratorService(new Schedule(CatchUpPolicy.SKIP),sink);service.add(state("diamond",ResourceType.DIAMOND));service.start(0);service.tick(1000);assertEquals(1,sink.amount);}
    @Test void phaseUpgradeChangesTierWithoutDuplicate(){Sink sink=new Sink();GeneratorService service=new GeneratorService(new Schedule(CatchUpPolicy.LIMITED),sink);service.add(state("emerald",ResourceType.EMERALD));service.start(0);service.setTier(ResourceType.EMERALD,2,1);service.tick(6);assertEquals(2,sink.amount);assertEquals(1,service.states().size());}
    @Test void featherUsesSingleConfiguredGenerator(){Sink sink=new Sink();GeneratorService service=new GeneratorService(new Schedule(CatchUpPolicy.SKIP),sink);service.add(state("feather",ResourceType.FEATHER));service.start(0);service.tick(10);assertEquals(ResourceType.FEATHER,service.states().get(0).getType());assertEquals(1,sink.calls);}
    @Test void duplicateIdsAreRejectedWithoutReplacingOriginal(){GeneratorService service=new GeneratorService(new Schedule(CatchUpPolicy.SKIP),new Sink());service.add(state("same",ResourceType.IRON));assertThrows(IllegalArgumentException.class,()->service.add(state("same",ResourceType.GOLD)));assertEquals(ResourceType.IRON,service.states().get(0).getType());}
}
