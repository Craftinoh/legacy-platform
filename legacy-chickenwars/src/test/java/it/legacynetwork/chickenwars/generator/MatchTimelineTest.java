package it.legacynetwork.chickenwars.generator;

import static org.junit.jupiter.api.Assertions.*;
import it.legacynetwork.chickenwars.model.ResourceType;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.bukkit.configuration.file.YamlConfiguration;

class MatchTimelineTest {
    private MatchPhaseDefinition phase(String id,int at){return new MatchPhaseDefinition(id,at,ResourceType.DIAMOND,2,false);}
    @Test void appliesSkippedPhasesInOrderOnce(){MatchTimeline timeline=new MatchTimeline(new MatchPhaseSchedule(Arrays.asList(phase("one",10),phase("two",20))));assertEquals(2,timeline.poll(25).size());assertTrue(timeline.poll(30).isEmpty());}
    @Test void nextPhaseTracksTimer(){MatchTimeline timeline=new MatchTimeline(new MatchPhaseSchedule(Arrays.asList(phase("one",10),phase("two",20))));assertEquals("ONE",timeline.next(0).getId());timeline.poll(10);assertEquals("TWO",timeline.next(10).getId());}
    @Test void reloadDoesNotReapplyCompletedId(){MatchTimeline timeline=new MatchTimeline(new MatchPhaseSchedule(Arrays.asList(phase("one",10))));timeline.poll(10);timeline.reload(new MatchPhaseSchedule(Arrays.asList(phase("one",5),phase("new",8))));assertEquals("NEW",timeline.poll(10).get(0).getId());}
    @Test void rejectsDuplicateTimeAndId(){assertThrows(IllegalArgumentException.class,()->new MatchPhaseSchedule(Arrays.asList(phase("same",10),phase("same",20))));assertThrows(IllegalArgumentException.class,()->new MatchPhaseSchedule(Arrays.asList(phase("one",10),phase("two",10))));}
    @Test void rejectsOutOfOrderTimeline(){assertThrows(IllegalArgumentException.class,()->new MatchPhaseSchedule(Arrays.asList(phase("later",20),phase("earlier",10))));}
    @Test void phaseSoundAndMessageAreValidatedAndLoaded() throws Exception {YamlConfiguration config=new YamlConfiguration();config.loadFromString("phases:\n  ONE:\n    at-seconds: 10\n    resource: DIAMOND\n    tier: 2\n    message-key: phase.custom\n    sound: CLICK\n");MatchPhaseDefinition loaded=MatchPhaseSchedule.fromSection(config.getConfigurationSection("phases")).getPhases().get(0);assertEquals("phase.custom",loaded.getMessageKey());assertEquals("CLICK",loaded.getSound());config.loadFromString("phases:\n  BAD:\n    at-seconds: 1\n    royal-collapse: true\n    sound: FUTURE_SOUND\n");assertThrows(IllegalArgumentException.class,()->MatchPhaseSchedule.fromSection(config.getConfigurationSection("phases")));}
}
