package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryInstanceRegistry implements InstanceRegistry {
    private final Map<String, GameInstanceDescriptor> instances =
            new ConcurrentHashMap<String, GameInstanceDescriptor>();
    @Override public void heartbeat(GameInstanceDescriptor descriptor) {
        if (descriptor == null) throw new IllegalArgumentException("Heartbeat mancante");
        instances.put(descriptor.getInstanceId(), descriptor);
    }
    @Override public GameInstanceDescriptor find(String id) { return instances.get(id); }
    @Override public List<GameInstanceDescriptor> list(MatchMode mode) {
        List<GameInstanceDescriptor> result = new ArrayList<GameInstanceDescriptor>();
        for (GameInstanceDescriptor value : instances.values()) {
            if (value.getMode() == mode) result.add(value);
        }
        Collections.sort(result, new Comparator<GameInstanceDescriptor>() {
            @Override public int compare(GameInstanceDescriptor left,
                                         GameInstanceDescriptor right) {
                int load = Integer.compare(right.getPlayers(), left.getPlayers());
                return load != 0 ? load : left.getInstanceId().compareTo(right.getInstanceId());
            }
        });
        return result;
    }
    @Override public void remove(String id) { instances.remove(id); }
    @Override public void clear() { instances.clear(); }
}
