package it.legacynetwork.chickenwars.generator;

import it.legacynetwork.chickenwars.model.ResourceType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Identità runtime degli item naturali, compatibile 1.8 senza PDC. */
public final class GeneratedResourceRegistry {
    private static final class Entry{private final String matchId;private final ResourceType type;Entry(String matchId,ResourceType type){this.matchId=matchId;this.type=type;}}
    private final Map<UUID,Entry> entries=new HashMap<UUID,Entry>();
    public synchronized void register(UUID entityId,String matchId,ResourceType type){if(entityId!=null&&matchId!=null&&type!=null)entries.put(entityId,new Entry(matchId,type));}
    public synchronized boolean contains(UUID entityId,String matchId){Entry entry=entries.get(entityId);return entry!=null&&entry.matchId.equals(matchId);}
    public synchronized ResourceType consume(UUID entityId,String matchId){Entry entry=entries.get(entityId);if(entry==null||!entry.matchId.equals(matchId))return null;entries.remove(entityId);return entry.type;}
    public synchronized ResourceType pickup(UUID entityId,String matchId,
                                            boolean fullyCollected){Entry entry=entries.get(entityId);if(entry==null||!entry.matchId.equals(matchId))return null;if(fullyCollected)entries.remove(entityId);return entry.type;}
    public synchronized void clearMatch(String matchId){Iterator<Map.Entry<UUID,Entry>> iterator=entries.entrySet().iterator();while(iterator.hasNext())if(iterator.next().getValue().matchId.equals(matchId))iterator.remove();}
    public synchronized void clear(){entries.clear();}
}
