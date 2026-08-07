package it.legacynetwork.chickenwars.chicken;

import it.legacynetwork.chickenwars.effect.EffectAdapter;
import it.legacynetwork.chickenwars.upgrade.RoyalUpgradeType;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import it.legacynetwork.chickenwars.upgrade.UpgradeConfigLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RoyalChickenWiringTest {

    private TeamUpgradeService upgrades;
    private RoyalChickenRegistry registry;
    private RoyalChickenDamageService damageService;
    private RoyalUpgradeApplier applier;
    private RoyalDefeatDispatcher dispatcher;
    private RoyalChicken chicken;

    @BeforeEach
    void setUp() throws Exception {
        upgrades = new TeamUpgradeService();
        String yaml = "team-upgrades:\n"
                + "  PROTECTION:\n"
                + "    icon: IRON_CHESTPLATE\n"
                + "    levels:\n"
                + "      1:\n"
                + "        amplifier: 0\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 2}\n"
                + "royal-upgrades:\n"
                + "  ROYAL_ARMOR:\n"
                + "    icon: IRON_CHESTPLATE\n"
                + "    levels:\n"
                + "      1:\n"
                + "        value: 0.25\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 2}\n"
                + "  ROYAL_VITALITY:\n"
                + "    icon: GOLDEN_APPLE\n"
                + "    levels:\n"
                + "      1:\n"
                + "        value: 25.0\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 2}\n";
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        upgrades.setCatalog(UpgradeConfigLoader.load(config));

        registry = new RoyalChickenRegistry();
        damageService = new RoyalChickenDamageService();
        applier = new RoyalUpgradeApplier(upgrades);
        dispatcher = new RoyalDefeatDispatcher();

        ChickenSettings settings = ChickenSettings.fromSection(null);
        chicken = new RoyalChicken("red",
                it.legacynetwork.chickenwars.model.SimpleLocation.parse(
                        "world,0,0,0"), settings);
        chicken.releaseProtection();
    }

    @Test
    void registryRegisterAndLookup() {
        UUID entityId = UUID.randomUUID();
        RoyalChickenRegistry.Entry entry = registry.register(
                entityId, "a1", "red");
        assertNotNull(entry);
        assertTrue(registry.isRoyalChicken(entityId));

        RoyalChickenRegistry.Entry found = registry.lookup(entityId);
        assertNotNull(found);
        assertEquals("a1", found.getArenaId());
        assertEquals("red", found.getTeamId());
    }

    @Test
    void registryUnknownEntity() {
        assertNull(registry.lookup(UUID.randomUUID()));
        assertFalse(registry.isRoyalChicken(UUID.randomUUID()));
    }

    @Test
    void dannoConArmorRidotto() {
        UUID attackerId = UUID.randomUUID();
        upgrades.getState("a1", "red")
                .reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_ARMOR, 1);

        double reduction = applier.resolveArmorReduction("a1", "red");
        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, "blue")
                .owner("red")
                .gameRunning(true)
                .attackerPlaying(true)
                .rawDamage(10.0)
                .damageReduction(reduction)
                .build();

        RoyalDamageResult result = damageService.damage(chicken, request);
        assertFalse(result.isIgnored());
        assertEquals(RoyalDamageResult.Type.DAMAGED, result.getType());
        assertEquals(7.5, result.getAppliedDamage(), 0.01);
    }

    @Test
    void dannoSenzaArmorCompleto() {
        UUID attackerId = UUID.randomUUID();
        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, "blue")
                .owner("red")
                .gameRunning(true)
                .attackerPlaying(true)
                .rawDamage(10.0)
                .damageReduction(0.0)
                .build();

        RoyalDamageResult result = damageService.damage(chicken, request);
        assertFalse(result.isIgnored());
        assertEquals(10.0, result.getAppliedDamage(), 0.01);
    }

    @Test
    void dannoAlleatoIgnorato() {
        UUID attackerId = UUID.randomUUID();
        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, "red")
                .owner("red")
                .gameRunning(true)
                .attackerPlaying(true)
                .rawDamage(10.0)
                .damageReduction(0.0)
                .build();

        RoyalDamageResult result = damageService.damage(chicken, request);
        assertTrue(result.isIgnored());
    }

    @Test
    void dannoNonLetaleNonDispatched() {
        UUID attackerId = UUID.randomUUID();
        AtomicReference<RoyalDefeat> dispatched = new AtomicReference<RoyalDefeat>();
        dispatcher.register(defeat -> dispatched.set(defeat));

        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, "blue")
                .owner("red")
                .gameRunning(true)
                .attackerPlaying(true)
                .rawDamage(5.0)
                .damageReduction(0.0)
                .build();

        RoyalDamageResult result = damageService.damage(chicken, request);
        assertEquals(RoyalDamageResult.Type.DAMAGED, result.getType());
        assertNull(dispatched.get());
    }

    @Test
    void dannoLetaleDispatchedUnaVolta() {
        UUID attackerId = UUID.randomUUID();
        AtomicReference<RoyalDefeat> dispatched = new AtomicReference<RoyalDefeat>();
        dispatcher.register(defeat -> dispatched.set(defeat));

        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, "blue")
                .owner("red")
                .gameRunning(true)
                .attackerPlaying(true)
                .rawDamage(999.0)
                .damageReduction(0.0)
                .build();

        RoyalDamageResult result = damageService.damage(chicken, request);
        assertEquals(RoyalDamageResult.Type.DEFEATED, result.getType());

        boolean marked = chicken.markDefeated();
        assertTrue(marked);

        if (result.isDefeated() && marked) {
            dispatcher.dispatch(new RoyalDefeat("a1", "red",
                    chicken.getEntity() != null ? chicken.getEntity().getUniqueId()
                            : UUID.randomUUID(),
                    attackerId, System.currentTimeMillis()));
        }

        assertNotNull(dispatched.get());
        assertEquals("red", dispatched.get().getTeamId());

        dispatched.set(null);
        boolean second = chicken.markDefeated();
        assertFalse(second);
    }

    @Test
    void doppioEventoLetaleIgnorato() {
        chicken.getVitals().kill();
        assertFalse(chicken.isAlive());

        UUID attackerId = UUID.randomUUID();
        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, "blue")
                .owner("red")
                .gameRunning(true)
                .attackerPlaying(true)
                .rawDamage(1.0)
                .damageReduction(0.0)
                .build();

        RoyalDamageResult result = damageService.damage(chicken, request);
        assertTrue(result.isIgnored());
    }

    @Test
    void dannoAGallinaProtettaIgnorato() {
        RoyalChicken protectedChicken = new RoyalChicken("red",
                it.legacynetwork.chickenwars.model.SimpleLocation.parse(
                        "world,0,0,0"),
                ChickenSettings.fromSection(null));

        UUID attackerId = UUID.randomUUID();
        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, "blue")
                .owner("red")
                .gameRunning(true)
                .attackerPlaying(true)
                .rawDamage(10.0)
                .damageReduction(0.0)
                .build();

        RoyalDamageResult result = damageService.damage(protectedChicken, request);
        assertTrue(result.isIgnored());
    }

    @Test
    void dannoGameNotRunningIgnorato() {
        UUID attackerId = UUID.randomUUID();
        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, "blue")
                .owner("red")
                .gameRunning(false)
                .attackerPlaying(true)
                .rawDamage(10.0)
                .damageReduction(0.0)
                .build();

        RoyalDamageResult result = damageService.damage(chicken, request);
        assertTrue(result.isIgnored());
    }

    @Test
    void dannoZeroIgnorato() {
        UUID attackerId = UUID.randomUUID();
        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, "blue")
                .owner("red")
                .gameRunning(true)
                .attackerPlaying(true)
                .rawDamage(0.0)
                .damageReduction(0.0)
                .build();

        RoyalDamageResult result = damageService.damage(chicken, request);
        assertTrue(result.isIgnored());
    }

    @Test
    void dannoNegativoIgnorato() {
        UUID attackerId = UUID.randomUUID();
        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, "blue")
                .owner("red")
                .gameRunning(true)
                .attackerPlaying(true)
                .rawDamage(-5.0)
                .damageReduction(0.0)
                .build();

        RoyalDamageResult result = damageService.damage(chicken, request);
        assertTrue(result.isIgnored());
    }

    @Test
    void dannoAmbientaleIgnorato() {
        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(null, null)
                .owner("red")
                .gameRunning(true)
                .attackerPlaying(false)
                .rawDamage(10.0)
                .damageReduction(0.0)
                .build();

        RoyalDamageResult result = damageService.damage(chicken, request);
        assertTrue(result.isIgnored());
    }

    @Test
    void vitalityApplicataUnaSolaVolta() {
        double initialMax = chicken.getVitals().getMaxHealth();
        upgrades.getState("a1", "red")
                .reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_VITALITY, 1);

        double added = applier.applyVitality("a1", "red", chicken);
        assertEquals(25.0, added, 0.01);
        assertTrue(chicken.getVitals().getMaxHealth() > initialMax);

        double second = applier.applyVitality("a1", "red", chicken);
        assertEquals(0.0, second, 0.01);
    }

    @Test
    void registryClearArena() {
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        registry.register(e1, "a1", "red");
        registry.register(e2, "a2", "blue");

        assertEquals(2, registry.size());
        registry.clearArena("a1");
        assertEquals(1, registry.size());
        assertNull(registry.lookup(e1));
        assertNotNull(registry.lookup(e2));
    }

    @Test
    void dannoApplicatoRiduceSalute() {
        UUID attackerId = UUID.randomUUID();
        double before = chicken.getVitals().getHealth();

        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, "blue")
                .owner("red")
                .gameRunning(true)
                .attackerPlaying(true)
                .rawDamage(50.0)
                .damageReduction(0.0)
                .build();

        RoyalDamageResult result = damageService.damage(chicken, request);
        assertEquals(RoyalDamageResult.Type.DAMAGED, result.getType());
        assertTrue(chicken.getVitals().getHealth() < before);
    }

    @Test
    void dispatcherClearedNotifiesNone() {
        AtomicReference<RoyalDefeat> dispatched = new AtomicReference<RoyalDefeat>();
        dispatcher.register(defeat -> dispatched.set(defeat));
        dispatcher.clear();

        int notified = dispatcher.dispatch(
                new RoyalDefeat("a1", "red", UUID.randomUUID(), null,
                        System.currentTimeMillis()));
        assertEquals(0, notified);
    }
}
