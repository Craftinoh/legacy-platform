package it.legacynetwork.chickenwars.routing;

import static org.junit.jupiter.api.Assertions.*;
import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GameRoutingTest {
    private GameInstanceDescriptor instance(String id,int players,int capacity,long heartbeat){return new GameInstanceDescriptor(id,"server-"+id,MatchMode.SOLO,"arena",InstanceStatus.WAITING,players,capacity,heartbeat,true);}
    @Test void prefersPopulatedAvailableInstanceAndReserves(){InMemoryInstanceRegistry registry=new InMemoryInstanceRegistry();registry.heartbeat(instance("a",1,8,100));registry.heartbeat(instance("b",4,8,100));ReservationService reservations=new ReservationService();RouteResult result=new GameRouter(registry,reservations,50,20).route(UUID.randomUUID(),MatchMode.SOLO,null,"request",110);assertTrue(result.isSuccessful());assertEquals("b",result.getInstance().getInstanceId());assertEquals(1,reservations.reservedSeats("b",110));}
    @Test void staleAndFullInstancesAreNotRouted(){InMemoryInstanceRegistry registry=new InMemoryInstanceRegistry();registry.heartbeat(instance("stale",0,8,1));registry.heartbeat(instance("full",8,8,100));RouteResult result=new GameRouter(registry,new ReservationService(),50,20).route(UUID.randomUUID(),MatchMode.SOLO,null,"r",100);assertEquals(RouteFailure.NO_INSTANCE,result.getFailure());}
    @Test void partyCapacityIsAtomic(){InMemoryInstanceRegistry registry=new InMemoryInstanceRegistry();registry.heartbeat(instance("a",7,8,100));RouteResult result=new GameRouter(registry,new ReservationService(),50,20).route(UUID.randomUUID(),MatchMode.SOLO,Arrays.asList(UUID.randomUUID(),UUID.randomUUID()),"party",100);assertFalse(result.isSuccessful());}
    @Test void duplicateRequestReturnsSameReservation(){InMemoryInstanceRegistry registry=new InMemoryInstanceRegistry();registry.heartbeat(instance("a",0,8,100));ReservationService reservations=new ReservationService();GameRouter router=new GameRouter(registry,reservations,50,20);UUID id=UUID.randomUUID();String first=router.route(id,MatchMode.SOLO,null,"same",100).getReservations().get(0).getReservationId();String second=router.route(id,MatchMode.SOLO,null,"same",100).getReservations().get(0).getReservationId();assertEquals(first,second);assertEquals(1,reservations.reservedSeats("a",100));}
    @Test void claimIsSingleUseAndExpiryReleasesSeat(){ReservationService service=new ReservationService();UUID id=UUID.randomUUID();GameReservation value=service.create(id,MatchMode.SOLO,"a",10,"k");assertTrue(service.claim(value.getReservationId(),id,1));assertFalse(service.claim(value.getReservationId(),id,1));GameReservation expiring=service.create(UUID.randomUUID(),MatchMode.SOLO,"a",5,"e");assertEquals(1,service.cleanup(5));assertEquals(0,service.reservedSeats("a",5));assertEquals(ReservationStatus.EXPIRED,expiring.getStatus());}
    @Test void distributedReconnectConsumesOnceAndRejectsEnding(){InMemoryInstanceRegistry registry=new InMemoryInstanceRegistry();registry.heartbeat(instance("a",1,8,100));DistributedReconnectService service=new DistributedReconnectService();UUID id=UUID.randomUUID();service.remember(new DistributedReconnectService.Session(id,"a",200));assertNotNull(service.consume(id,110,registry,50));assertNull(service.consume(id,110,registry,50));}
}
