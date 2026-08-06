package it.legacynetwork.regions.model;

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

    public static RegionFlag fromString(String s) {
        if (s == null) {
            return null;
        }
        String normalized = s.toUpperCase().replace('-', '_');
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isSpecific() {
        return this == BLOCK_BREAK || this == BLOCK_PLACE || this == FALL_DAMAGE;
    }

    public RegionFlag getGeneralFlag() {
        if (this == BLOCK_BREAK || this == BLOCK_PLACE) {
            return BUILD;
        }
        if (this == FALL_DAMAGE) {
            return DAMAGE;
        }
        return null;
    }

    public String getPermissionKey() {
        return name().toLowerCase().replace('_', '-');
    }
}
