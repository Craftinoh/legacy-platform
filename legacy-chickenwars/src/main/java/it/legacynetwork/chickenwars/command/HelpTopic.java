package it.legacynetwork.chickenwars.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Sezioni della guida in gioco.
 *
 * <p>Ogni sezione corrisponde a una lista di righe nei file lingua, cosi' che i
 * testi restino fuori dal codice Java.</p>
 */
public enum HelpTopic {

    /** Panoramica iniziale con l'elenco delle sezioni. */
    GENERAL("help.main", null, "generale", "main", "indice"),
    // I valori successivi riservati allo staff usano ADMIN_PERMISSION.
    /** Comandi usati durante una partita. */
    GAME("help.game", null, "gioco", "game", "partita"),
    /** Come funziona il minigame: gallina, scudo, respawn. */
    CHICKEN("help.chicken", null, "gallina", "chicken", "meccaniche", "regole"),
    /** Comandi di gestione arene. */
    ADMIN("help.admin", "chickenwars.admin", "admin", "amministrazione"),
    /** Procedura guidata di creazione arena. */
    SETUP("help.setup", "chickenwars.admin", "setup", "editor"),
    /** Comandi di gestione mondi. */
    WORLDS("help.worlds", "chickenwars.admin", "mondi", "worlds", "world");

    /** Permesso richiesto dalle sezioni riservate allo staff. */
    public static final String ADMIN_PERMISSION = "chickenwars.admin";

    private final String messageKey;
    private final String permission;
    private final List<String> aliases;

    HelpTopic(String messageKey, String permission, String... aliases) {
        this.messageKey = messageKey;
        this.permission = permission;
        this.aliases = Collections.unmodifiableList(Arrays.asList(aliases));
    }

    /**
     * Individua la sezione corrispondente al testo digitato.
     *
     * <p>Ricerca pura per nome: un testo assente o vuoto non corrisponde a
     * nessuna sezione. La scelta di ripiegare sull'indice spetta al chiamante,
     * cosi' che questo metodo non possa mai innescare comportamenti a catena.</p>
     *
     * @return la sezione, oppure {@code null} se nessuna corrisponde
     */
    public static HelpTopic find(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (HelpTopic topic : values()) {
            if (topic.name().toLowerCase(Locale.ROOT).equals(normalized)
                    || topic.aliases.contains(normalized)) {
                return topic;
            }
        }
        return null;
    }

    /**
     * Indica se la sezione e' riservata allo staff.
     */
    public boolean requiresAdmin() {
        return permission != null;
    }

    /**
     * @return il permesso richiesto, oppure {@code null} se la sezione e' libera
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Nome canonico usato nei suggerimenti del comando.
     */
    public String getCanonicalName() {
        return aliases.isEmpty()
                ? name().toLowerCase(Locale.ROOT) : aliases.get(0);
    }

    public String getMessageKey() {
        return messageKey;
    }

    public List<String> getAliases() {
        return aliases;
    }
}
