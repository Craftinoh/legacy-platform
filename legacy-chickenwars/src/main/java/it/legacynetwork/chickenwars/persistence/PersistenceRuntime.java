package it.legacynetwork.chickenwars.persistence;

import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import it.legacynetwork.chickenwars.routing.*;

/** Possiede pool, executor e repository di produzione e ne coordina il cleanup. */
public final class PersistenceRuntime {
    private final ExecutorService databaseExecutor;
    private final ScheduledExecutorService scheduler;
    private final HikariDataSource dataSource;
    private final QuickBuyRepository quickBuy;
    private final ProfileLifecycleService profiles;
    private final MatchPersistence matches;
    private final RoutingServices routing;
    private final CoinTransactionRepository coins;

    private PersistenceRuntime(ExecutorService databaseExecutor,
            ScheduledExecutorService scheduler,HikariDataSource dataSource,
            QuickBuyRepository quickBuy,ProfileLifecycleService profiles,
            MatchPersistence matches,RoutingServices routing,CoinTransactionRepository coins){this.databaseExecutor=databaseExecutor;this.scheduler=scheduler;
        this.dataSource=dataSource;this.quickBuy=quickBuy;this.profiles=profiles;this.matches=matches;this.routing=routing;this.coins=coins;}

    public static PersistenceRuntime start(DatabaseSettings settings,
            ClassLoader loader,Logger logger,long profileTimeout,int retries,
            long heartbeatTimeout,long reservationTtl){
        return start(settings,loader,logger,profileTimeout,retries,
                heartbeatTimeout,reservationTtl,"",120000L);
    }

    public static PersistenceRuntime start(DatabaseSettings settings,
            ClassLoader loader,Logger logger,long profileTimeout,int retries,
            long heartbeatTimeout,long reservationTtl,String instancePrefix,
            long reconnectTtl){
        ExecutorService executor=Executors.newFixedThreadPool(settings.isEnabled()?settings.getMaximumPoolSize():1);
        ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor();
        if(!settings.isEnabled()){
            QuickBuyRepository quick=new InMemoryQuickBuyRepository();
            ProfileLifecycleService profiles=new ProfileLifecycleService(
                    new UnavailableProgressionRepository(),new UnavailableStatisticsRepository(),quick,
                    scheduler,profileTimeout,retries,DatabaseOfflinePolicy.REJECT_TRACKED);
            InMemoryInstanceRegistry registry=new InMemoryInstanceRegistry();
            ReservationService reservations=new ReservationService();
            DistributedReconnectService reconnectSessions=new DistributedReconnectService();
            RoutingServices routing=new RoutingServices(new InMemoryRoutingCoordinator(
                    registry,reservations,heartbeatTimeout,reservationTtl),
                    new InMemoryInstancePublisher(registry),new InMemoryReconnectCoordinator(
                    reconnectSessions,registry,reservations,heartbeatTimeout,reservationTtl),
                    instancePrefix,reconnectTtl);
            scheduleRoutingCleanup(scheduler,routing);
            return new PersistenceRuntime(executor,scheduler,null,quick,profiles,
                    new InMemoryMatchPersistence(),routing,
                    new InMemoryCoinTransactionRepository());
        }
        HikariDataSource source=DataSourceFactory.create(settings);
        CompletableFuture<Void> ready=CompletableFuture.runAsync(()->{
            try{new MigrationRunner(source,loader).migrate();}
            catch(Exception failure){logger.log(Level.SEVERE,"Migrazioni ChickenWars fallite",failure);throw new PersistenceException("Migrazioni fallite",failure);}},executor);
        QuickBuyRepository quick=new DeferredQuickBuyRepository(ready.thenApply(ignored->new JdbcQuickBuyRepository(source,executor)));
        ProgressionRepository progression=new DeferredProgressionRepository(ready.thenApply(ignored->new JdbcProgressionRepository(source,executor)));
        StatisticsRepository statistics=new DeferredStatisticsRepository(ready.thenApply(ignored->new JdbcStatisticsRepository(source,executor)));
        CoinTransactionRepository coins=new DeferredCoinTransactionRepository(
                ready.thenApply(ignored->new JdbcCoinTransactionRepository(source,executor)));
        ProfileLifecycleService profiles=new ProfileLifecycleService(
                progression,statistics,
                quick,scheduler,profileTimeout,retries,DatabaseOfflinePolicy.REJECT_TRACKED);
        RoutingServices routing=new RoutingServices(
                new DeferredRoutingCoordinator(ready.thenApply(ignored->new JdbcRoutingCoordinator(source,executor,heartbeatTimeout,reservationTtl))),
                new DeferredInstancePublisher(ready.thenApply(ignored->new JdbcInstancePublisher(source,executor))),
                new DeferredReconnectCoordinator(ready.thenApply(ignored->new JdbcReconnectCoordinator(source,executor,heartbeatTimeout,reservationTtl))),
                instancePrefix,reconnectTtl);
        scheduleRoutingCleanup(scheduler,routing);
        return new PersistenceRuntime(executor,scheduler,source,quick,profiles,
                new DeferredMatchPersistence(ready.thenApply(ignored->new JdbcMatchPersistence(source,executor))),routing,coins);
    }
    public QuickBuyRepository getQuickBuy(){return quickBuy;}
    public ProfileLifecycleService getProfiles(){return profiles;}
    public MatchPersistence getMatches(){return matches;}
    public ProgressionServices services(it.legacynetwork.chickenwars.progression.MatchRewardPolicy rewards,it.legacynetwork.chickenwars.progression.ExperiencePolicy experience){return new ProgressionServices(profiles,matches,coins,rewards,experience);}
    public RoutingServices routing(){return routing;}
    private static void scheduleRoutingCleanup(ScheduledExecutorService scheduler,
            final RoutingServices routing){scheduler.scheduleAtFixedRate(new Runnable(){
        @Override public void run(){long now=System.currentTimeMillis();routing.getCoordinator().cleanup(now);routing.getReconnect().cleanup(now);}
    },10L,10L,TimeUnit.SECONDS);}
    public void close(){await(profiles.shutdown());await(quickBuy.close());await(matches.close());await(coins.close());
        scheduler.shutdown();databaseExecutor.shutdown();try{databaseExecutor.awaitTermination(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        if(dataSource!=null)dataSource.close();}
    private void await(java.util.concurrent.CompletionStage<?> operation){try{operation.toCompletableFuture().get(5,TimeUnit.SECONDS);}catch(Exception ignored){/* shutdown best effort e limitato */}}
}
