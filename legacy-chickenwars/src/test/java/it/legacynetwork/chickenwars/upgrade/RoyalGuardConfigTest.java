package it.legacynetwork.chickenwars.upgrade;

import it.legacynetwork.chickenwars.effect.EffectAdapter;
import it.legacynetwork.chickenwars.trap.TrapTriggerRequest;
import it.legacynetwork.chickenwars.trap.TrapTriggerResult;
import it.legacynetwork.chickenwars.trap.TrapTriggerService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Royal Guard verificato sul file realmente spedito nel JAR.
 *
 * <p>Questa classe non usa YAML inline: un catalogo scritto nel test
 * dimostrerebbe soltanto che il meccanismo funziona, non che la
 * configurazione distribuita lo attivi davvero. E' esattamente la divergenza
 * che aveva reso Royal Guard un acquisto senza effetto.</p>
 */
class RoyalGuardConfigTest {

    /** Durata base degli effetti di Contro-Offensiva, da upgrades.yml. */
    private static final int COUNTER_OFFENSIVE_BASE_TICKS = 300;

    private static final String ARENA = "a1";
    private static final String OWNER = "red";

    private UpgradeCatalog catalog;
    private TeamUpgradeService upgrades;
    private RecordingEffects effects;

    @BeforeEach
    void setUp() throws Exception {
        catalog = loadShippedCatalog();
        upgrades = new TeamUpgradeService();
        upgrades.setCatalog(catalog);
        effects = new RecordingEffects();
    }

    /**
     * Legge {@code src/main/resources/upgrades.yml}, il file spedito.
     *
     * <p>Gli effetti pozione sono risolti dal registro popolato dal server:
     * in una JVM di test va sostituito, altrimenti ogni effetto risulterebbe
     * sconosciuto e le trappole verrebbero scartate dal caricamento.</p>
     */
    private UpgradeCatalog loadShippedCatalog() throws Exception {
        File file = new File("src/main/resources/upgrades.yml");
        assertTrue(file.isFile(),
                "file spedito mancante: " + file.getAbsolutePath());
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(new String(
                Files.readAllBytes(file.toPath()), Charset.forName("UTF-8")));

        // I mock vanno creati prima di aprire quello statico: costruirli
        // dentro thenReturn(...) anniderebbe due stubbing e Mockito
        // rifiuterebbe l'operazione.
        Map<String, PotionEffectType> known =
                new LinkedHashMap<String, PotionEffectType>();
        for (String name : Arrays.asList("FAST_DIGGING", "REGENERATION",
                "SPEED", "JUMP", "BLINDNESS", "SLOW", "SLOW_DIGGING")) {
            known.put(name, effectType(name));
        }

        try (MockedStatic<PotionEffectType> types =
                     mockStatic(PotionEffectType.class)) {
            for (Map.Entry<String, PotionEffectType> entry : known.entrySet()) {
                final String name = entry.getKey();
                final PotionEffectType type = entry.getValue();
                types.when(() -> PotionEffectType.getByName(name))
                        .thenReturn(type);
            }
            return UpgradeConfigLoader.load(configuration);
        }
    }

    private PotionEffectType effectType(String name) {
        PotionEffectType type = mock(PotionEffectType.class);
        when(type.getName()).thenReturn(name);
        return type;
    }

    private TrapTriggerResult triggerCounterOffensive(int guardLevel,
                                                      List<UUID> defenders) {
        TeamUpgradeState state = upgrades.getState(ARENA, OWNER);
        state.queueTrap("counter_offensive", catalog.getMaximumTraps());
        for (int level = 0; level < guardLevel; level++) {
            state.reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_GUARD,
                    catalog.getRoyalUpgrade(RoyalUpgradeType.ROYAL_GUARD)
                            .getMaximumLevel());
        }
        return new TrapTriggerService(upgrades, effects).trigger(
                TrapTriggerRequest.builder()
                        .base(ARENA, OWNER)
                        .intruder(UUID.randomUUID(), "blue")
                        .defenders(defenders)
                        .build());
    }

    // ------------------------------------------------------------------
    // Configurazione spedita
    // ------------------------------------------------------------------

    @Test
    void royalGuardEsisteNelFileSpedito() {
        RoyalUpgradeDefinition guard =
                catalog.getRoyalUpgrade(RoyalUpgradeType.ROYAL_GUARD);

        assertNotNull(guard, "ROYAL_GUARD assente da upgrades.yml");
        assertEquals(2, guard.getMaximumLevel());
    }

    @Test
    void entrambiILivelliDichiaranoUnaDurataPositiva() {
        RoyalUpgradeDefinition guard =
                catalog.getRoyalUpgrade(RoyalUpgradeType.ROYAL_GUARD);

        // Senza durata il bonus sarebbe zero e l'upgrade non farebbe nulla.
        assertTrue(guard.getLevel(1).getDurationTicks() > 0,
                "Guard I senza duration-ticks");
        assertTrue(guard.getLevel(2).getDurationTicks() > 0,
                "Guard II senza duration-ticks");
    }

    @Test
    void guardPrimoLivelloVale40Tick() {
        assertEquals(40, catalog.getRoyalUpgrade(RoyalUpgradeType.ROYAL_GUARD)
                .getCumulativeDurationTicks(1));
    }

    @Test
    void guardSecondoLivelloVale80TickCumulativi() {
        assertEquals(80, catalog.getRoyalUpgrade(RoyalUpgradeType.ROYAL_GUARD)
                .getCumulativeDurationTicks(2));
    }

    @Test
    void laDurataCresceInModoProgressivo() {
        RoyalUpgradeDefinition guard =
                catalog.getRoyalUpgrade(RoyalUpgradeType.ROYAL_GUARD);

        assertEquals(0, guard.getCumulativeDurationTicks(0));
        assertTrue(guard.getCumulativeDurationTicks(2)
                > guard.getCumulativeDurationTicks(1));
    }

    // ------------------------------------------------------------------
    // Effetto reale sulla Contro-Offensiva
    // ------------------------------------------------------------------

    @Test
    void senzaGuardLaControOffensivaUsaLaDurataBase() {
        TrapTriggerResult result = triggerCounterOffensive(0,
                Arrays.asList(UUID.randomUUID()));

        assertEquals(TrapTriggerResult.Type.TRIGGERED, result.getType());
        assertEquals(COUNTER_OFFENSIVE_BASE_TICKS,
                result.getDefenderDurationTicks());
    }

    @Test
    void guardIAllungaLaControOffensivaDi40Tick() {
        UUID defender = UUID.randomUUID();

        TrapTriggerResult result =
                triggerCounterOffensive(1, Arrays.asList(defender));

        assertEquals(TrapTriggerResult.Type.TRIGGERED, result.getType());
        assertEquals(COUNTER_OFFENSIVE_BASE_TICKS + 40,
                result.getDefenderDurationTicks());
        // Il bonus deve arrivare all'adapter, non restare nel risultato.
        assertEquals(2, effects.applicationsFor(defender));
        assertTrue(effects.durationsFor(defender).contains(
                Integer.valueOf(COUNTER_OFFENSIVE_BASE_TICKS + 40)));
    }

    @Test
    void guardIIAllungaLaControOffensivaDi80Tick() {
        UUID defender = UUID.randomUUID();

        TrapTriggerResult result =
                triggerCounterOffensive(2, Arrays.asList(defender));

        assertEquals(TrapTriggerResult.Type.TRIGGERED, result.getType());
        assertEquals(COUNTER_OFFENSIVE_BASE_TICKS + 80,
                result.getDefenderDurationTicks());
        assertTrue(effects.durationsFor(defender).contains(
                Integer.valueOf(COUNTER_OFFENSIVE_BASE_TICKS + 80)));
    }

    @Test
    void ilBonusRaggiungeTuttiIDifensoriPresenti() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        TrapTriggerResult result =
                triggerCounterOffensive(1, Arrays.asList(first, second));

        assertEquals(2, result.getAffectedDefenders().size());
        assertTrue(effects.durationsFor(first).contains(
                Integer.valueOf(COUNTER_OFFENSIVE_BASE_TICKS + 40)));
        assertTrue(effects.durationsFor(second).contains(
                Integer.valueOf(COUNTER_OFFENSIVE_BASE_TICKS + 40)));
    }

    @Test
    void ilBonusLettoDalCatalogoCoincideConQuelloDelServizio() {
        TeamUpgradeState state = upgrades.getState(ARENA, OWNER);
        state.reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_GUARD, 2);
        TrapTriggerService traps = new TrapTriggerService(upgrades, effects);

        assertEquals(40, traps.guardBonusTicks(ARENA, OWNER));

        state.reserveNextRoyalLevel(RoyalUpgradeType.ROYAL_GUARD, 2);
        assertEquals(80, traps.guardBonusTicks(ARENA, OWNER));
    }

    /**
     * Adapter che registra durate e destinatari senza toccare il server.
     */
    private static final class RecordingEffects implements EffectAdapter {

        private final List<UUID> targets = new ArrayList<UUID>();
        private final List<Integer> durations = new ArrayList<Integer>();

        @Override
        public boolean apply(UUID playerId, PotionEffectType type,
                             int durationTicks, int amplifier) {
            targets.add(playerId);
            durations.add(Integer.valueOf(durationTicks));
            return true;
        }

        @Override
        public boolean clear(UUID playerId, PotionEffectType type) {
            return false;
        }

        int applicationsFor(UUID playerId) {
            int count = 0;
            for (UUID target : targets) {
                if (target.equals(playerId)) {
                    count++;
                }
            }
            return count;
        }

        List<Integer> durationsFor(UUID playerId) {
            List<Integer> found = new ArrayList<Integer>();
            for (int index = 0; index < targets.size(); index++) {
                if (targets.get(index).equals(playerId)) {
                    found.add(durations.get(index));
                }
            }
            return found;
        }
    }
}
