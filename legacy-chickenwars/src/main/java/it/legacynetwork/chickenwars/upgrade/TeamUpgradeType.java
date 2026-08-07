package it.legacynetwork.chickenwars.upgrade;

import java.util.Locale;

/**
 * Upgrade acquistabili da una squadra.
 *
 * <p>Il tipo determina come l'effetto viene applicato: incantesimo, effetto
 * pozione permanente o effetto legato alla regione della base.</p>
 */
public enum TeamUpgradeType {

    /** Protezione applicata a tutti i pezzi di armatura. */
    PROTECTION(Application.ENCHANT),
    /** Affilatezza applicata alla spada, base e acquistata. */
    SHARPNESS(Application.ENCHANT),
    /** Velocita' di scavo costante per i membri della squadra. */
    HASTE(Application.TEAM_EFFECT),
    /** Rigenerazione attiva soltanto dentro la propria base. */
    HEAL_POOL(Application.BASE_EFFECT);

    /** Modalita' con cui l'effetto raggiunge i giocatori. */
    public enum Application {
        /** Incantesimo applicato quando l'equipaggiamento viene ricreato. */
        ENCHANT,
        /** Effetto pozione mantenuto sui membri vivi. */
        TEAM_EFFECT,
        /** Effetto pozione applicato solo dentro la regione della base. */
        BASE_EFFECT
    }

    private final Application application;

    TeamUpgradeType(Application application) {
        this.application = application;
    }

    public Application getApplication() {
        return application;
    }

    /**
     * Converte un nome, ignorando maiuscole, spazi e trattini.
     *
     * @return il tipo, oppure {@code null} se il nome non corrisponde
     */
    public static TeamUpgradeType fromString(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        for (TeamUpgradeType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Chiave del nome localizzato.
     */
    public String getNameKey() {
        return "upgrade.team." + name().toLowerCase(Locale.ROOT) + ".name";
    }

    /**
     * Chiave della descrizione localizzata.
     */
    public String getLoreKey() {
        return "upgrade.team." + name().toLowerCase(Locale.ROOT) + ".lore";
    }
}
