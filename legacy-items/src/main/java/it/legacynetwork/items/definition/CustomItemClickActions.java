package it.legacynetwork.items.definition;

import java.util.Collections;
import java.util.List;

public final class CustomItemClickActions {
    private final int cooldownMillis;
    private final boolean cancelEvent;
    private final List<CustomItemAction> execute;

    public CustomItemClickActions(int cooldownMillis,
                                   boolean cancelEvent,
                                   List<CustomItemAction> execute) {
        this.cooldownMillis = cooldownMillis;
        this.cancelEvent = cancelEvent;
        this.execute = execute != null
                ? Collections.unmodifiableList(execute)
                : Collections.emptyList();
    }

    public int getCooldownMillis() {
        return cooldownMillis;
    }

    public boolean isCancelEvent() {
        return cancelEvent;
    }

    public List<CustomItemAction> getExecute() {
        return execute;
    }
}
