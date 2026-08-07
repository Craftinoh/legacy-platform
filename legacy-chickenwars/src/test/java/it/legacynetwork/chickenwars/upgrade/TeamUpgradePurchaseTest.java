package it.legacynetwork.chickenwars.upgrade;

import it.legacynetwork.chickenwars.economy.ResourceInventory;
import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.mode.ModeProfile;
import it.legacynetwork.chickenwars.mode.ModeProfileRegistry;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.player.PlayerSession;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acquisto atomico degli upgrade di squadra.
 *
 * <p>Verifica l'invariante economica: le risorse iniziali coincidono sempre con
 * il costo consumato piu' quelle rimaste.</p>
 */
class TeamUpgradePurchaseTest {

    private static final String ARENA = "farm";
    private static final String TEAM = "red";

    private TeamUpgradeService service;
    private ModeProfile solo;
    private ModeProfile doubles;

    @BeforeEach
    void setUp() throws Exception {
        service = new TeamUpgradeService();
        service.setCatalog(UpgradeConfigLoader.load(parse(catalogYaml())));
        solo = ModeProfileRegistry.defaults().get(MatchMode.SOLO);
        doubles = ModeProfileRegistry.defaults().get(MatchMode.DOUBLES);
    }

    private YamlConfiguration parse(String yaml) throws Exception {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(yaml);
        return configuration;
    }

    private String catalogYaml() {
        return "team-upgrades:\n"
                + "  PROTECTION:\n"
                + "    icon: IRON_CHESTPLATE\n"
                + "    levels:\n"
                + "      1:\n"
                + "        amplifier: 0\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 2}\n"
                + "          doubles: {currency: DIAMOND, amount: 3}\n"
                + "          trio: {currency: DIAMOND, amount: 4}\n"
                + "      2:\n"
                + "        amplifier: 1\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 4}\n"
                + "          doubles: {currency: DIAMOND, amount: 6}\n"
                + "          trio: {currency: DIAMOND, amount: 8}\n"
                + "  SHARPNESS:\n"
                + "    icon: IRON_SWORD\n"
                + "    levels:\n"
                + "      1:\n"
                + "        amplifier: 0\n"
                + "        cost:\n"
                + "          solo_duel: {currency: FEATHER, amount: 10}\n"
                + "          doubles: {currency: FEATHER, amount: 10}\n"
                + "          trio: {currency: FEATHER, amount: 10}\n"
                + "traps:\n"
                + "  maximum: 2\n"
                + "  position-costs:\n"
                + "    1:\n"
                + "      solo_duel: {currency: DIAMOND, amount: 1}\n"
                + "      doubles: {currency: DIAMOND, amount: 1}\n"
                + "      trio: {currency: DIAMOND, amount: 1}\n"
                + "    2:\n"
                + "      solo_duel: {currency: DIAMOND, amount: 3}\n"
                + "      doubles: {currency: DIAMOND, amount: 3}\n"
                + "      trio: {currency: DIAMOND, amount: 3}\n"
                + "  types:\n"
                + "    alarm:\n"
                + "      icon: REDSTONE_TORCH_ON\n"
                + "      reveals-invisibility: true\n";
    }

    private ResourceInventory wallet(ResourceType type, int amount) {
        Map<ResourceType, Integer> contents =
                new HashMap<ResourceType, Integer>();
        contents.put(type, Integer.valueOf(amount));
        return ResourceInventory.with(contents);
    }

    // ------------------------------------------------------------------
    // Acquisto
    // ------------------------------------------------------------------

    @Test
    void unAcquistoRiuscitoAddebitaEsattamenteIlCosto() {
        ResourceInventory purse = wallet(ResourceType.DIAMOND, 10);

        assertSame(UpgradeResult.SUCCESS, service.purchase(purse, ARENA, TEAM,
                TeamUpgradeType.PROTECTION, solo));

        // 10 iniziali = 2 consumati + 8 rimasti
        assertEquals(8, purse.count(ResourceType.DIAMOND));
        assertEquals(1, service.getState(ARENA, TEAM)
                .getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void iLivelliSuccessiviCostanoDiPiu() {
        ResourceInventory purse = wallet(ResourceType.DIAMOND, 10);

        service.purchase(purse, ARENA, TEAM, TeamUpgradeType.PROTECTION, solo);
        service.purchase(purse, ARENA, TEAM, TeamUpgradeType.PROTECTION, solo);

        // 10 = 2 + 4 consumati, 4 rimasti
        assertEquals(4, purse.count(ResourceType.DIAMOND));
        assertEquals(2, service.getState(ARENA, TEAM)
                .getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void oltreIlLivelloMassimoNonSiPaga() {
        ResourceInventory purse = wallet(ResourceType.DIAMOND, 20);
        service.purchase(purse, ARENA, TEAM, TeamUpgradeType.PROTECTION, solo);
        service.purchase(purse, ARENA, TEAM, TeamUpgradeType.PROTECTION, solo);
        int remaining = purse.count(ResourceType.DIAMOND);

        assertSame(UpgradeResult.MAX_LEVEL, service.purchase(purse, ARENA, TEAM,
                TeamUpgradeType.PROTECTION, solo));

        assertEquals(remaining, purse.count(ResourceType.DIAMOND));
        assertEquals(2, service.getState(ARENA, TEAM)
                .getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void conRisorseInsufficientiNienteVieneAddebitato() {
        ResourceInventory purse = wallet(ResourceType.DIAMOND, 1);

        assertSame(UpgradeResult.NOT_ENOUGH, service.purchase(purse, ARENA,
                TEAM, TeamUpgradeType.PROTECTION, solo));

        // Nessuna risorsa persa e nessun livello riservato.
        assertEquals(1, purse.count(ResourceType.DIAMOND));
        assertEquals(0, service.getState(ARENA, TEAM)
                .getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void ilProfiloDellaPartitaDeterminaIlPrezzo() {
        ResourceInventory soloPurse = wallet(ResourceType.DIAMOND, 10);
        ResourceInventory doublesPurse = wallet(ResourceType.DIAMOND, 10);

        service.purchase(soloPurse, ARENA, "red",
                TeamUpgradeType.PROTECTION, solo);
        service.purchase(doublesPurse, ARENA, "blue",
                TeamUpgradeType.PROTECTION, doubles);

        assertEquals(8, soloPurse.count(ResourceType.DIAMOND));
        assertEquals(7, doublesPurse.count(ResourceType.DIAMOND));
    }

    @Test
    void duelloESoloPaganoLoStessoPrezzo() {
        ModeProfile duel = ModeProfileRegistry.defaults().get(MatchMode.DUEL);
        ResourceInventory duelPurse = wallet(ResourceType.DIAMOND, 10);
        ResourceInventory soloPurse = wallet(ResourceType.DIAMOND, 10);

        service.purchase(duelPurse, ARENA, "red",
                TeamUpgradeType.PROTECTION, duel);
        service.purchase(soloPurse, "altra", "red",
                TeamUpgradeType.PROTECTION, solo);

        assertEquals(duelPurse.count(ResourceType.DIAMOND),
                soloPurse.count(ResourceType.DIAMOND));
    }

    @Test
    void lePiumeSonoUnaValutaValidaPerGliUpgrade() {
        ResourceInventory purse = wallet(ResourceType.FEATHER, 25);

        assertSame(UpgradeResult.SUCCESS, service.purchase(purse, ARENA, TEAM,
                TeamUpgradeType.SHARPNESS, solo));

        assertEquals(15, purse.count(ResourceType.FEATHER));
    }

    @Test
    void unUpgradeAssenteNonAddebitaNulla() {
        ResourceInventory purse = wallet(ResourceType.DIAMOND, 10);

        assertSame(UpgradeResult.UNAVAILABLE, service.purchase(purse, ARENA,
                TEAM, TeamUpgradeType.HASTE, solo));

        assertEquals(10, purse.count(ResourceType.DIAMOND));
    }

    @Test
    void senzaSquadraLAcquistoVieneRifiutato() {
        ResourceInventory purse = wallet(ResourceType.DIAMOND, 10);

        assertSame(UpgradeResult.NO_TEAM, service.purchase(purse, ARENA, null,
                TeamUpgradeType.PROTECTION, solo));

        assertEquals(10, purse.count(ResourceType.DIAMOND));
    }

    // ------------------------------------------------------------------
    // Isolamento
    // ------------------------------------------------------------------

    @Test
    void leSquadreSonoIsolate() {
        service.purchase(wallet(ResourceType.DIAMOND, 10), ARENA, "red",
                TeamUpgradeType.PROTECTION, solo);

        assertEquals(1, service.getState(ARENA, "red")
                .getLevel(TeamUpgradeType.PROTECTION));
        assertEquals(0, service.getState(ARENA, "blue")
                .getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void areneDiverseNonCondividonoLoStessoNomeSquadra() {
        service.purchase(wallet(ResourceType.DIAMOND, 10), "farm", "red",
                TeamUpgradeType.PROTECTION, solo);

        // Senza la chiave composta le due squadre "red" si confonderebbero.
        assertEquals(0, service.getState("castle", "red")
                .getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void laPuliziaDellArenaNonToccaLeAltre() {
        service.purchase(wallet(ResourceType.DIAMOND, 10), "farm", "red",
                TeamUpgradeType.PROTECTION, solo);
        service.purchase(wallet(ResourceType.DIAMOND, 10), "castle", "red",
                TeamUpgradeType.PROTECTION, solo);

        assertEquals(1, service.clearArena("farm"));

        assertEquals(0, service.getState("farm", "red")
                .getLevel(TeamUpgradeType.PROTECTION));
        assertEquals(1, service.getState("castle", "red")
                .getLevel(TeamUpgradeType.PROTECTION));
    }

    // ------------------------------------------------------------------
    // Incantesimi forniti all'equipaggiamento
    // ------------------------------------------------------------------

    @Test
    void ilProviderRiflettteILivelliAcquistati() {
        PlayerSession session =
                new PlayerSession(UUID.randomUUID(), "Tester", ARENA, null);
        session.setTeamId(TEAM);

        assertEquals(0, service.getProtectionLevel(session));
        assertEquals(0, service.getSharpnessLevel(session));

        service.purchase(wallet(ResourceType.DIAMOND, 10), ARENA, TEAM,
                TeamUpgradeType.PROTECTION, solo);
        service.purchase(wallet(ResourceType.FEATHER, 20), ARENA, TEAM,
                TeamUpgradeType.SHARPNESS, solo);

        // amplifier 0 corrisponde al livello I dell'incantesimo.
        assertEquals(1, service.getProtectionLevel(session));
        assertEquals(1, service.getSharpnessLevel(session));
    }

    @Test
    void ilProviderIgnoraLeSessioniSenzaSquadra() {
        PlayerSession session =
                new PlayerSession(UUID.randomUUID(), "Tester", ARENA, null);

        assertEquals(0, service.getProtectionLevel(session));
        assertEquals(0, service.getSharpnessLevel(null));
    }

    @Test
    void ilProviderNonMescolaLeArene() {
        service.purchase(wallet(ResourceType.DIAMOND, 10), "farm", TEAM,
                TeamUpgradeType.PROTECTION, solo);
        PlayerSession elsewhere =
                new PlayerSession(UUID.randomUUID(), "Tester", "castle", null);
        elsewhere.setTeamId(TEAM);

        assertEquals(0, service.getProtectionLevel(elsewhere));
    }

    // ------------------------------------------------------------------
    // Trappole
    // ------------------------------------------------------------------

    @Test
    void leTrappoleCostanoDiPiuManManoCheLaCodaCresce() {
        ResourceInventory purse = wallet(ResourceType.DIAMOND, 10);

        assertSame(UpgradeResult.SUCCESS, service.purchaseTrap(purse, ARENA,
                TEAM, "alarm", solo));
        assertEquals(9, purse.count(ResourceType.DIAMOND));

        assertSame(UpgradeResult.SUCCESS, service.purchaseTrap(purse, ARENA,
                TEAM, "alarm", solo));
        assertEquals(6, purse.count(ResourceType.DIAMOND));
    }

    @Test
    void oltreIlLimiteLaTrappolaNonVieneAddebitata() {
        ResourceInventory purse = wallet(ResourceType.DIAMOND, 20);
        service.purchaseTrap(purse, ARENA, TEAM, "alarm", solo);
        service.purchaseTrap(purse, ARENA, TEAM, "alarm", solo);
        int remaining = purse.count(ResourceType.DIAMOND);

        assertSame(UpgradeResult.TRAP_QUEUE_FULL, service.purchaseTrap(purse,
                ARENA, TEAM, "alarm", solo));

        assertEquals(remaining, purse.count(ResourceType.DIAMOND));
        assertEquals(2, service.getState(ARENA, TEAM).getTrapCount());
    }

    @Test
    void unaTrappolaSenzaRisorseNonRestaInCoda() {
        ResourceInventory purse = wallet(ResourceType.DIAMOND, 0);

        assertSame(UpgradeResult.NOT_ENOUGH, service.purchaseTrap(purse, ARENA,
                TEAM, "alarm", solo));

        assertEquals(0, service.getState(ARENA, TEAM).getTrapCount());
    }

    @Test
    void unaTrappolaSconosciutaVieneRifiutata() {
        ResourceInventory purse = wallet(ResourceType.DIAMOND, 10);

        assertSame(UpgradeResult.UNAVAILABLE, service.purchaseTrap(purse, ARENA,
                TEAM, "inesistente", solo));

        assertEquals(10, purse.count(ResourceType.DIAMOND));
        assertEquals(0, service.getState(ARENA, TEAM).getTrapCount());
    }

    @Test
    void ilCatalogoIncludeLaTrappolaConfigurata() {
        assertTrue(service.getCatalog().getTrap("alarm") != null);
        assertTrue(service.getCatalog().getTrap("alarm").revealsInvisibility());
        assertEquals(2, service.getCatalog().getMaximumTraps());
    }
}
