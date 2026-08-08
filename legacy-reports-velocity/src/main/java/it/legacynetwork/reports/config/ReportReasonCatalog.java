package it.legacynetwork.reports.config;

import it.legacynetwork.reports.model.ReportReason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Motivi di segnalazione disponibili.
 *
 * <p>La lista vive nella configurazione, non nel comando: aggiungere un motivo
 * significa aggiungere una voce nel file e la chiave di traduzione
 * corrispondente, non toccare il codice.</p>
 */
public final class ReportReasonCatalog {

    private final Map<String, ReportReason> byId;

    public ReportReasonCatalog(List<ReportReason> reasons) {
        Map<String, ReportReason> ordered = new LinkedHashMap<>();
        if (reasons != null) {
            for (ReportReason reason : reasons) {
                if (reason != null) {
                    ordered.put(reason.getId(), reason);
                }
            }
        }
        this.byId = Collections.unmodifiableMap(ordered);
    }

    /**
     * Legge la sezione {@code reports.reasons}.
     */
    public static ReportReasonCatalog fromSection(ConfigSection reasons) {
        List<ReportReason> parsed = new ArrayList<>();
        for (String id : reasons.keys()) {
            ConfigSection entry = reasons.section(id);
            parsed.add(new ReportReason(
                    id,
                    entry.text("display-key",
                            "reports.reason." + id.toLowerCase(Locale.ROOT)),
                    new java.util.LinkedHashSet<>(entry.list("aliases")),
                    entry.flag("enabled", true),
                    entry.flag("allow-details", false),
                    entry.flag("require-details", false)));
        }
        return new ReportReasonCatalog(parsed);
    }

    /**
     * Motivi attivi, nell'ordine del file.
     */
    public List<ReportReason> enabled() {
        List<ReportReason> active = new ArrayList<>();
        for (ReportReason reason : byId.values()) {
            if (reason.isEnabled()) {
                active.add(reason);
            }
        }
        return Collections.unmodifiableList(active);
    }

    /**
     * Tutti i motivi configurati, attivi o meno.
     */
    public List<ReportReason> all() {
        return Collections.unmodifiableList(new ArrayList<>(byId.values()));
    }

    public boolean isEmpty() {
        return enabled().isEmpty();
    }

    /**
     * Cerca il motivo indicato da un identificatore o da un alias.
     *
     * <p>Un motivo disattivato non viene trovato: per chi scrive il comando e'
     * come se non esistesse.</p>
     */
    public Optional<ReportReason> find(String input) {
        if (input == null) {
            return Optional.empty();
        }
        for (ReportReason reason : byId.values()) {
            if (reason.isEnabled() && reason.matches(input)) {
                return Optional.of(reason);
            }
        }
        return Optional.empty();
    }

    /**
     * Cerca per identificatore esatto, anche se disattivato.
     *
     * <p>Serve a mostrare i report storici creati con un motivo poi rimosso.</p>
     */
    public Optional<ReportReason> findById(String id) {
        return id == null ? Optional.empty()
                : Optional.ofNullable(
                        byId.get(id.trim().toLowerCase(Locale.ROOT)));
    }

    /**
     * Chiave di traduzione da mostrare per un motivo memorizzato.
     */
    public String displayKey(String reasonId) {
        return findById(reasonId)
                .map(ReportReason::getDisplayKey)
                .orElse("reports.reason.unknown");
    }
}
