package it.legacynetwork.chickenwars.trap;

import it.legacynetwork.chickenwars.effect.EffectAdapter;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TrapTriggerServiceTest {

    private TeamUpgradeService upgrades;
    private FakeEffectAdapter effects;
    private TrapTriggerService service;

    @BeforeEach
    void setUp() {
        upgrades = new TeamUpgradeService();
        effects = new FakeEffectAdapter();
        service = new TrapTriggerService(upgrades, effects);
    }

    @Test
    void nullRequestRitornaNotEligible() {
        TrapTriggerResult result = service.trigger(null);
        assertEquals(TrapTriggerResult.Type.NOT_ELIGIBLE, result.getType());
    }

    @Test
    void gameNotRunningRitornaNotEligible() {
        TrapTriggerRequest req = TrapTriggerRequest.builder()
                .base("a1", "red")
                .intruder(UUID.randomUUID(), "blue")
                .gameRunning(false)
                .intruderEligible(true)
                .build();

        TrapTriggerResult result = service.trigger(req);
        assertEquals(TrapTriggerResult.Type.NOT_ELIGIBLE, result.getType());
    }

    @Test
    void friendlyIntruderRitornaNotEligible() {
        TrapTriggerRequest req = TrapTriggerRequest.builder()
                .base("a1", "red")
                .intruder(UUID.randomUUID(), "red")
                .gameRunning(true)
                .intruderEligible(true)
                .build();

        TrapTriggerResult result = service.trigger(req);
        assertEquals(TrapTriggerResult.Type.NOT_ELIGIBLE, result.getType());
    }

    @Test
    void intruderNonEligibleRitornaNotEligible() {
        TrapTriggerRequest req = TrapTriggerRequest.builder()
                .base("a1", "red")
                .intruder(UUID.randomUUID(), "blue")
                .gameRunning(true)
                .intruderEligible(false)
                .build();

        TrapTriggerResult result = service.trigger(req);
        assertEquals(TrapTriggerResult.Type.NOT_ELIGIBLE, result.getType());
    }

    @Test
    void codaVuotaRitornaNoTrap() {
        TrapTriggerRequest req = TrapTriggerRequest.builder()
                .base("a1", "red")
                .intruder(UUID.randomUUID(), "blue")
                .gameRunning(true)
                .intruderEligible(true)
                .build();

        TrapTriggerResult result = service.trigger(req);
        assertEquals(TrapTriggerResult.Type.NO_TRAP, result.getType());
    }

    @Test
    void nullArenaRitornaNotEligible() {
        TrapTriggerRequest req = TrapTriggerRequest.builder()
                .base(null, "red")
                .intruder(UUID.randomUUID(), "blue")
                .gameRunning(true)
                .intruderEligible(true)
                .build();

        TrapTriggerResult result = service.trigger(req);
        assertEquals(TrapTriggerResult.Type.NOT_ELIGIBLE, result.getType());
    }

    @Test
    void nullTeamRitornaNotEligible() {
        TrapTriggerRequest req = TrapTriggerRequest.builder()
                .base("a1", null)
                .intruder(UUID.randomUUID(), "blue")
                .gameRunning(true)
                .intruderEligible(true)
                .build();

        TrapTriggerResult result = service.trigger(req);
        assertEquals(TrapTriggerResult.Type.NOT_ELIGIBLE, result.getType());
    }

    @Test
    void guardbonusSenzaCatalogRitornaZero() {
        int bonus = service.guardBonusTicks("a1", "red");
        assertEquals(0, bonus);
    }

    @Test
    void defendersListBuiltCorrectly() {
        UUID defender = UUID.randomUUID();
        TrapTriggerRequest req = TrapTriggerRequest.builder()
                .base("a1", "red")
                .intruder(UUID.randomUUID(), "blue")
                .gameRunning(true)
                .intruderEligible(true)
                .defenders(Arrays.asList(defender))
                .build();

        assertEquals(1, req.getDefenders().size());
        assertTrue(req.getDefenders().contains(defender));
    }

    @Test
    void trapRequestBuilderDefaultValues() {
        TrapTriggerRequest req = TrapTriggerRequest.builder()
                .base("a1", "red")
                .intruder(UUID.randomUUID(), "blue")
                .build();

        assertTrue(req.isGameRunning());
        assertTrue(req.isIntruderEligible());
        assertTrue(req.getDefenders().isEmpty());
    }

    private static class FakeEffectAdapter implements EffectAdapter {
        final Map<String, Integer> counts = new HashMap<String, Integer>();

        @Override
        public boolean apply(UUID playerId, PotionEffectType type,
                             int durationTicks, int amplifier) {
            String key = playerId + "/" + type.getName();
            counts.put(key, counts.getOrDefault(key, 0) + 1);
            return true;
        }

        @Override
        public boolean clear(UUID playerId, PotionEffectType type) {
            return true;
        }
    }
}
