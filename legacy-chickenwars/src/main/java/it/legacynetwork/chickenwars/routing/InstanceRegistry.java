package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.List;

public interface InstanceRegistry {
    void heartbeat(GameInstanceDescriptor descriptor);
    GameInstanceDescriptor find(String instanceId);
    List<GameInstanceDescriptor> list(MatchMode mode);
    void remove(String instanceId);
    void clear();
}
