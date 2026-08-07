package it.legacynetwork.chickenwars.persistence;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Accoda le operazioni finche' migrazioni e datasource sono pronti. */
public final class DeferredQuickBuyRepository implements QuickBuyRepository {
    private final CompletionStage<QuickBuyRepository> ready;
    public DeferredQuickBuyRepository(CompletionStage<QuickBuyRepository> ready){this.ready=ready;}
    @Override public CompletionStage<List<QuickBuyPresetRecord>> loadPresets(final UUID id){return ready.thenCompose(r->r.loadPresets(id));}
    @Override public CompletionStage<Void> savePreset(final QuickBuyPresetRecord p){return ready.thenCompose(r->r.savePreset(p));}
    @Override public CompletionStage<Void> deletePreset(final UUID id,final String p){return ready.thenCompose(r->r.deletePreset(id,p));}
    @Override public CompletionStage<Void> selectPreset(final UUID id,final String p){return ready.thenCompose(r->r.selectPreset(id,p));}
    @Override public CompletionStage<Void> close(){return ready.thenCompose(QuickBuyRepository::close);}
}
