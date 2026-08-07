package it.legacynetwork.chickenwars.effect;

import it.legacynetwork.chickenwars.trap.TrapTriggerRequest;
import it.legacynetwork.chickenwars.trap.TrapTriggerResult;
import it.legacynetwork.chickenwars.trap.TrapTriggerService;
import it.legacynetwork.chickenwars.upgrade.RoyalUpgradeType;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeState;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeType;
import it.legacynetwork.chickenwars.upgrade.UpgradeConfigLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Opus2RuntimeBehaviorTest {

    private TeamUpgradeService upgrades;
    private RecordingEffects effects;
    private PotionEffectType haste;
    private PotionEffectType regeneration;
    private PotionEffectType blindness;
    private PotionEffectType slowness;
    private PotionEffectType fatigue;
    private PotionEffectType speed;
    private PotionEffectType jump;

    @BeforeEach
    void setUp() throws Exception {
        upgrades = new TeamUpgradeService();
        haste = effect("FAST_DIGGING");
        regeneration = effect("REGENERATION");
        blindness = effect("BLINDNESS");
        slowness = effect("SLOW");
        fatigue = effect("SLOW_DIGGING");
        speed = effect("SPEED");
        jump = effect("JUMP");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(
                "team-upgrades:\n"
                        + "  HASTE:\n"
                        + "    icon: GOLD_PICKAXE\n"
                        + "    effect: FAST_DIGGING\n"
                        + "    levels:\n"
                        + "      1:\n"
                        + "        amplifier: 0\n"
                        + "        duration-ticks: 400\n"
                        + "        cost:\n"
                        + "          solo_duel: {currency: DIAMOND, amount: 1}\n"
                        + "  HEAL_POOL:\n"
                        + "    icon: BEACON\n"
                        + "    effect: REGENERATION\n"
                        + "    levels:\n"
                        + "      1:\n"
                        + "        amplifier: 0\n"
                        + "        duration-ticks: 100\n"
                        + "        cost:\n"
                        + "          solo_duel: {currency: DIAMOND, amount: 1}\n"
                        + "royal-upgrades:\n"
                        + "  ROYAL_GUARD:\n"
                        + "    icon: IRON_FENCE\n"
                        + "    levels:\n"
                        + "      1:\n"
                        + "        duration-ticks: 40\n"
                        + "        cost:\n"
                        + "          solo_duel: {currency: DIAMOND, amount: 1}\n"
                        + "traps:\n"
                        + "  maximum: 3\n"
                        + "  position-costs:\n"
                        + "    1:\n"
                        + "      solo_duel: {currency: DIAMOND, amount: 1}\n"
                        + "    2:\n"
                        + "      solo_duel: {currency: DIAMOND, amount: 2}\n"
                        + "    3:\n"
                        + "      solo_duel: {currency: DIAMOND, amount: 4}\n"
                        + "  types:\n"
                        + "    intruder:\n"
                        + "      icon: LEATHER_BOOTS\n"
                        + "      intruder-effects: [\"BLINDNESS:8:0\", \"SLOW:8:0\"]\n"
                        + "    miner_fatigue:\n"
                        + "      icon: IRON_PICKAXE\n"
                        + "      intruder-effects: [\"SLOW_DIGGING:10:0\"]\n"
                        + "    counter_offensive:\n"
                        + "      icon: FEATHER\n"
                        + "      defender-effects: [\"SPEED:15:1\", \"JUMP:15:1\"]\n"
                        + "    alarm:\n"
                        + "      icon: REDSTONE_TORCH_ON\n"
                        + "      reveals-invisibility: true\n");
        try (MockedStatic<PotionEffectType> types =
                     mockStatic(PotionEffectType.class)) {
            types.when(() -> PotionEffectType.getByName("FAST_DIGGING"))
                    .thenReturn(haste);
            types.when(() -> PotionEffectType.getByName("REGENERATION"))
                    .thenReturn(regeneration);
            types.when(() -> PotionEffectType.getByName("BLINDNESS"))
                    .thenReturn(blindness);
            types.when(() -> PotionEffectType.getByName("SLOW"))
                    .thenReturn(slowness);
            types.when(() -> PotionEffectType.getByName("SLOW_DIGGING"))
                    .thenReturn(fatigue);
            types.when(() -> PotionEffectType.getByName("SPEED"))
                    .thenReturn(speed);
            types.when(() -> PotionEffectType.getByName("JUMP"))
                    .thenReturn(jump);
            upgrades.setCatalog(UpgradeConfigLoader.load(yaml));
        }
        effects = new RecordingEffects();
    }

    @Test
    void hasteSiApplicaSubitoMaNonSiDuplica() {
        UUID player = UUID.randomUUID();
        upgrades.getState("a1", "red")
                .reserveNextLevel(TeamUpgradeType.HASTE, 1);
        TeamEffectService service = new TeamEffectService(upgrades, effects);

        assertTrue(service.apply("a1", "red", player, true));
        assertFalse(service.apply("a1", "red", player, true));
        assertEquals(1, effects.applied(player, haste));
    }

    @Test
    void hasteVieneRimossoQuandoIlGiocatoreNonEPiuIdoneo() {
        UUID player = UUID.randomUUID();
        upgrades.getState("a1", "red")
                .reserveNextLevel(TeamUpgradeType.HASTE, 1);
        TeamEffectService service = new TeamEffectService(upgrades, effects);
        service.apply("a1", "red", player, true);

        assertTrue(service.apply("a1", "red", player, false));
        assertEquals(1, effects.cleared(player, haste));
    }

    @Test
    void healPoolSegueIngressoUscitaESoloIlProprioMarcatore() {
        UUID ally = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        upgrades.getState("a1", "red")
                .reserveNextLevel(TeamUpgradeType.HEAL_POOL, 1);
        HealPoolService service = new HealPoolService(upgrades, effects);

        assertTrue(service.update("a1", "red", ally, true, true));
        assertFalse(service.update("a1", "red", outsider, true, false));
        assertFalse(service.update("a1", "red", ally, false, true));
        assertEquals(1, effects.applied(ally, regeneration));
        assertEquals(1, effects.cleared(ally, regeneration));
        assertEquals(0, effects.cleared(outsider, regeneration));
    }

    @Test
    void trappoleSonoFifoEConsumateUnaVoltaPerIntruso() {
        TeamUpgradeState state = upgrades.getState("a1", "red");
        state.queueTrap("intruder", 3);
        state.queueTrap("miner_fatigue", 3);
        TrapTriggerService service = new TrapTriggerService(upgrades, effects);

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        TrapTriggerResult firstResult = service.trigger(request(first));
        TrapTriggerResult secondResult = service.trigger(request(second));

        assertEquals("intruder", firstResult.getTrapId());
        assertEquals("miner_fatigue", secondResult.getTrapId());
        assertEquals(0, state.getTrapCount());
        assertEquals(1, effects.applied(first, blindness));
        assertEquals(1, effects.applied(first, slowness));
        assertEquals(1, effects.applied(second, fatigue));
    }

    @Test
    void fallimentoTotaleRimetteLaTrappolaInTesta() {
        TeamUpgradeState state = upgrades.getState("a1", "red");
        state.queueTrap("intruder", 3);
        effects.acceptApplications = false;

        TrapTriggerResult result = new TrapTriggerService(upgrades, effects)
                .trigger(request(UUID.randomUUID()));

        assertEquals(TrapTriggerResult.Type.FAILED, result.getType());
        assertEquals("intruder", state.peekTrap());
        assertEquals(1, state.getTrapCount());
    }

    @Test
    void counterOffensiveRiceveIlBonusRoyalGuard() {
        UUID defenderOne = UUID.randomUUID();
        UUID defenderTwo = UUID.randomUUID();
        TeamUpgradeState state = upgrades.getState("a1", "red");
        state.queueTrap("counter_offensive", 3);
        state.reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_GUARD, 1);
        TrapTriggerRequest request = TrapTriggerRequest.builder()
                .base("a1", "red")
                .intruder(UUID.randomUUID(), "blue")
                .defenders(Arrays.asList(defenderOne, defenderTwo))
                .build();

        TrapTriggerResult result = new TrapTriggerService(upgrades, effects)
                .trigger(request);

        assertEquals(TrapTriggerResult.Type.TRIGGERED, result.getType());
        assertEquals(340, result.getDefenderDurationTicks());
        assertEquals(1, effects.applied(defenderOne, speed));
        assertEquals(1, effects.applied(defenderOne, jump));
        assertEquals(1, effects.applied(defenderTwo, speed));
        assertEquals(1, effects.applied(defenderTwo, jump));
    }

    @Test
    void alarmSiConsumaENotificaAncheSenzaInvisibilita() {
        TeamUpgradeState state = upgrades.getState("a1", "red");
        state.queueTrap("alarm", 3);

        TrapTriggerResult result = new TrapTriggerService(upgrades, effects)
                .trigger(request(UUID.randomUUID()));

        assertEquals(TrapTriggerResult.Type.TRIGGERED, result.getType());
        assertEquals(0, state.getTrapCount());
    }

    private TrapTriggerRequest request(UUID intruder) {
        return TrapTriggerRequest.builder()
                .base("a1", "red")
                .intruder(intruder, "blue")
                .gameRunning(true)
                .intruderEligible(true)
                .build();
    }

    private PotionEffectType effect(String name) {
        PotionEffectType type = mock(PotionEffectType.class);
        when(type.getName()).thenReturn(name);
        return type;
    }

    private static final class RecordingEffects implements EffectAdapter {

        private final Map<String, Integer> applications =
                new LinkedHashMap<String, Integer>();
        private final Map<String, Integer> clears =
                new LinkedHashMap<String, Integer>();
        private boolean acceptApplications = true;

        @Override
        public boolean apply(UUID playerId, PotionEffectType type,
                             int durationTicks, int amplifier) {
            increment(applications, key(playerId, type));
            return acceptApplications;
        }

        @Override
        public boolean clear(UUID playerId, PotionEffectType type) {
            increment(clears, key(playerId, type));
            return true;
        }

        int applied(UUID playerId, PotionEffectType type) {
            return count(applications, key(playerId, type));
        }

        int cleared(UUID playerId, PotionEffectType type) {
            return count(clears, key(playerId, type));
        }

        private static String key(UUID playerId, PotionEffectType type) {
            return playerId + "/" + System.identityHashCode(type);
        }

        private static void increment(Map<String, Integer> values, String key) {
            values.put(key, Integer.valueOf(count(values, key) + 1));
        }

        private static int count(Map<String, Integer> values, String key) {
            Integer value = values.get(key);
            return value == null ? 0 : value.intValue();
        }
    }
}
