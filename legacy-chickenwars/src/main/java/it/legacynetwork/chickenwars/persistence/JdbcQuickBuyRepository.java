package it.legacynetwork.chickenwars.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

/** Persistenza Quick Buy asincrona e transazionale. */
public final class JdbcQuickBuyRepository implements QuickBuyRepository {
    private final DataSource dataSource; private final Executor executor;
    public JdbcQuickBuyRepository(DataSource dataSource, Executor executor) {
        if (dataSource == null || executor == null) throw new IllegalArgumentException("JDBC incompleto");
        this.dataSource=dataSource; this.executor=executor;
    }
    @Override public CompletionStage<List<QuickBuyPresetRecord>> loadPresets(final UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try { return loadNow(id); } catch(SQLException e) { throw failure(e); }
        }, executor);
    }
    private List<QuickBuyPresetRecord> loadNow(UUID id) throws SQLException {
        Map<String,QuickBuyPresetRecord> result=new LinkedHashMap<String,QuickBuyPresetRecord>();
        try(Connection c=dataSource.getConnection(); PreparedStatement p=c.prepareStatement(
                "SELECT preset_id,display_name,selected FROM cw_quick_buy_presets WHERE player_id=? ORDER BY preset_id")) {
            p.setString(1,id.toString()); try(ResultSet r=p.executeQuery()) {
                while(r.next()) { String preset=r.getString(1); result.put(preset,
                        new QuickBuyPresetRecord(id,preset,r.getString(2),r.getBoolean(3),loadSlots(c,id,preset))); }
            }
        } return new ArrayList<QuickBuyPresetRecord>(result.values());
    }
    private Map<Integer,String> loadSlots(Connection c,UUID id,String preset)throws SQLException{
        Map<Integer,String> slots=new LinkedHashMap<Integer,String>();
        try(PreparedStatement p=c.prepareStatement("SELECT slot_number,item_id FROM cw_quick_buy_slots WHERE player_id=? AND preset_id=? ORDER BY slot_number")){
            p.setString(1,id.toString());p.setString(2,preset);try(ResultSet r=p.executeQuery()){while(r.next())slots.put(Integer.valueOf(r.getInt(1)),r.getString(2));}
        }return slots;
    }
    @Override public CompletionStage<Void> savePreset(final QuickBuyPresetRecord value) {
        return run(() -> saveNow(value));
    }
    private void saveNow(QuickBuyPresetRecord value)throws SQLException{
        try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{
            deleteRows(c,value.getPlayerId(),value.getPresetId());
            try(PreparedStatement p=c.prepareStatement("INSERT INTO cw_quick_buy_presets(player_id,preset_id,display_name,selected) VALUES(?,?,?,?)")){
                p.setString(1,value.getPlayerId().toString());p.setString(2,value.getPresetId());p.setString(3,value.getDisplayName());p.setBoolean(4,value.isSelected());p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO cw_quick_buy_slots(player_id,preset_id,slot_number,item_id) VALUES(?,?,?,?)")){
                for(Map.Entry<Integer,String> e:value.getSlots().entrySet()){p.setString(1,value.getPlayerId().toString());p.setString(2,value.getPresetId());p.setInt(3,e.getKey().intValue());p.setString(4,e.getValue());p.addBatch();}p.executeBatch();}
            c.commit();}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
    }
    @Override public CompletionStage<Void> deletePreset(final UUID id,final String preset){return run(()->{try(Connection c=dataSource.getConnection()){deleteRows(c,id,preset);}});}
    private void deleteRows(Connection c,UUID id,String preset)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("DELETE FROM cw_quick_buy_slots WHERE player_id=? AND preset_id=?")){p.setString(1,id.toString());p.setString(2,preset);p.executeUpdate();}
        try(PreparedStatement p=c.prepareStatement("DELETE FROM cw_quick_buy_presets WHERE player_id=? AND preset_id=?")){p.setString(1,id.toString());p.setString(2,preset);p.executeUpdate();}
    }
    @Override public CompletionStage<Void> selectPreset(final UUID id,final String preset){return run(()->{try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{
        try(PreparedStatement p=c.prepareStatement("UPDATE cw_quick_buy_presets SET selected=FALSE WHERE player_id=?")){p.setString(1,id.toString());p.executeUpdate();}
        int changed;try(PreparedStatement p=c.prepareStatement("UPDATE cw_quick_buy_presets SET selected=TRUE WHERE player_id=? AND preset_id=?")){p.setString(1,id.toString());p.setString(2,preset);changed=p.executeUpdate();}
        if(changed!=1)throw new SQLException("Preset non trovato: "+preset);c.commit();}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}});}
    private CompletionStage<Void> run(final SqlAction action){return CompletableFuture.runAsync(()->{try{action.run();}catch(SQLException e){throw failure(e);}},executor);}
    private PersistenceException failure(SQLException e){return new PersistenceException("Quick Buy JDBC fallito",e);}
    private interface SqlAction{void run()throws SQLException;}
    @Override public CompletionStage<Void> close(){return CompletableFuture.completedFuture(null);}
}
