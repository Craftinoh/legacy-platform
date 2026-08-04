package it.legacynetwork.items.cooldown;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemCooldownService {
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public boolean isOnCooldown(UUID playerId, String itemId, String actionKey) {
        String key = buildKey(playerId, itemId, actionKey);
        Long endTime = cooldowns.get(key);
        if (endTime == null) {
            return false;
        }
        return System.nanoTime() < endTime;
    }

    public long getRemainingMillis(UUID playerId, String itemId, String actionKey) {
        String key = buildKey(playerId, itemId, actionKey);
        Long endTime = cooldowns.get(key);
        if (endTime == null) {
            return 0;
        }
        long remainingNanos = endTime - System.nanoTime();
        return Math.max(0, remainingNanos / 1_000_000);
    }

    public void setCooldown(UUID playerId, String itemId, String actionKey,
                             int millis) {
        if (millis <= 0) {
            return;
        }
        String key = buildKey(playerId, itemId, actionKey);
        cooldowns.put(key, System.nanoTime() + (millis * 1_000_000L));
    }

    public void clearPlayer(UUID playerId) {
        String prefix = playerId.toString() + "|";
        cooldowns.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    }

    public void clear() {
        cooldowns.clear();
    }

    private String buildKey(UUID playerId, String itemId, String actionKey) {
        return playerId.toString() + "|" + itemId + "|" + actionKey;
    }
}
