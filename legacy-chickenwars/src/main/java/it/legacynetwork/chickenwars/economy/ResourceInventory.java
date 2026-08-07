package it.legacynetwork.chickenwars.economy;

import it.legacynetwork.chickenwars.model.ResourceType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter per l'inventario delle risorse, testabile senza Bukkit.
 */
public interface ResourceInventory {

    int count(ResourceType type);

    Map<ResourceType, Integer> withdrawAll();

    int deposit(ResourceType type, int amount);

    boolean hasFreeSlot();

    static ResourceInventory with(Map<ResourceType, Integer> initial) {
        return new MapInventory(new HashMap<>(initial));
    }

    static ResourceInventory empty() {
        return new MapInventory(new HashMap<ResourceType, Integer>());
    }

    final class MapInventory implements ResourceInventory {
        private final Map<ResourceType, Integer> storage;
        private int maxSlots = 36;
        private int usedSlots;

        MapInventory(Map<ResourceType, Integer> initial) {
            this.storage = new HashMap<>(initial);
            int used = 0;
            for (Integer v : initial.values()) {
                if (v != null && v.intValue() > 0) used++;
            }
            this.usedSlots = used;
        }

        public void setMaxSlots(int slots) { this.maxSlots = slots; }
        public void setUsedSlots(int used) { this.usedSlots = used; }

        @Override
        public int count(ResourceType type) {
            Integer v = storage.get(type);
            return v == null ? 0 : v.intValue();
        }

        @Override
        public Map<ResourceType, Integer> withdrawAll() {
            Map<ResourceType, Integer> removed = new EnumMap<>(ResourceType.class);
            for (Map.Entry<ResourceType, Integer> e : storage.entrySet()) {
                if (e.getValue() > 0) {
                    removed.put(e.getKey(), e.getValue());
                }
            }
            storage.clear();
            usedSlots = 0;
            return removed;
        }

        @Override
        public int deposit(ResourceType type, int amount) {
            if (usedSlots >= maxSlots) return amount;
            storage.put(type, storage.getOrDefault(type, 0) + amount);
            return 0;
        }

        @Override
        public boolean hasFreeSlot() {
            return usedSlots < maxSlots;
        }
    }
}
