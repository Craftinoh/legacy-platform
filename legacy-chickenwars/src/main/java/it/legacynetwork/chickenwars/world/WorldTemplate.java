package it.legacynetwork.chickenwars.world;

import java.util.Locale;

/**
 * Tipo di generazione usato per creare il mondo di un'arena.
 */
public enum WorldTemplate {

    /** Mondo completamente vuoto, con una piccola piattaforma allo spawn. */
    VOID,
    /** Superpiatto, comodo per prototipare una mappa. */
    FLAT,
    /** Generazione normale di Minecraft. */
    NORMAL,
    /**
     * Mondo gia' esistente, adottato cosi' com'e'.
     *
     * <p>Il plugin non ne tocca la generazione: viene caricato con le
     * impostazioni registrate nel suo {@code level.dat}, esattamente come farebbe
     * il server o un world manager esterno.</p>
     */
    EXISTING;

    /**
     * Converte un nome di template, ignorando maiuscole e spazi.
     *
     * @return il template, oppure {@code null} se il nome non corrisponde
     */
    public static WorldTemplate fromString(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (WorldTemplate template : values()) {
            if (template.name().equals(normalized)) {
                return template;
            }
        }
        return null;
    }

    /**
     * Indica se il template richiede un generatore personalizzato, da
     * riapplicare a ogni caricamento del mondo.
     */
    public boolean needsCustomGenerator() {
        return this == VOID;
    }

    /**
     * Indica se il template puo' essere usato per creare un mondo nuovo.
     *
     * <p>{@link #EXISTING} descrive un mondo adottato, non una generazione.</p>
     */
    public boolean isCreatable() {
        return this != EXISTING;
    }
}
