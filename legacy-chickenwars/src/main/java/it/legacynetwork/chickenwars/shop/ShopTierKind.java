package it.legacynetwork.chickenwars.shop;

import java.util.Locale;

/**
 * Natura di un articolo rispetto allo stato autorevole dell'equipaggiamento.
 *
 * <p>Determina quale progressione viene consultata per rifiutare l'acquisto di
 * un tier gia' posseduto o inferiore, e quale parte del loadout viene
 * riapplicata dopo respawn e reconnect.</p>
 */
public enum ShopTierKind {

    /** Articolo consumabile: blocchi, pozioni, utility. */
    CONSUMABLE,
    /** Leggings e stivali, permanenti per la partita. */
    ARMOR,
    /** Spada, azzerata a ogni morte. */
    SWORD,
    /** Piccone, con downgrade dopo la morte. */
    PICKAXE,
    /** Ascia, con downgrade dopo la morte. */
    AXE,
    /** Cesoie, permanenti e senza livelli. */
    SHEARS;

    /**
     * Converte un nome, ignorando maiuscole e spazi.
     *
     * @return la natura, oppure {@code null} se il nome non corrisponde
     */
    public static ShopTierKind fromString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return CONSUMABLE;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (ShopTierKind kind : values()) {
            if (kind.name().equals(normalized)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * Indica se l'articolo viene gestito dal servizio equipaggiamento anziche'
     * consegnato direttamente nell'inventario.
     */
    public boolean isEquipment() {
        return this != CONSUMABLE;
    }

    /**
     * Indica se la progressione richiede un livello esplicito.
     */
    public boolean requiresLevel() {
        return this == ARMOR || this == SWORD
                || this == PICKAXE || this == AXE;
    }
}
