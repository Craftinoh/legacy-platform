package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.statistics.ModeStatistics;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Carica/salva profili con timeout e retry limitati, senza API Bukkit. */
public final class ProfileLifecycleService {
    private final ProgressionRepository progression;
    private final StatisticsRepository statistics;
    private final QuickBuyRepository quickBuy;
    private final ScheduledExecutorService scheduler;
    private final long timeoutMillis;
    private final int maximumRetries;
    private final DatabaseOfflinePolicy offlinePolicy;
    private final Map<UUID,PlayerProfile> loaded=new ConcurrentHashMap<UUID,PlayerProfile>();

    public ProfileLifecycleService(ProgressionRepository progression,
            StatisticsRepository statistics,QuickBuyRepository quickBuy,
            ScheduledExecutorService scheduler,long timeoutMillis,int maximumRetries,
            DatabaseOfflinePolicy offlinePolicy){
        if(progression==null||statistics==null||quickBuy==null||scheduler==null
                ||timeoutMillis<=0||maximumRetries<0||offlinePolicy==null)throw new IllegalArgumentException("Lifecycle profilo incompleto");
        this.progression=progression;this.statistics=statistics;this.quickBuy=quickBuy;
        this.scheduler=scheduler;this.timeoutMillis=timeoutMillis;
        this.maximumRetries=maximumRetries;this.offlinePolicy=offlinePolicy;
    }
    public CompletionStage<ProfileLoadResult> load(UUID playerId){return attempt(playerId,0);}
    private CompletionStage<ProfileLoadResult> attempt(final UUID id,final int retry){
        final CompletableFuture<ProfileLoadResult> result=new CompletableFuture<ProfileLoadResult>();
        CompletableFuture<PlayerProgressRecord> p=progression.load(id).toCompletableFuture();
        CompletableFuture<List<ModeStatistics>> s=statistics.load(id).toCompletableFuture();
        CompletableFuture<List<QuickBuyPresetRecord>> q=quickBuy.loadPresets(id).toCompletableFuture();
        CompletableFuture.allOf(p,s,q).whenComplete((ignored,failure)->{
            if(failure==null){PlayerProgressRecord record=p.join();if(record==null)record=new PlayerProgressRecord(id,0,0,System.currentTimeMillis());
                PlayerProfile profile=new PlayerProfile(id,record,s.join()==null?Collections.<ModeStatistics>emptyList():s.join(),q.join()==null?Collections.<QuickBuyPresetRecord>emptyList():q.join());
                if(result.complete(ProfileLoadResult.loaded(profile))){
                    loaded.put(id,profile);
                }
            }else if(retry<maximumRetries){attempt(id,retry+1).whenComplete((value,nextFailure)->{if(nextFailure==null)result.complete(value);else result.complete(ProfileLoadResult.failed(ProfileLoadResult.Status.OFFLINE));});
            }else result.complete(ProfileLoadResult.failed(ProfileLoadResult.Status.OFFLINE));
        });
        scheduler.schedule(()->result.complete(ProfileLoadResult.failed(ProfileLoadResult.Status.TIMED_OUT)),timeoutMillis,TimeUnit.MILLISECONDS);
        return result;
    }
    public boolean mayEnterTracked(UUID playerId){return loaded.containsKey(playerId)
            ||offlinePolicy==DatabaseOfflinePolicy.UNTRACKED_DEGRADED;}
    public CompletionStage<Void> saveAndUnload(UUID id){
        loaded.remove(id);return CompletableFuture.completedFuture(null);
    }
    public PlayerProfile get(UUID id){return loaded.get(id);}
    public CompletionStage<Void> shutdown(){
        CompletableFuture<?>[] saves=new CompletableFuture<?>[loaded.size()];int i=0;
        for(UUID id:loaded.keySet())saves[i++]=saveAndUnload(id).toCompletableFuture();
        return CompletableFuture.allOf(saves);
    }
}
