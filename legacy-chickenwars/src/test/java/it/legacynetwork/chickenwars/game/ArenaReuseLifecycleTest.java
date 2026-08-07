package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.arena.GeneratorDefinition;
import it.legacynetwork.chickenwars.config.GeneratorTier;
import it.legacynetwork.chickenwars.generator.CatchUpPolicy;
import it.legacynetwork.chickenwars.generator.GeneratedResourceRegistry;
import it.legacynetwork.chickenwars.generator.GeneratorDropSink;
import it.legacynetwork.chickenwars.generator.GeneratorSchedule;
import it.legacynetwork.chickenwars.generator.GeneratorService;
import it.legacynetwork.chickenwars.generator.GeneratorState;
import it.legacynetwork.chickenwars.generator.MatchPhaseDefinition;
import it.legacynetwork.chickenwars.generator.MatchPhaseSchedule;
import it.legacynetwork.chickenwars.generator.MatchTimeline;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaReuseLifecycleTest {

    @Test
    void secondMatchOnSameArenaStartsWithoutFirstMatchRuntimeState() {
        CountingSink sink = new CountingSink();
        GeneratorService generators = new GeneratorService(new FixedSchedule(), sink);
        GeneratedResourceRegistry resources = new GeneratedResourceRegistry();
        MatchPhaseSchedule phases = new MatchPhaseSchedule(Arrays.asList(
                new MatchPhaseDefinition("DIAMOND_II", 5,
                        ResourceType.DIAMOND, 2, false)));
        MatchTimeline firstTimeline = new MatchTimeline(phases);
        UUID oldItem = UUID.randomUUID();

        generators.add(generator("first", "match-1"));
        generators.start(0);
        generators.tick(10);
        firstTimeline.poll(5);
        resources.register(oldItem, "match-1", ResourceType.IRON);

        generators.clear();
        resources.clearMatch("match-1");

        MatchTimeline secondTimeline = new MatchTimeline(phases);
        generators.add(generator("second", "match-2"));
        generators.start(100);

        assertTrue(generators.isRunning());
        assertEquals(1, generators.states().size());
        assertEquals("match-2", generators.states().get(0).getMatchId());
        assertEquals("DIAMOND_II", secondTimeline.next(0).getId());
        assertNull(resources.consume(oldItem, "match-1"));
        assertFalse(firstTimeline.poll(6).size() > 0);
        assertEquals(1, sink.cleanups);
    }

    private GeneratorState generator(String id, String match) {
        return new GeneratorState(match, new GeneratorDefinition(id,
                ResourceType.IRON, new SimpleLocation("arena", 0, 64, 0,
                0, 0), null, 1, false));
    }

    private static final class FixedSchedule implements GeneratorSchedule {
        @Override public GeneratorTier tier(ResourceType type, int level) {
            return new GeneratorTier(10, 1);
        }
        @Override public CatchUpPolicy catchUpPolicy() {
            return CatchUpPolicy.SKIP;
        }
        @Override public int maximumCatchUpDrops() { return 1; }
    }

    private static final class CountingSink implements GeneratorDropSink {
        private int cleanups;
        @Override public boolean drop(GeneratorState state, int amount) {
            return true;
        }
        @Override public void cleanup() { cleanups++; }
    }
}
