package it.legacynetwork.chickenwars.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProfileLifecycleServiceTest {
    private final ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor();
    @AfterEach void close(){scheduler.shutdownNow();}
    @Test void loadsAbsentProfileAndAllowsTrackedJoin(){UUID id=UUID.randomUUID();ProfileLifecycleService service=new ProfileLifecycleService(new InMemoryProgressionRepository(),new InMemoryStatisticsRepository(),new InMemoryQuickBuyRepository(),scheduler,1000,0,DatabaseOfflinePolicy.REJECT_TRACKED);ProfileLoadResult result=service.load(id).toCompletableFuture().join();assertTrue(result.isLoaded());assertEquals(0,result.getProfile().getProgress().getCoins());assertTrue(service.mayEnterTracked(id));service.saveAndUnload(id).toCompletableFuture().join();assertFalse(service.mayEnterTracked(id));}
    @Test void databaseFailureRejectsTrackedModes(){ProgressionRepository failed=new ProgressionRepository(){public CompletionStage<PlayerProgressRecord> load(UUID id){CompletableFuture<PlayerProgressRecord> f=new CompletableFuture<PlayerProgressRecord>();f.completeExceptionally(new IllegalStateException());return f;}public CompletionStage<Void> save(PlayerProgressRecord r){return CompletableFuture.completedFuture(null);}public CompletionStage<Void> close(){return CompletableFuture.completedFuture(null);}};ProfileLifecycleService service=new ProfileLifecycleService(failed,new InMemoryStatisticsRepository(),new InMemoryQuickBuyRepository(),scheduler,1000,1,DatabaseOfflinePolicy.REJECT_TRACKED);UUID id=UUID.randomUUID();assertEquals(ProfileLoadResult.Status.OFFLINE,service.load(id).toCompletableFuture().join().getStatus());assertFalse(service.mayEnterTracked(id));}
    @Test void timeoutCompletesEvenWhenRepositoryNeverDoes(){ProgressionRepository stuck=new ProgressionRepository(){public CompletionStage<PlayerProgressRecord> load(UUID id){return new CompletableFuture<PlayerProgressRecord>();}public CompletionStage<Void> save(PlayerProgressRecord r){return CompletableFuture.completedFuture(null);}public CompletionStage<Void> close(){return CompletableFuture.completedFuture(null);}};ProfileLifecycleService service=new ProfileLifecycleService(stuck,new InMemoryStatisticsRepository(),new InMemoryQuickBuyRepository(),scheduler,20,0,DatabaseOfflinePolicy.REJECT_TRACKED);assertEquals(ProfileLoadResult.Status.TIMED_OUT,service.load(UUID.randomUUID()).toCompletableFuture().join().getStatus());}
    @Test void lateCompletionAfterTimeoutDoesNotAuthorizeTrackedJoin() throws Exception {final CompletableFuture<PlayerProgressRecord> delayed=new CompletableFuture<PlayerProgressRecord>();ProgressionRepository repo=new ProgressionRepository(){public CompletionStage<PlayerProgressRecord> load(UUID id){return delayed;}public CompletionStage<Void> save(PlayerProgressRecord r){return CompletableFuture.completedFuture(null);}public CompletionStage<Void> close(){return CompletableFuture.completedFuture(null);}};ProfileLifecycleService service=new ProfileLifecycleService(repo,new InMemoryStatisticsRepository(),new InMemoryQuickBuyRepository(),scheduler,20,0,DatabaseOfflinePolicy.REJECT_TRACKED);UUID id=UUID.randomUUID();assertEquals(ProfileLoadResult.Status.TIMED_OUT,service.load(id).toCompletableFuture().join().getStatus());delayed.complete(new PlayerProgressRecord(id,1,1,1));Thread.sleep(10);assertFalse(service.mayEnterTracked(id));}
}
