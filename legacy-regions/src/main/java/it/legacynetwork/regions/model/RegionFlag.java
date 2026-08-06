package it.legacynetwork.regions.model;

import java.util.Locale;

public enum RegionFlag {
    BUILD,
    BLOCK_BREAK,
    BLOCK_PLACE,
    PVP,
    DAMAGE,
    FALL_DAMAGE,
    HUNGER,
    ITEM_DROP,
    ITEM_PICKUP,
    EXPLOSIONS,
    FIRE_SPREAD,
    MOB_SPAWN,
    PROJECTILES,
    VEHICLE_USE,
    INTERACT;

    public static RegionFlag fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public boolean isSpecific() {
        return this == BLOCK_BREAK
                || this == BLOCK_PLACE
                || this == FALL_DAMAGE
                || this == PVP;
    }

    public RegionFlag getGeneralFlag() {
        if (this == BLOCK_BREAK || this == BLOCK_PLACE) {
            return BUILD;
        }
        if (this == FALL_DAMAGE || this == PVP) {
            return DAMAGE;
        }
        return null;
    }

    public String getPermissionKey() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
