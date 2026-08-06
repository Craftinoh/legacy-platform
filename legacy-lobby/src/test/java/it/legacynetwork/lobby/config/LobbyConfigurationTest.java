package it.legacynetwork.lobby.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyConfigurationTest {

    @Test
    void validSlotBetweenOneAndNine() {
        FileConfiguration config = parseConfig(
                "join:\n" +
                "  selected-slot:\n" +
                "    enabled: true\n" +
                "    slot: 5\n" +
                "    delay-ticks: 2\n" +
                "    force: true\n");
        LobbyConfiguration lobbyConfig = LobbyConfiguration.from(config);
        assertTrue(lobbyConfig.isJoinSlotEnabled());
        assertEquals(5, lobbyConfig.getJoinSlot());
        assertEquals(2, lobbyConfig.getJoinSlotDelayTicks());
        assertTrue(lobbyConfig.isJoinSlotForce());
    }

    @Test
    void firstAndLastSlotsAreAccepted() {
        FileConfiguration firstConfig = parseConfig(
                "join:\n  selected-slot:\n    slot: 1\n");
        FileConfiguration lastConfig = parseConfig(
                "join:\n  selected-slot:\n    slot: 9\n");
        assertEquals(1,
                LobbyConfiguration.from(firstConfig).getJoinSlot());
        assertEquals(9,
                LobbyConfiguration.from(lastConfig).getJoinSlot());
    }

    @Test
    void slotBelowOneFallsBackToOne() {
        FileConfiguration config = parseConfig(
                "join:\n" +
                "  selected-slot:\n" +
                "    slot: 0\n");
        LobbyConfiguration lobbyConfig = LobbyConfiguration.from(config);
        assertEquals(1, lobbyConfig.getJoinSlot());
    }

    @Test
    void slotAboveNineFallsBackToOne() {
        FileConfiguration config = parseConfig(
                "join:\n" +
                "  selected-slot:\n" +
                "    slot: 10\n");
        LobbyConfiguration lobbyConfig = LobbyConfiguration.from(config);
        assertEquals(1, lobbyConfig.getJoinSlot());
    }

    @Test
    void joinSlotDisabled() {
        FileConfiguration config = parseConfig(
                "join:\n" +
                "  selected-slot:\n" +
                "    enabled: false\n");
        LobbyConfiguration lobbyConfig = LobbyConfiguration.from(config);
        assertFalse(lobbyConfig.isJoinSlotEnabled());
    }

    @Test
    void forceDisabledAllowsManualChange() {
        FileConfiguration config = parseConfig(
                "join:\n" +
                "  selected-slot:\n" +
                "    force: false\n");
        LobbyConfiguration lobbyConfig = LobbyConfiguration.from(config);
        assertFalse(lobbyConfig.isJoinSlotForce());
    }

    @Test
    void negativeDelayClampedToZero() {
        FileConfiguration config = parseConfig(
                "join:\n" +
                "  selected-slot:\n" +
                "    delay-ticks: -5\n");
        LobbyConfiguration lobbyConfig = LobbyConfiguration.from(config);
        assertEquals(0, lobbyConfig.getJoinSlotDelayTicks());
    }

    @Test
    void authMeAndVoidTeleportAreDisabledByDefault() {
        LobbyConfiguration lobbyConfig =
                LobbyConfiguration.from(parseConfig(""));
        assertFalse(lobbyConfig.isAuthmeIntegration());
        assertFalse(lobbyConfig.isVoidTeleportEnabled());
        assertEquals("AUTHME", lobbyConfig.getVoidTeleportTarget());
        assertEquals("WORLD_SPAWN", lobbyConfig.getVoidTeleportFallback());
        assertEquals(10, lobbyConfig.getVoidTeleportCheckTicks());
    }

    @Test
    void voidTeleportValuesAreLoadedAndCadenceIsClamped() {
        FileConfiguration config = parseConfig(
                "spawn:\n" +
                "  authme-integration:\n" +
                "    enabled: true\n" +
                "  void-teleport:\n" +
                "    enabled: true\n" +
                "    below-y: -10\n" +
                "    target: WORLD_SPAWN\n" +
                "    fallback: DISABLED\n" +
                "    check-ticks: 1\n");
        LobbyConfiguration lobbyConfig = LobbyConfiguration.from(config);
        assertTrue(lobbyConfig.isAuthmeIntegration());
        assertTrue(lobbyConfig.isVoidTeleportEnabled());
        assertEquals(-10, lobbyConfig.getVoidTeleportBelowY());
        assertEquals("WORLD_SPAWN", lobbyConfig.getVoidTeleportTarget());
        assertEquals("DISABLED", lobbyConfig.getVoidTeleportFallback());
        assertEquals(5, lobbyConfig.getVoidTeleportCheckTicks());
    }

    @Test
    void defaultConfigValues() {
        FileConfiguration config = parseConfig("");
        LobbyConfiguration lobbyConfig = LobbyConfiguration.from(config);
        assertEquals("lobby-01", lobbyConfig.getServerId());
        assertEquals("NetworkLang", lobbyConfig.getLanguageChannel());
        assertEquals("en", lobbyConfig.getLanguageFallback());
        assertEquals("scoreboard.yml", lobbyConfig.getScoreboardConfigFile());
        assertEquals("bossbar.yml", lobbyConfig.getBossbarConfigFile());
    }

    private FileConfiguration parseConfig(String yaml) {
        return YamlConfiguration.loadConfiguration(new StringReader(yaml));
    }
}
