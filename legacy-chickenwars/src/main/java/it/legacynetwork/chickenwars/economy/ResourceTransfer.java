package it.legacynetwork.chickenwars.economy;

import it.legacynetwork.chickenwars.model.ResourceType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Riepilogo di un trasferimento di risorse dalla vittima al suo uccisore.
 */
public final class ResourceTransfer {

    private static final ResourceTransfer EMPTY = new ResourceTransfer(
            Collections.<ResourceType, Integer>emptyMap(),
            Collections.<ResourceType, Integer>emptyMap());

    private final Map<ResourceType, Integer> delivered;
    private final Map<ResourceType, Integer> queued;

    ResourceTransfer(Map<ResourceType, Integer> delivered,
                     Map<ResourceType, Integer> queued) {
        EnumMap<ResourceType, Integer> d = new EnumMap<>(ResourceType.class);
        d.putAll(delivered);
        this.delivered = Collections.unmodifiableMap(d);
        EnumMap<ResourceType, Integer> q = new EnumMap<>(ResourceType.class);
        q.putAll(queued);
        this.queued = Collections.unmodifiableMap(q);
    }

    /**
     * Trasferimento senza alcun movimento, per morti prive di uccisore valido.
     */
    public static ResourceTransfer empty() {
        return EMPTY;
    }

    /** Quantita' entrate subito nell'inventario dell'uccisore. */
    public Map<ResourceType, Integer> getDelivered() {
        return delivered;
    }

    /** Quantita' rimaste in coda per mancanza di spazio. */
    public Map<ResourceType, Integer> getQueued() {
        return queued;
    }

    public boolean isEmpty() {
        return delivered.isEmpty() && queued.isEmpty();
    }

    public boolean hasQueued() {
        return !queued.isEmpty();
    }

    /**
     * Totale complessivo delle unita' spostate, consegnate o accodate.
     */
    public int getTotalAmount() {
        int total = 0;
        for (Integer amount : delivered.values()) {
            total += amount.intValue();
        }
        for (Integer amount : queued.values()) {
            total += amount.intValue();
        }
        return total;
    }

    /**
     * Descrizione compatta delle sole risorse consegnate o accodate.
     *
     * @param separator separatore tra le voci
     * @return il riepilogo, vuoto se non e' stato spostato nulla
     */
    public String describe(String separator) {
        StringBuilder builder = new StringBuilder();
        for (ResourceType type : ResourceType.values()) {
            int amount = valueOf(delivered, type) + valueOf(queued, type);
            if (amount <= 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(type.getColor()).append(amount).append(' ')
                    .append(type.name().toLowerCase());
        }
        return builder.toString();
    }

    private int valueOf(Map<ResourceType, Integer> source, ResourceType type) {
        Integer value = source.get(type);
        return value == null ? 0 : value.intValue();
    }
}
