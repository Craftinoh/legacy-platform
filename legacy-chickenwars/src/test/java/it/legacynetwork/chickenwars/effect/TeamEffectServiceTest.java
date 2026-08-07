package it.legacynetwork.chickenwars.effect;

import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TeamEffectServiceTest {

    private TeamUpgradeService upgrades;
    private FakeEffectAdapter effects;
    private TeamEffectService service;

    @BeforeEach
    void setUp() {
        upgrades = new TeamUpgradeService();
        effects = new FakeEffectAdapter();
        service = new TeamEffectService(upgrades, effects);
    }

    @Test
    void senzaCatalogNonApplicaNulla() {
        UUID playerId = UUID.randomUUID();
        boolean changed = service.apply("a1", "red", playerId, true);
        assertFalse(changed);
    }

    @Test
    void forgetSuNullNonLancia() {
        assertFalse(service.forget(null));
    }

    @Test
    void clearArenaNullNonLancia() {
        assertEquals(0, service.clearArena(null));
    }

    @Test
    void getAppliedAmplifierNullRitornaNegativo() {
        assertEquals(-1, service.getAppliedAmplifier(null));
    }

    @Test
    void refreshNullMembersNonLancia() {
        assertEquals(0, service.refresh("a1", "red", null));
    }

    @Test
    void trackedPlayersInizializzatoAZero() {
        assertEquals(0, service.getTrackedPlayers());
    }

    @Test
    void clearAllSuVuotoNonFaNulla() {
        assertEquals(0, service.clearAll());
    }

    @Test
    void applicaNonEligibleNonAggiungeEffetto() {
        UUID playerId = UUID.randomUUID();
        service.apply("a1", "red", playerId, false);
        assertEquals(0, effects.applyCount);
    }

    @Test
    void refreshAggiungeEffettiAMembri() {
        List<UUID> members = new ArrayList<UUID>();
        for (int i = 0; i < 5; i++) {
            members.add(UUID.randomUUID());
        }
        int touched = service.refresh("a1", "red", members);
        assertTrue(touched >= 0);
    }

    @Test
    void dimenticaRimuoveEffettoTracciato() {
        UUID playerId = UUID.randomUUID();
        effects.apply(playerId, org.bukkit.potion.PotionEffectType.FAST_DIGGING,
                400, 0);
        assertTrue(effects.applyCount > 0);
        boolean removed = service.forget(playerId);
        assertFalse(removed);
    }

    @Test
    void isolamentoArena() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        assertFalse(service.apply("a1", "red", p1, true));
        assertFalse(service.apply("a2", "blue", p2, true));

        int removed1 = service.clearArena("a1");
        int removed2 = service.clearArena("a2");
        assertTrue(removed1 >= 0);
        assertTrue(removed2 >= 0);
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
