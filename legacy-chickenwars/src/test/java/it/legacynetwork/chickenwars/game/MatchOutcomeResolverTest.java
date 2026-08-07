package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.arena.TeamDefinition;
import it.legacynetwork.chickenwars.model.TeamColor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchOutcomeResolverTest {
    private GameTeam team(String id, TeamColor color, int alive) {
        GameTeam team = new GameTeam(new TeamDefinition(id, id, color, 4));
        for (int index = 0; index < alive; index++) {
            team.addMember(UUID.randomUUID());
        }
        return team;
    }

    @Test
    void exactlyOneEligibleTeamWinsOnce() {
        GameTeam red = team("red", TeamColor.RED, 1);
        GameTeam blue = team("blue", TeamColor.BLUE, 1);
        blue.collapse();
        blue.eliminateMember(blue.getMembers().iterator().next());

        MatchOutcomeResolver.Result result = new MatchOutcomeResolver()
                .afterElimination(Arrays.asList(red, blue));

        assertTrue(result.isTerminal());
        assertSame(red, result.getWinner());
        assertFalse(result.isDraw());
    }

    @Test
    void simultaneousEliminationProducesDraw() {
        GameTeam red = team("red", TeamColor.RED, 1);
        GameTeam blue = team("blue", TeamColor.BLUE, 1);
        for (GameTeam team : Arrays.asList(red, blue)) {
            team.collapse();
            team.eliminateMember(team.getMembers().iterator().next());
        }
        MatchOutcomeResolver.Result result = new MatchOutcomeResolver()
                .afterElimination(Arrays.asList(red, blue));
        assertTrue(result.isDraw());
        assertNull(result.getWinner());
    }

    @Test
    void timeoutPolicyUsesUniqueLeaderAndDrawsOnTie() {
        GameTeam red = team("red", TeamColor.RED, 2);
        GameTeam blue = team("blue", TeamColor.BLUE, 1);
        MatchOutcomeResolver resolver = new MatchOutcomeResolver();
        assertSame(red, resolver.atTimeout(Arrays.asList(red, blue),
                MatchTimeoutPolicy.LEADING_TEAM).getWinner());
        assertNull(resolver.atTimeout(Arrays.asList(red, blue),
                MatchTimeoutPolicy.DRAW).getWinner());
        red.eliminateMember(red.getAliveMembers().iterator().next());
        assertTrue(resolver.atTimeout(Arrays.asList(red, blue),
                MatchTimeoutPolicy.LEADING_TEAM).isDraw());
    }
}
