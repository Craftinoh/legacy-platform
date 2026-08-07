package it.legacynetwork.chickenwars.arena;

import it.legacynetwork.chickenwars.model.SimpleLocation;
import it.legacynetwork.chickenwars.model.TeamColor;
import it.legacynetwork.chickenwars.region.CuboidRegion;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regione base delle squadre, dalla configurazione al controllo puro.
 */
class TeamBaseRegionTest {

    private static final String WORLD = "cw_farm";

    private TeamDefinition team() {
        return new TeamDefinition("red", null, TeamColor.RED, 4);
    }

    private SimpleLocation at(String world, double x, double y, double z) {
        return new SimpleLocation(world, x, y, z, 0.0F, 0.0F);
    }

    // ------------------------------------------------------------------
    // Modello
    // ------------------------------------------------------------------

    @Test
    void senzaAngoliNonEsisteRegione() {
        TeamDefinition definition = team();

        assertNull(definition.getBaseRegion());
        assertFalse(definition.hasBaseRegion());
        // L'assenza non e' un errore: le mappe esistenti restano valide.
        assertTrue(definition.findBaseRegionIssues().isEmpty());
    }

    @Test
    void dueAngoliValidiFormanoLaRegione() {
        TeamDefinition definition = team();
        definition.setBaseMin(at(WORLD, 10.0D, 60.0D, 10.0D));
        definition.setBaseMax(at(WORLD, 20.0D, 70.0D, 20.0D));

        CuboidRegion region = definition.getBaseRegion();

        assertNotNull(region);
        assertTrue(definition.hasBaseRegion());
        assertTrue(region.contains(WORLD, 15.0D, 65.0D, 15.0D));
        assertFalse(region.contains(WORLD, 100.0D, 65.0D, 15.0D));
        assertTrue(definition.findBaseRegionIssues().isEmpty());
    }

    @Test
    void laRegioneVieneRicalcolataDopoUnaModifica() {
        TeamDefinition definition = team();
        definition.setBaseMin(at(WORLD, 0.0D, 0.0D, 0.0D));
        definition.setBaseMax(at(WORLD, 5.0D, 5.0D, 5.0D));
        assertTrue(definition.getBaseRegion().contains(WORLD, 3.0D, 3.0D, 3.0D));

        definition.setBaseMax(at(WORLD, 1.0D, 1.0D, 1.0D));

        assertFalse(definition.getBaseRegion()
                .contains(WORLD, 3.0D, 3.0D, 3.0D));
    }

    @Test
    void unSoloAngoloVieneSegnalato() {
        TeamDefinition definition = team();
        definition.setBaseMin(at(WORLD, 10.0D, 60.0D, 10.0D));

        List<String> issues = definition.findBaseRegionIssues();

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("angolo mancante"));
        assertNull(definition.getBaseRegion());
    }

    @Test
    void angoliInMondiDiversiVengonoSegnalati() {
        TeamDefinition definition = team();
        definition.setBaseMin(at(WORLD, 10.0D, 60.0D, 10.0D));
        definition.setBaseMax(at("altro_mondo", 20.0D, 70.0D, 20.0D));

        List<String> issues = definition.findBaseRegionIssues();

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("mondi diversi"));
        assertNull(definition.getBaseRegion());
    }

    @Test
    void leCoordinateNonFiniteVengonoSegnalate() {
        TeamDefinition definition = team();
        definition.setBaseMin(at(WORLD, Double.NaN, 60.0D, 10.0D));
        definition.setBaseMax(at(WORLD, 20.0D, 70.0D, 20.0D));

        assertFalse(definition.findBaseRegionIssues().isEmpty());
    }

    @Test
    void gliAngoliInvertitiVengonoNormalizzati() {
        TeamDefinition definition = team();
        definition.setBaseMin(at(WORLD, 20.0D, 70.0D, 20.0D));
        definition.setBaseMax(at(WORLD, 10.0D, 60.0D, 10.0D));

        assertTrue(definition.getBaseRegion()
                .contains(WORLD, 15.0D, 65.0D, 15.0D));
        assertTrue(definition.findBaseRegionIssues().isEmpty());
    }

    @Test
    void leRegioniSovrapposteSonoRilevabili() {
        TeamDefinition red = team();
        red.setBaseMin(at(WORLD, 0.0D, 60.0D, 0.0D));
        red.setBaseMax(at(WORLD, 10.0D, 70.0D, 10.0D));

        TeamDefinition blue = new TeamDefinition("blue", null,
                TeamColor.BLUE, 4);
        blue.setBaseMin(at(WORLD, 5.0D, 60.0D, 5.0D));
        blue.setBaseMax(at(WORLD, 15.0D, 70.0D, 15.0D));

        assertTrue(red.getBaseRegion().overlaps(blue.getBaseRegion()));
    }

    // ------------------------------------------------------------------
    // Persistenza
    // ------------------------------------------------------------------

    @Test
    void laRegioneSopravviveAlSalvataggioERilettura() throws Exception {
        ArenaDefinition arena = new ArenaDefinition("farm");
        arena.setWorld(WORLD);
        arena.setLobby(at(WORLD, 0.0D, 100.0D, 0.0D));
        arena.setSpectator(at(WORLD, 0.0D, 120.0D, 0.0D));
        arena.setPos1(at(WORLD, -50.0D, 0.0D, -50.0D));
        arena.setPos2(at(WORLD, 50.0D, 128.0D, 50.0D));

        TeamDefinition red = team();
        red.setSpawn(at(WORLD, 1.0D, 65.0D, 1.0D));
        red.setNest(at(WORLD, 2.0D, 65.0D, 1.0D));
        red.setChicken(at(WORLD, 3.0D, 65.0D, 1.0D));
        red.setShop(at(WORLD, 4.0D, 65.0D, 1.0D));
        red.setBaseMin(at(WORLD, 10.0D, 60.0D, 10.0D));
        red.setBaseMax(at(WORLD, 20.0D, 70.0D, 20.0D));
        arena.addTeam(red);

        File file = File.createTempFile("cw-arena", ".yml");
        file.deleteOnExit();
        Logger logger = Logger.getAnonymousLogger();

        assertTrue(ArenaConfigLoader.save(file, arena, logger));
        ArenaDefinition reloaded = ArenaConfigLoader.load(file, logger);

        assertNotNull(reloaded);
        TeamDefinition loadedTeam = reloaded.getTeam("red");
        assertNotNull(loadedTeam);
        assertTrue(loadedTeam.hasBaseRegion());
        assertTrue(loadedTeam.getBaseRegion()
                .contains(WORLD, 15.0D, 65.0D, 15.0D));
    }

    @Test
    void unArenaSenzaRegioniRestaCaricabile() throws Exception {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(
                "arena:\n"
                        + "  id: farm\n"
                        + "  world: " + WORLD + "\n"
                        + "teams:\n"
                        + "  red:\n"
                        + "    color: RED\n"
                        + "    spawn: \"" + WORLD + ",1,65,1,0,0\"\n");

        File file = File.createTempFile("cw-legacy", ".yml");
        file.deleteOnExit();
        configuration.save(file);

        ArenaDefinition reloaded =
                ArenaConfigLoader.load(file, Logger.getAnonymousLogger());

        assertNotNull(reloaded);
        assertFalse(reloaded.getTeam("red").hasBaseRegion());
        assertTrue(reloaded.getTeam("red").findBaseRegionIssues().isEmpty());
    }
}
