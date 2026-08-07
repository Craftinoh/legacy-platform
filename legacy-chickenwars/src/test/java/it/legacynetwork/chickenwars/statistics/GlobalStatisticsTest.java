package it.legacynetwork.chickenwars.statistics;

import it.legacynetwork.chickenwars.mode.MatchMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlobalStatisticsTest {

    @Test
    void sumsSoloDoublesAndTrio() {
        UUID playerId = UUID.randomUUID();
        GlobalStatistics global = GlobalStatistics.sum(Arrays.asList(
                new ModeStatistics(playerId, MatchMode.SOLO,
                        10, 3, 20, 15, 5, 2, 600, 100),
                new ModeStatistics(playerId, MatchMode.DOUBLES,
                        5, 2, 8, 6, 3, 1, 300, 50),
                new ModeStatistics(playerId, MatchMode.TRIO,
                        2, 1, 4, 2, 1, 1, 120, 20)));

        assertEquals(17L, global.getGames());
        assertEquals(6L, global.getWins());
        assertEquals(11L, global.getLosses());
        assertEquals(32L, global.getKills());
        assertEquals(23L, global.getDeaths());
        assertEquals(9L, global.getFinalKills());
        assertEquals(4L, global.getChickenKills());
    }

    @Test
    void duelCannotBecomePersistentStatistics() {
        UUID playerId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () ->
                new ModeStatistics(playerId, MatchMode.DUEL,
                        1, 1, 10, 0, 10, 1, 60, 10));
    }
}
