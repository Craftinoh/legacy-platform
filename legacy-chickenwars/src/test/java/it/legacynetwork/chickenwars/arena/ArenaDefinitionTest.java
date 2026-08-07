package it.legacynetwork.chickenwars.arena;

import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import it.legacynetwork.chickenwars.model.TeamColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArenaDefinitionTest {

    private SimpleLocation at(double x, double y, double z) {
        return new SimpleLocation("cw_farm", x, y, z, 0.0F, 0.0F);
    }

    private TeamDefinition completeTeam(String id, TeamColor color) {
        TeamDefinition team = new TeamDefinition(id, null, color, 4);
        team.setSpawn(at(0.0D, 65.0D, 0.0D));
        team.setNest(at(1.0D, 65.0D, 0.0D));
        team.setChicken(at(2.0D, 65.0D, 0.0D));
        team.setShop(at(3.0D, 65.0D, 0.0D));
        return team;
    }

    private ArenaDefinition completeArena() {
        ArenaDefinition arena = new ArenaDefinition("farm_kingdom");
        arena.setWorld("cw_farm");
        arena.setLobby(at(0.0D, 100.0D, 0.0D));
        arena.setSpectator(at(0.0D, 120.0D, 0.0D));
        arena.setPos1(at(-150.0D, 0.0D, -150.0D));
        arena.setPos2(at(150.0D, 200.0D, 150.0D));
        arena.addTeam(completeTeam("red", TeamColor.RED));
        arena.addTeam(completeTeam("blue", TeamColor.BLUE));
        arena.addGenerator(new GeneratorDefinition("iron_1", ResourceType.IRON,
                at(0.0D, 65.0D, 0.0D), "red", 1, false));
        return arena;
    }

    @Test
    void unArenaCompletaNonHaElementiMancanti() {
        assertTrue(completeArena().isComplete());
    }

    @Test
    void unArenaVuotaElencaTuttiGliElementiMancanti() {
        List<String> missing = new ArenaDefinition("vuota").findMissing();

        assertFalse(missing.isEmpty());
        assertTrue(missing.contains("mondo dell'arena"));
        assertTrue(missing.contains("lobby pre-partita"));
        assertTrue(missing.contains("spawn spettatori"));
        assertTrue(missing.contains("almeno due squadre configurate"));
        assertTrue(missing.contains("almeno un generatore"));
    }

    @Test
    void servonoAlmenoDueSquadre() {
        ArenaDefinition arena = completeArena();
        arena.removeTeam("blue");

        assertTrue(arena.findMissing().contains("almeno due squadre configurate"));
    }

    @Test
    void leSquadreIncompleteVengonoSegnalate() {
        ArenaDefinition arena = completeArena();
        arena.getTeam("red").setChicken(null);

        assertTrue(arena.findMissing()
                .contains("posizione gallina squadra red"));
    }

    @Test
    void laCapienzaELaSommaDelleSquadre() {
        assertEquals(8, completeArena().getMaximumPlayers());
    }

    @Test
    void laRegioneRiconosceIPuntiInterni() {
        ArenaDefinition arena = completeArena();

        assertTrue(arena.contains("cw_farm", 0.0D, 65.0D, 0.0D));
        assertTrue(arena.contains("cw_farm", -150.0D, 0.0D, -150.0D));
        assertFalse(arena.contains("cw_farm", 200.0D, 65.0D, 0.0D));
        assertFalse(arena.contains("altro_mondo", 0.0D, 65.0D, 0.0D));
    }

    @Test
    void senzaRegioneNessunPuntoEConsideratoInterno() {
        ArenaDefinition arena = new ArenaDefinition("vuota");

        assertFalse(arena.contains("cw_farm", 0.0D, 0.0D, 0.0D));
    }

    @Test
    void gliIdGeneratoreSonoProgressiviELiberi() {
        ArenaDefinition arena = completeArena();

        // L'arena contiene gia' iron_1, quindi il primo ID libero e' iron_2.
        String first = arena.nextGeneratorId("iron");
        arena.addGenerator(new GeneratorDefinition(first, ResourceType.IRON,
                at(5.0D, 65.0D, 5.0D), null, 1, true));
        String second = arena.nextGeneratorId("iron");

        assertNotEquals(first, second);
        assertEquals("iron_2", first);
        assertEquals("iron_3", second);
    }

    @Test
    void gliIdVengonoNormalizzatiInMinuscolo() {
        ArenaDefinition arena = new ArenaDefinition("  FARM_Kingdom ");

        assertEquals("farm_kingdom", arena.getId());
    }

    @Test
    void generatoriDisabilitatiNonRendonoCompletaLArena() {
        ArenaDefinition arena = completeArena();
        arena.removeGenerator("iron_1");
        arena.addGenerator(new GeneratorDefinition("off", ResourceType.IRON,
                at(0.0D, 65.0D, 0.0D), null, 1, false, false));

        assertTrue(arena.findMissing().contains("almeno un generatore"));
    }

    @Test
    void unIdGeneratoreDuplicatoVieneRifiutato() {
        ArenaDefinition arena = completeArena();
        assertThrows(IllegalArgumentException.class, () -> arena.addGenerator(
                new GeneratorDefinition("IRON_1", ResourceType.GOLD,
                        at(0.0D, 65.0D, 0.0D), null, 1, false)));
    }
}
