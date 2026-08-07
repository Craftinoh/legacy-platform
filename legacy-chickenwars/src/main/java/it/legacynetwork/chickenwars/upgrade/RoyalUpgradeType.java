package it.legacynetwork.chickenwars.upgrade;

import java.util.Locale;

/**
 * Potenziamenti acquistabili per la Gallina Reale della squadra.
 */
public enum RoyalUpgradeType {

    /** Aumenta la salute massima della gallina. */
    ROYAL_VITALITY,
    /** Riduce il danno subito dalla gallina. */
    ROYAL_ARMOR,
    /** Rafforza le difese e gli avvisi legati alla gallina. */
    ROYAL_GUARD;

    /**
     * Converte un nome, ignorando maiuscole, spazi e trattini.
     *
     * @return il tipo, oppure {@code null} se il nome non corrisponde
     */
    public static RoyalUpgradeType fromString(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        for (RoyalUpgradeType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    public String getNameKey() {
        return "upgrade.royal." + name().toLowerCase(Locale.ROOT) + ".name";
    }

    public String getLoreKey() {
        return "upgrade.royal." + name().toLowerCase(Locale.ROOT) + ".lore";
    }
}
