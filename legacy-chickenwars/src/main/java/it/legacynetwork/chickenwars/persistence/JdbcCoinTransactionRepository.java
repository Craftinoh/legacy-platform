package it.legacynetwork.chickenwars.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

/** Ledger JDBC serializzato dalla riga profilo e protetto da chiave unica. */
public final class JdbcCoinTransactionRepository implements CoinTransactionRepository {
    private final DataSource dataSource;private final Executor executor;
    public JdbcCoinTransactionRepository(DataSource d,Executor e){dataSource=d;executor=e;}
    @Override public CompletionStage<CoinTransactionRecord> credit(UUID id,long amount,String key){if(amount<=0)return failed(new IllegalArgumentException("Credito non positivo"));return mutate(id,amount,key);}
    @Override public CompletionStage<CoinTransactionRecord> debit(UUID id,long amount,String key){if(amount<=0)return failed(new IllegalArgumentException("Addebito non positivo"));return mutate(id,-amount,key);}
    private CompletionStage<CoinTransactionRecord> mutate(final UUID id,final long amount,final String key){return CompletableFuture.supplyAsync(()->{try{return mutateNow(id,amount,key);}catch(SQLException e){throw new PersistenceException("Ledger JDBC fallito",e);}},executor);}
    private CoinTransactionRecord mutateNow(UUID id,long amount,String key)throws SQLException{try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{
        CoinTransactionRecord prior=findByKey(c,key);if(prior!=null){if(!prior.getPlayerId().equals(id)||prior.getAmount()!=amount)throw new SQLException("Chiave idempotenza incoerente");c.rollback();return prior;}
        long balance=0;try(PreparedStatement p=c.prepareStatement("SELECT coins FROM cw_player_progress WHERE player_id=? FOR UPDATE")){p.setString(1,id.toString());try(ResultSet r=p.executeQuery()){if(r.next())balance=r.getLong(1);else{try(PreparedStatement i=c.prepareStatement("INSERT INTO cw_player_progress(player_id,total_experience,coins,updated_at) VALUES(?,0,0,?)")){i.setString(1,id.toString());i.setLong(2,System.currentTimeMillis());i.executeUpdate();}}}}
        if(amount<0&&balance < -amount)throw new InsufficientCoinsException();long next=amount>0&&Long.MAX_VALUE-balance<amount?Long.MAX_VALUE:balance+amount;
        try(PreparedStatement p=c.prepareStatement("UPDATE cw_player_progress SET coins=?,updated_at=? WHERE player_id=?")){p.setLong(1,next);p.setLong(2,System.currentTimeMillis());p.setString(3,id.toString());p.executeUpdate();}
        CoinTransactionRecord record=new CoinTransactionRecord(id,amount,next,key,System.currentTimeMillis());try(PreparedStatement p=c.prepareStatement("INSERT INTO cw_coin_ledger(transaction_id,player_id,amount,balance_after,idempotency_key,created_at) VALUES(?,?,?,?,?,?)")){p.setString(1,UUID.randomUUID().toString());p.setString(2,id.toString());p.setLong(3,amount);p.setLong(4,next);p.setString(5,key);p.setLong(6,record.getCreatedAtEpochMillis());p.executeUpdate();}c.commit();return record;
    }catch(RuntimeException e){c.rollback();throw e;}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}}
    private CoinTransactionRecord findByKey(Connection c,String key)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT player_id,amount,balance_after,created_at FROM cw_coin_ledger WHERE idempotency_key=?")){p.setString(1,key);try(ResultSet r=p.executeQuery()){return r.next()?new CoinTransactionRecord(UUID.fromString(r.getString(1)),r.getLong(2),r.getLong(3),key,r.getLong(4)):null;}}}
    @Override public CompletionStage<Long> balance(final UUID id){return CompletableFuture.supplyAsync(()->{try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("SELECT coins FROM cw_player_progress WHERE player_id=?")){p.setString(1,id.toString());try(ResultSet r=p.executeQuery()){return Long.valueOf(r.next()?r.getLong(1):0L);}}catch(SQLException e){throw new PersistenceException("Saldo JDBC fallito",e);}},executor);}
    @Override public CompletionStage<List<CoinTransactionRecord>> history(final UUID id){return CompletableFuture.supplyAsync(()->{List<CoinTransactionRecord> out=new ArrayList<CoinTransactionRecord>();try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("SELECT amount,balance_after,idempotency_key,created_at FROM cw_coin_ledger WHERE player_id=? ORDER BY created_at")){p.setString(1,id.toString());try(ResultSet r=p.executeQuery()){while(r.next())out.add(new CoinTransactionRecord(id,r.getLong(1),r.getLong(2),r.getString(3),r.getLong(4)));}}catch(SQLException e){throw new PersistenceException("Storico JDBC fallito",e);}return out;},executor);}
    private <T> CompletionStage<T> failed(Throwable t){CompletableFuture<T> f=new CompletableFuture<T>();f.completeExceptionally(t);return f;}
    @Override public CompletionStage<Void> close(){return CompletableFuture.completedFuture(null);}
}
