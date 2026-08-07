package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.arena.TeamDefinition;
import it.legacynetwork.chickenwars.chicken.ChickenSettings;
import it.legacynetwork.chickenwars.chicken.RoyalChicken;
import it.legacynetwork.chickenwars.model.TeamColor;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTeamTest {

    private GameTeam team() {
        return new GameTeam(new TeamDefinition("red", null, TeamColor.RED, 4));
    }

    /**
     * Gallina gia' uscita dalla protezione iniziale, come a partita avviata.
     */
    private RoyalChicken chicken() {
        RoyalChicken royal =
                new RoyalChicken("red", null, ChickenSettings.fromSection(null));
        royal.releaseProtection();
        return royal;
    }

    @Test
    void unaSquadraConGallinaVivaNonEFuori() {
        GameTeam team = team();
        team.setChicken(chicken());
        team.addMember(UUID.randomUUID());

        assertTrue(team.hasChicken());
        assertFalse(team.isOut());
    }

    @Test
    void senzaGallinaMaConSuperstitiRestaInPartita() {
        GameTeam team = team();
        UUID member = UUID.randomUUID();
        team.addMember(member);

        assertFalse(team.hasChicken());
        assertFalse(team.isOut());
    }

    @Test
    void senzaGallinaESenzaSuperstitiEFuori() {
        GameTeam team = team();
        UUID member = UUID.randomUUID();
        team.addMember(member);

        assertTrue(team.eliminateMember(member));
        assertTrue(team.isOut());
    }

    @Test
    void unMembroGiaEliminatoNonVieneContatoDueVolte() {
        GameTeam team = team();
        UUID member = UUID.randomUUID();
        team.addMember(member);

        assertTrue(team.eliminateMember(member));
        assertFalse(team.eliminateMember(member));
    }

    @Test
    void laGallinaMortaImpedisceIlRespawn() {
        GameTeam team = team();
        RoyalChicken royal = chicken();
        team.setChicken(royal);
        team.addMember(UUID.randomUUID());

        royal.damage(1000.0D, null);

        assertFalse(team.hasChicken());
        assertFalse(team.isOut());
    }

    @Test
    void laSquadraNonSuperaLaCapienzaMassima() {
        GameTeam team = new GameTeam(
                new TeamDefinition("red", null, TeamColor.RED, 1));

        assertTrue(team.addMember(UUID.randomUUID()));
        assertFalse(team.addMember(UUID.randomUUID()));
        assertTrue(team.isFull());
    }
}
