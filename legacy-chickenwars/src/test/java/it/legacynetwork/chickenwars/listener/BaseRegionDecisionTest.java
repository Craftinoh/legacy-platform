package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.chicken.RoyalChickenDamageService;
import it.legacynetwork.chickenwars.chicken.RoyalChickenRegistry;
import it.legacynetwork.chickenwars.chicken.RoyalDefeatDispatcher;
import it.legacynetwork.chickenwars.chicken.RoyalUpgradeApplier;
import it.legacynetwork.chickenwars.effect.EffectAdapter;
import it.legacynetwork.chickenwars.effect.HealPoolService;
import it.legacynetwork.chickenwars.effect.TeamEffectService;
import it.legacynetwork.chickenwars.trap.BaseEntryTracker;
import it.legacynetwork.chickenwars.trap.TrapTriggerResult;
import it.legacynetwork.chickenwars.trap.TrapTriggerService;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BaseRegionDecisionTest {

    private BaseEntryTracker tracker;
    private FakeEffectAdapter effects;
    private TeamUpgradeService upgrades;
    private HealPoolService healPool;
    private TeamEffectService teamEffects;
    private TrapTriggerService traps;
    private RoyalChickenRegistry registry;
    private RoyalChickenDamageService royalDamage;
    private RoyalUpgradeApplier royalApplier;
    private RoyalDefeatDispatcher royalDefeatDispatcher;

    @BeforeEach
    void setUp() {
        tracker = new BaseEntryTracker();
        effects = new FakeEffectAdapter();
        upgrades = new TeamUpgradeService();
        healPool = new HealPoolService(upgrades, effects);
        teamEffects = new TeamEffectService(upgrades, effects);
        traps = new TrapTriggerService(upgrades, effects);
        registry = new RoyalChickenRegistry();
        royalDamage = new RoyalChickenDamageService();
        royalApplier = new RoyalUpgradeApplier(upgrades);
        royalDefeatDispatcher = new RoyalDefeatDispatcher();
    }

    @Test
    void hasChangedBlockStessoBloccoIgnorato() {
        Location a = new Location(null, 0, 65, 0);
        Location b = new Location(null, 0.9, 65.5, 0.2);
        assertFalse(hasChangedBlock(a, b));
    }

    @Test
    void hasChangedBlockCambioBloccoRilevato() {
        Location a = new Location(null, 0, 65, 0);
        Location b = new Location(null, 1, 65, 0);
        assertTrue(hasChangedBlock(a, b));
    }

    @Test
    void hasChangedBlockFromNullRestituisceTrue() {
        assertTrue(hasChangedBlock(null, new Location(null, 0, 0, 0)));
    }

    @Test
    void hasChangedBlockToNullRestituisceTrue() {
        assertTrue(hasChangedBlock(new Location(null, 0, 0, 0), null));
    }

    @Test
    void trackerIngressoFuoriDentro() {
        UUID playerId = UUID.randomUUID();
        boolean entry = tracker.update(playerId, "a1", "red", true);
        assertTrue(entry);
    }

    @Test
    void trackerDentroDentroIgnorato() {
        UUID playerId = UUID.randomUUID();
        tracker.update(playerId, "a1", "red", true);
        boolean entry = tracker.update(playerId, "a1", "red", true);
        assertFalse(entry);
    }

    @Test
    void trackerUscitaERientroNuovoFronte() {
        UUID playerId = UUID.randomUUID();
        tracker.update(playerId, "a1", "red", true);
        tracker.update(playerId, "a1", "red", false);
        boolean entry = tracker.update(playerId, "a1", "red", true);
        assertTrue(entry);
    }

    @Test
    void trackerDueRegioniIsolate() {
        UUID playerId = UUID.randomUUID();
        boolean e1 = tracker.update(playerId, "a1", "red", true);
        boolean e2 = tracker.update(playerId, "a1", "blue", true);
        assertTrue(e1);
        assertTrue(e2);
    }

    @Test
    void healPoolSenzaCatalogNonApplica() {
        UUID playerId = UUID.randomUUID();
        boolean active = healPool.update("a1", "red", playerId, true, true);
        assertFalse(active);
    }

    @Test
    void healPoolFuoriBaseNessunEffetto() {
        UUID playerId = UUID.randomUUID();
        boolean active = healPool.update("a1", "red", playerId, false, true);
        assertFalse(active);
    }

    @Test
    void healPoolNonEligibleNessunEffetto() {
        UUID playerId = UUID.randomUUID();
        boolean active = healPool.update("a1", "red", playerId, true, false);
        assertFalse(active);
    }

    @Test
    void healPoolForgetRimuoveTracciamento() {
        UUID playerId = UUID.randomUUID();
        healPool.update("a1", "red", playerId, true, true);
        healPool.forget(playerId);
        assertFalse(healPool.isActive(playerId));
    }

    @Test
    void teamEffectsSenzaCatalogNonApplica() {
        UUID playerId = UUID.randomUUID();
        boolean changed = teamEffects.apply("a1", "red", playerId, true);
        assertFalse(changed);
    }

    @Test
    void teamEffectsNonEligible() {
        UUID playerId = UUID.randomUUID();
        boolean changed = teamEffects.apply("a1", "red", playerId, false);
        assertFalse(changed);
    }

    @Test
    void teamEffectsForget() {
        UUID playerId = UUID.randomUUID();
        assertFalse(teamEffects.forget(playerId));
    }

    @Test
    void serviziCreatiCorrettamente() {
        assertNotNull(tracker);
        assertNotNull(effects);
        assertNotNull(healPool);
        assertNotNull(teamEffects);
        assertNotNull(traps);
        assertNotNull(registry);
        assertNotNull(royalDamage);
        assertNotNull(royalApplier);
        assertNotNull(royalDefeatDispatcher);
    }

    @Test
    void registryOperazioniBase() {
        UUID entityId = UUID.randomUUID();
        assertNotNull(registry.register(entityId, "a1", "red"));
        assertTrue(registry.isRoyalChicken(entityId));
        assertNotNull(registry.lookup(entityId));
        assertEquals("a1", registry.lookup(entityId).getArenaId());
        assertEquals("red", registry.lookup(entityId).getTeamId());

        registry.unregister(entityId);
        assertNull(registry.lookup(entityId));
        assertFalse(registry.isRoyalChicken(entityId));
    }

    @Test
    void registryDueAreeConStessoTeamId() {
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();

        assertNotNull(registry.register(e1, "a1", "red"));
        assertNotNull(registry.register(e2, "a2", "red"));

        assertEquals("a1", registry.lookup(e1).getArenaId());
        assertEquals("a2", registry.lookup(e2).getArenaId());
    }

    @Test
    void trapTriggerNullRequest() {
        TrapTriggerResult result = traps.trigger(null);
        assertEquals(TrapTriggerResult.Type.NOT_ELIGIBLE, result.getType());
    }

    @Test
    void teleportAttivaValutazione() {
        Location a = new Location(null, 0, 65, 0);
        Location b = new Location(null, 100, 70, 100);
        assertTrue(hasChangedBlock(a, b));
    }

    static boolean hasChangedBlock(Location from, Location to) {
        if (from == null || to == null) {
            return true;
        }
        if (from.getWorld() != to.getWorld()) {
            return true;
        }
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    private static class FakeEffectAdapter implements EffectAdapter {
        @Override
        public boolean apply(UUID playerId,
                             org.bukkit.potion.PotionEffectType type,
                             int durationTicks, int amplifier) {
            return true;
        }

        @Override
        public boolean clear(UUID playerId,
                             org.bukkit.potion.PotionEffectType type) {
            return true;
        }
    }
}
