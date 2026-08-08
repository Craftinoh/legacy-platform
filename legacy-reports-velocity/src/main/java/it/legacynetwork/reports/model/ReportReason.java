package it.legacynetwork.reports.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Motivo di segnalazione, cosi' come descritto nella configurazione.
 *
 * <p>Il testo mostrato non vive qui: l'oggetto porta soltanto la chiave di
 * traduzione, che NetworkLanguage risolve nella lingua di chi legge.</p>
 */
public final class ReportReason {

    private final String id;
    private final String displayKey;
    private final Set<String> aliases;
    private final boolean enabled;
    private final boolean allowDetails;
    private final boolean requireDetails;

    public ReportReason(String id, String displayKey, Set<String> aliases,
                        boolean enabled, boolean allowDetails,
                        boolean requireDetails) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Identificatore motivo mancante");
        }
        if (displayKey == null || displayKey.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Chiave di traduzione mancante per il motivo " + id);
        }
        this.id = id.trim().toLowerCase(Locale.ROOT);
        this.displayKey = displayKey.trim();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        normalized.add(this.id);
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias != null && !alias.trim().isEmpty()) {
                    normalized.add(alias.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.aliases = Collections.unmodifiableSet(normalized);
        this.enabled = enabled;
        this.allowDetails = allowDetails;
        this.requireDetails = requireDetails;
    }

    public String getId() {
        return id;
    }

    public String getDisplayKey() {
        return displayKey;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAllowDetails() {
        return allowDetails;
    }

    public boolean isRequireDetails() {
        return requireDetails;
    }

    /**
     * Verifica se il testo scritto dal giocatore indica questo motivo.
     */
    public boolean matches(String input) {
        return input != null
                && aliases.contains(input.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return id;
    }
}
