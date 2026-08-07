package it.legacynetwork.chickenwars.persistence;

import static org.junit.jupiter.api.Assertions.*;
import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class MatchFinalizationTest {
    private MatchFinalizationRequest request(String id,MatchMode mode,UUID player){return new MatchFinalizationRequest(id,mode,"red",Collections.singletonList(new MatchParticipantRecord(player,"red",true,50,20,3,2,1,1,7,60)),10);}
    @Test void appliesTrackedMatchExactlyOnce(){InMemoryMatchPersistence store=new InMemoryMatchPersistence();UUID id=UUID.randomUUID();assertTrue(store.finalizeMatch(request("m",MatchMode.SOLO,id)).toCompletableFuture().join().isApplied());assertFalse(store.finalizeMatch(request("m",MatchMode.SOLO,id)).toCompletableFuture().join().isApplied());assertEquals(50,store.getProgress(id).getTotalExperience());assertEquals(1,store.getStatistics(id,"SOLO").getGames());}
    @Test void duelKeepsRuntimeInputButPersistsNoRewardsOrStats(){InMemoryMatchPersistence store=new InMemoryMatchPersistence();UUID id=UUID.randomUUID();store.finalizeMatch(request("duel",MatchMode.DUEL,id)).toCompletableFuture().join();assertNull(store.getProgress(id));assertNull(store.getStatistics(id,"DUEL"));assertTrue(store.hasMatch("duel"));}
    @Test void rollsBackEveryWriteOnFailure(){InMemoryMatchPersistence store=new InMemoryMatchPersistence();store.setFailBeforeCommit(true);UUID id=UUID.randomUUID();assertThrows(CompletionException.class,()->store.finalizeMatch(request("broken",MatchMode.SOLO,id)).toCompletableFuture().join());assertNull(store.getProgress(id));assertFalse(store.hasMatch("broken"));}
}
