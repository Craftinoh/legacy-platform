package it.legacynetwork.chickenwars.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CoinLedgerTest {
    @Test void creditDebitHistoryAndIdempotency(){InMemoryCoinTransactionRepository r=new InMemoryCoinTransactionRepository();UUID id=UUID.randomUUID();r.credit(id,100,"credit").toCompletableFuture().join();r.credit(id,100,"credit").toCompletableFuture().join();r.debit(id,40,"debit").toCompletableFuture().join();assertEquals(60,r.balance(id).toCompletableFuture().join());assertEquals(2,r.history(id).toCompletableFuture().join().size());}
    @Test void neverAllowsNegativeBalance(){InMemoryCoinTransactionRepository r=new InMemoryCoinTransactionRepository();CompletionException error=assertThrows(CompletionException.class,()->r.debit(UUID.randomUUID(),1,"x").toCompletableFuture().join());assertTrue(error.getCause() instanceof InsufficientCoinsException);}
    @Test void concurrentCreditsAreNotLost(){InMemoryCoinTransactionRepository r=new InMemoryCoinTransactionRepository();UUID id=UUID.randomUUID();IntStream.range(0,100).parallel().forEach(i->r.credit(id,1,"k"+i).toCompletableFuture().join());assertEquals(100,r.balance(id).toCompletableFuture().join());}
    @Test void rejectsKeyReuseWithDifferentData(){InMemoryCoinTransactionRepository r=new InMemoryCoinTransactionRepository();UUID id=UUID.randomUUID();r.credit(id,1,"same").toCompletableFuture().join();assertThrows(CompletionException.class,()->r.credit(id,2,"same").toCompletableFuture().join());}
}
