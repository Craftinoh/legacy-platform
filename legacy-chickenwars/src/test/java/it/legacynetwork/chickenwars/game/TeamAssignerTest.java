package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.arena.TeamDefinition;
import it.legacynetwork.chickenwars.model.TeamColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamAssignerTest {

    private GameTeam team(String id, TeamColor color, int maxPlayers) {
        return new GameTeam(new TeamDefinition(id, null, color, maxPlayers));
    }

    @Test
    void sceglieLaSquadraMenoNumerosa() {
        GameTeam red = team("red", TeamColor.RED, 4);
        GameTeam blue = team("blue", TeamColor.BLUE, 4);
        red.addMember(UUID.randomUUID());
        red.addMember(UUID.randomUUID());
        blue.addMember(UUID.randomUUID());

        assertSame(blue, TeamAssigner.findBestTeam(Arrays.asList(red, blue)));
    }

    @Test
    void aParitaMantieneLOrdineDiDefinizione() {
        GameTeam red = team("red", TeamColor.RED, 4);
        GameTeam blue = team("blue", TeamColor.BLUE, 4);

        assertSame(red, TeamAssigner.findBestTeam(Arrays.asList(red, blue)));
    }

    @Test
    void ignoraLeSquadrePiene() {
        GameTeam red = team("red", TeamColor.RED, 1);
        GameTeam blue = team("blue", TeamColor.BLUE, 2);
        red.addMember(UUID.randomUUID());

        assertSame(blue, TeamAssigner.findBestTeam(Arrays.asList(red, blue)));
    }

    @Test
    void restituisceNullQuandoTutteLeSquadreSonoPiene() {
        GameTeam red = team("red", TeamColor.RED, 1);
        red.addMember(UUID.randomUUID());

        assertNull(TeamAssigner.findBestTeam(Arrays.asList(red)));
    }

    @Test
    void distribuisceIGiocatoriInModoBilanciato() {
        GameTeam red = team("red", TeamColor.RED, 2);
        GameTeam blue = team("blue", TeamColor.BLUE, 2);
        List<UUID> players = new ArrayList<UUID>();
        for (int i = 0; i < 4; i++) {
            players.add(UUID.randomUUID());
        }

        List<UUID> leftovers =
                TeamAssigner.distribute(Arrays.asList(red, blue), players);

        assertTrue(leftovers.isEmpty());
        assertEquals(2, red.getMemberCount());
        assertEquals(2, blue.getMemberCount());
    }

    @Test
    void segnalaIGiocatoriRimastiSenzaPosto() {
        GameTeam red = team("red", TeamColor.RED, 1);
        GameTeam blue = team("blue", TeamColor.BLUE, 1);
        List<UUID> players = Arrays.asList(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID());

        List<UUID> leftovers =
                TeamAssigner.distribute(Arrays.asList(red, blue), players);

        assertEquals(1, leftovers.size());
        assertEquals(players.get(2), leftovers.get(0));
    }

    @Test
    void servonoAlmenoDueSquadreOccupate() {
        GameTeam red = team("red", TeamColor.RED, 4);
        GameTeam blue = team("blue", TeamColor.BLUE, 4);
        red.addMember(UUID.randomUUID());

        assertFalse(TeamAssigner.hasEnoughOccupiedTeams(Arrays.asList(red, blue)));

        blue.addMember(UUID.randomUUID());

        assertTrue(TeamAssigner.hasEnoughOccupiedTeams(Arrays.asList(red, blue)));
    }

    @Test
    void rilasciaIlGiocatoreDaTutteLeSquadre() {
        GameTeam red = team("red", TeamColor.RED, 4);
        UUID player = UUID.randomUUID();
        red.addMember(player);

        TeamAssigner.release(Arrays.asList(red), player);

        assertEquals(0, red.getMemberCount());
        assertFalse(red.isMember(player));
    }
}
