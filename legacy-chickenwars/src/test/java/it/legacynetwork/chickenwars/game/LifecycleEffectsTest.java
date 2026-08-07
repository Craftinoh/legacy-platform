package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.effect.EffectAdapter;
import it.legacynetwork.chickenwars.effect.HealPoolService;
import it.legacynetwork.chickenwars.effect.TeamEffectService;
import it.legacynetwork.chickenwars.trap.BaseEntryTracker;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LifecycleEffectsTest {

    private TeamUpgradeService upgrades;
    private FakeEffectAdapter effects;
    private TeamEffectService teamEffects;
    private HealPoolService healPool;
    private BaseEntryTracker tracker;

    @BeforeEach
    void setUp() {
        upgrades = new TeamUpgradeService();
        effects = new FakeEffectAdapter();
        teamEffects = new TeamEffectService(upgrades, effects);
        healPool = new HealPoolService(upgrades, effects);
        tracker = new BaseEntryTracker();
    }

    @Test
    void hasteNonApplicatoSenzaCatalog() {
        UUID playerId = UUID.randomUUID();
        boolean changed = teamEffects.apply("a1", "red", playerId, true);
        assertFalse(changed);
        assertEquals(0, effects.applyCount);
    }

    @Test
    void hasteForgetRimuoveTracciamento() {
        UUID playerId = UUID.randomUUID();
        assertFalse(teamEffects.forget(playerId));
    }

    @Test
    void healPoolForgetRimuoveMarker() {
        UUID playerId = UUID.randomUUID();
        assertFalse(healPool.forget(playerId));
    }

    @Test
    void trackerForgetRimuovePresenza() {
        UUID playerId = UUID.randomUUID();
        tracker.update(playerId, "a1", "red", true);
        assertTrue(tracker.isInside(playerId, "a1", "red"));
        tracker.forget(playerId);
        assertEquals(0, tracker.trackedPlayers());
    }

    @Test
    void clearArenaRimuoveTuttiIServizi() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        assertFalse(teamEffects.apply("a1", "red", p1, true));
        assertFalse(healPool.update("a1", "red", p1, true, true));
        tracker.update(p1, "a1", "red", true);
        tracker.update(p2, "a1", "blue", true);

        teamEffects.clearArena("a1");
        healPool.clearArena("a1");
        int removed = tracker.clearArena("a1");

        assertEquals(0, teamEffects.getTrackedPlayers());
        assertEquals(0, healPool.getTrackedPlayers());
        assertTrue(removed > 0);
    }

    @Test
    void secondaPartitaStessiTeamNessunEreditato() {
        UUID p1 = UUID.randomUUID();

        assertFalse(teamEffects.apply("a1", "red", p1, true));
        assertFalse(healPool.update("a1", "red", p1, true, true));
        tracker.update(p1, "a1", "red", true);

        teamEffects.clearArena("a1");
        healPool.clearArena("a1");
        tracker.clearArena("a1");

        boolean secondAttempt = teamEffects.apply("a1", "red", p1, true);
        assertFalse(secondAttempt);
        assertEquals(0, teamEffects.getTrackedPlayers());
        assertEquals(0, healPool.getTrackedPlayers());
        assertEquals(0, tracker.trackedPlayers());
    }

    @Test
    void clearAllRimuoveTutto() {
        UUID p1 = UUID.randomUUID();
        teamEffects.apply("a1", "red", p1, true);
        healPool.update("a1", "red", p1, true, true);
        tracker.update(p1, "a1", "red", true);

        teamEffects.clearAll();
        healPool.clearAll();
        tracker.clearAll();

        assertEquals(0, teamEffects.getTrackedPlayers());
        assertEquals(0, healPool.getTrackedPlayers());
        assertEquals(0, tracker.trackedPlayers());
    }

    @Test
    void effettiTraAreneDiverseSonoIsolati() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        tracker.update(p1, "a1", "red", true);
        tracker.update(p2, "a2", "blue", true);

        assertEquals(2, tracker.trackedPlayers());
        tracker.clearArena("a1");
        assertEquals(1, tracker.trackedPlayers());
    }

    @Test
    void reconnectNonDuplicaEffetti() {
        UUID playerId = UUID.randomUUID();
        assertFalse(teamEffects.apply("a1", "red", playerId, true));
        int afterFirst = effects.applyCount;
        assertFalse(teamEffects.apply("a1", "red", playerId, true));
        assertEquals(afterFirst, effects.applyCount);
    }

    @Test
    void healPoolRimossoSoloEffettoGestito() {
        UUID playerId = UUID.randomUUID();
        healPool.update("a1", "red", playerId, true, true);

        healPool.forget(playerId);
        assertFalse(healPool.isActive(playerId));
        assertEquals(0, healPool.getTrackedPlayers());
    }

    private static class FakeEffectAdapter implements EffectAdapter {
        int applyCount;

        @Override
        public boolean apply(UUID playerId,
                             org.bukkit.potion.PotionEffectType type,
                             int durationTicks, int amplifier) {
            applyCount++;
            return true;
        }

        @Override
        public boolean clear(UUID playerId,
                             org.bukkit.potion.PotionEffectType type) {
            return true;
        }
    }
}
