package it.legacynetwork.chickenwars.effect;

import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HealPoolServiceTest {

    private TeamUpgradeService upgrades;
    private FakeEffectAdapter effects;
    private HealPoolService service;

    @BeforeEach
    void setUp() {
        upgrades = new TeamUpgradeService();
        effects = new FakeEffectAdapter();
        service = new HealPoolService(upgrades, effects);
    }

    @Test
    void senzaCatalogNonApplicaNulla() {
        UUID playerId = UUID.randomUUID();
        boolean active = service.update("a1", "red", playerId, true, true);
        assertFalse(active);
    }

    @Test
    void nonEligibleNonApplica() {
        UUID playerId = UUID.randomUUID();
        boolean active = service.update("a1", "red", playerId, true, false);
        assertFalse(active);
    }

    @Test
    void fuoriBaseNonApplica() {
        UUID playerId = UUID.randomUUID();
        boolean active = service.update("a1", "red", playerId, false, true);
        assertFalse(active);
    }

    @Test
    void nullPlayerNonApplica() {
        boolean active = service.update("a1", "red", null, true, true);
        assertFalse(active);
    }

    @Test
    void isActiveSuNullRitornaFalse() {
        assertFalse(service.isActive(null));
    }

    @Test
    void forgetSuNullRitornaFalse() {
        assertFalse(service.forget(null));
    }

    @Test
    void clearArenaNullNonLancia() {
        assertEquals(0, service.clearArena(null));
    }

    @Test
    void clearAllSuVuotoNonFaNulla() {
        assertEquals(0, service.clearAll());
    }

    @Test
    void trackedPlayersInizializzatoAZero() {
        assertEquals(0, service.getTrackedPlayers());
    }

    @Test
    void isActiveSuIdSconosciutoRitornaFalse() {
        assertFalse(service.isActive(UUID.randomUUID()));
    }

    @Test
    void forgetSuIdSconosciutoRitornaFalse() {
        assertFalse(service.forget(UUID.randomUUID()));
    }

    private static class FakeEffectAdapter implements EffectAdapter {
        int applyCount;

        @Override
        public boolean apply(UUID playerId, org.bukkit.potion.PotionEffectType type,
                             int durationTicks, int amplifier) {
            applyCount++;
            return true;
        }

        @Override
        public boolean clear(UUID playerId, org.bukkit.potion.PotionEffectType type) {
            return true;
        }
    }
}
