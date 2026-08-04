package it.legacynetwork.items.definition;

public final class CustomItemFlags {
    private final boolean preventDrop;
    private final boolean preventMove;
    private final boolean preventSwap;
    private final boolean preventConsume;
    private final boolean preventDamage;
    private final boolean replaceExisting;
    private final boolean unique;

    public CustomItemFlags(boolean preventDrop,
                           boolean preventMove,
                           boolean preventSwap,
                           boolean preventConsume,
                           boolean preventDamage,
                           boolean replaceExisting,
                           boolean unique) {
        this.preventDrop = preventDrop;
        this.preventMove = preventMove;
        this.preventSwap = preventSwap;
        this.preventConsume = preventConsume;
        this.preventDamage = preventDamage;
        this.replaceExisting = replaceExisting;
        this.unique = unique;
    }

    public boolean isPreventDrop() {
        return preventDrop;
    }

    public boolean isPreventMove() {
        return preventMove;
    }

    public boolean isPreventSwap() {
        return preventSwap;
    }

    public boolean isPreventConsume() {
        return preventConsume;
    }

    public boolean isPreventDamage() {
        return preventDamage;
    }

    public boolean isReplaceExisting() {
        return replaceExisting;
    }

    public boolean isUnique() {
        return unique;
    }
}
