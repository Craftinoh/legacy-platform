package it.legacynetwork.screenshare.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cosa fare quando e' lo staffer a scollegarsi durante un controllo.
 *
 * <p>{@link #TRANSFER_TO_AVAILABLE_STAFF} e' dichiarato ma non implementato:
 * assegnare il controllo a un altro staffer richiede una coda reale di staff
 * disponibili, che questa rete non ha. Il valore viene quindi rifiutato in
 * configurazione invece di essere accettato e ignorato in silenzio.</p>
 */
public enum StaffDisconnectPolicy {

    /** La sessione viene annullata subito. */
    CANCEL(true),
    /** La sessione resta attiva per la finestra di rientro configurata. */
    KEEP_ACTIVE_FOR_SECONDS(true),
    /** Non disponibile in questa versione. */
    TRANSFER_TO_AVAILABLE_STAFF(false);

    private final boolean implemented;

    StaffDisconnectPolicy(boolean implemented) {
        this.implemented = implemented;
    }

    public boolean isImplemented() {
        return implemented;
    }

    /**
     * Valori realmente utilizzabili nella configurazione.
     */
    public static List<String> supportedNames() {
        List<String> names = new ArrayList<>();
        for (StaffDisconnectPolicy policy : values()) {
            if (policy.implemented) {
                names.add(policy.name());
            }
        }
        return names;
    }

    /**
     * Legge il valore scritto in configurazione.
     *
     * @throws ScreenshareConfigurationException se il valore non esiste oppure
     *         esiste ma non e' implementato
     */
    public static StaffDisconnectPolicy parse(String raw) {
        String normalized = raw == null ? "" : raw.trim()
                .toUpperCase(Locale.ROOT).replace('-', '_');
        for (StaffDisconnectPolicy policy : values()) {
            if (policy.name().equals(normalized)) {
                if (!policy.isImplemented()) {
                    throw new ScreenshareConfigurationException(
                            "screenshare.staff-disconnect-policy: " + normalized
                                    + " non e' implementato in questa versione;"
                                    + " valori supportati "
                                    + supportedNames());
                }
                return policy;
            }
        }
        throw new ScreenshareConfigurationException(
                "screenshare.staff-disconnect-policy: valore sconosciuto '"
                        + raw + "'; valori supportati " + supportedNames());
    }
}
