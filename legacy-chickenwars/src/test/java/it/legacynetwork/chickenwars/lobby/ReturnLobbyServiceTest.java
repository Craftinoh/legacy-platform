package it.legacynetwork.chickenwars.lobby;

import it.legacynetwork.chickenwars.routing.TransferGateway;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReturnLobbyServiceTest {
    @Test
    void configuredLobbyUsesProxyConnect() {
        TransferGateway gateway = mock(TransferGateway.class);
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.isOnline()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(id);

        assertTrue(new ReturnLobbyService(gateway, "lobby-1").transfer(player));
        verify(gateway).connect(id, "lobby-1");
    }

    @Test
    void emptyLobbyFallsBackWithoutSendingAnything() {
        TransferGateway gateway = mock(TransferGateway.class);
        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);

        assertFalse(new ReturnLobbyService(gateway, "").transfer(player));
        verifyNoInteractions(gateway);
    }
}
