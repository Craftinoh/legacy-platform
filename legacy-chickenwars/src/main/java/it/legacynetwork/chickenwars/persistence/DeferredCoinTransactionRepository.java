package it.legacynetwork.chickenwars.persistence;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class DeferredCoinTransactionRepository implements CoinTransactionRepository {
    private final CompletionStage<CoinTransactionRepository> ready;
    public DeferredCoinTransactionRepository(CompletionStage<CoinTransactionRepository> ready){this.ready=ready;}
    @Override public CompletionStage<CoinTransactionRecord> credit(final UUID p,final long a,final String k){return ready.thenCompose(r->r.credit(p,a,k));}
    @Override public CompletionStage<CoinTransactionRecord> debit(final UUID p,final long a,final String k){return ready.thenCompose(r->r.debit(p,a,k));}
    @Override public CompletionStage<Long> balance(final UUID p){return ready.thenCompose(r->r.balance(p));}
    @Override public CompletionStage<List<CoinTransactionRecord>> history(final UUID p){return ready.thenCompose(r->r.history(p));}
    @Override public CompletionStage<Void> close(){return ready.thenCompose(CoinTransactionRepository::close);}
}
