package it.legacynetwork.chickenwars.lobby;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.routing.*;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class LobbyRoutingServiceTest {
    @Test void transfersOnlyAfterReservationAndBlocksDuplicateRequest(){InMemoryInstanceRegistry instances=new InMemoryInstanceRegistry();instances.heartbeat(new GameInstanceDescriptor("i","server",MatchMode.SOLO,"a",InstanceStatus.WAITING,0,8,100,true));ReservationService reservations=new ReservationService();TransferGateway transfer=mock(TransferGateway.class);Player player=mock(Player.class);UUID id=UUID.randomUUID();when(player.getUniqueId()).thenReturn(id);LobbyRoutingService service=new LobbyRoutingService(new InMemoryRoutingCoordinator(instances,reservations,50,20),new InMemoryReconnectCoordinator(new DistributedReconnectService(),instances,reservations,50,20),transfer,mock(MessageService.class),Runnable::run);assertTrue(service.join(player,MatchMode.SOLO,110));assertEquals(LobbyQueueState.TRANSFERRING,service.state(id));assertFalse(service.join(player,MatchMode.SOLO,110));verify(transfer).transfer(eq(id),eq("server"),anyString(),eq("a"));}
    @Test void noInstanceClearsQueueAndNeverTransfers(){TransferGateway transfer=mock(TransferGateway.class);Player player=mock(Player.class);UUID id=UUID.randomUUID();when(player.getUniqueId()).thenReturn(id);InMemoryInstanceRegistry instances=new InMemoryInstanceRegistry();ReservationService reservations=new ReservationService();LobbyRoutingService service=new LobbyRoutingService(new InMemoryRoutingCoordinator(instances,reservations,50,20),new InMemoryReconnectCoordinator(new DistributedReconnectService(),instances,reservations,50,20),transfer,mock(MessageService.class),Runnable::run);assertTrue(service.join(player,MatchMode.TRIO,1));assertNull(service.state(id));verifyNoInteractions(transfer);}
}
