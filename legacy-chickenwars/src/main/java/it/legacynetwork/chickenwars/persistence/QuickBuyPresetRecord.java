package it.legacynetwork.chickenwars.persistence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Preset Quick Buy persistibile e indipendente dall'implementazione database.
 */
public final class QuickBuyPresetRecord {

    private final UUID playerId;
    private final String presetId;
    private final String displayName;
    private final boolean selected;
    private final Map<Integer, String> slots;

    public QuickBuyPresetRecord(UUID playerId, String presetId,
                                String displayName, boolean selected,
                                Map<Integer, String> slots) {
        if (playerId == null) {
            throw new IllegalArgumentException("UUID giocatore mancante");
        }
        if (presetId == null || presetId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID preset mancante");
        }
        this.playerId = playerId;
        this.presetId = presetId.trim();
        this.displayName = displayName == null || displayName.trim().isEmpty()
                ? this.presetId : displayName.trim();
        Map<Integer, String> copy = new LinkedHashMap<Integer, String>();
        if (slots != null) {
            for (Map.Entry<Integer, String> entry : slots.entrySet()) {
                Integer slot = entry.getKey();
                String itemKey = entry.getValue();
                if (slot != null && slot >= 0 && slot < 54
                        && itemKey != null && !itemKey.trim().isEmpty()) {
                    copy.put(slot, itemKey.trim());
                }
            }
        }
        this.selected = selected;
        this.slots = Collections.unmodifiableMap(copy);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPresetId() {
        return presetId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSelected() {
        return selected;
    }

    public Map<Integer, String> getSlots() {
        return slots;
    }
}
