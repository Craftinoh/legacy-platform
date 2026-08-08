package it.legacynetwork.chickenwars.velocity.rejoin;

/**
 * Decisioni del comando prima di toccare il sistema distribuito.
 *
 * <p>Riceve soltanto valori primitivi gia' estratti dal mittente: e' quindi
 * verificabile senza le interfacce del proxy, che non sono implementabili a
 * mano in un test.</p>
 */
public final class RejoinCommandPolicy {

    private final boolean enabled;
    private final String permission;

    public RejoinCommandPolicy(boolean enabled, String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            throw new IllegalArgumentException("Permesso rejoin mancante");
        }
        this.enabled = enabled;
        this.permission = permission.trim();
    }

    /**
     * Valuta se la richiesta puo' proseguire.
     *
     * @param player        indica se il mittente e' un giocatore
     * @param hasPermission esito della verifica del permesso
     * @return {@code null} se la richiesta puo' proseguire, altrimenti il
     *         motivo del rifiuto
     */
    public RejoinOutcome reject(boolean player, boolean hasPermission) {
        if (!player) {
            return RejoinOutcome.PLAYER_ONLY;
        }
        if (!enabled) {
            return RejoinOutcome.DISABLED;
        }
        if (!hasPermission) {
            return RejoinOutcome.NO_PERMISSION;
        }
        return null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getPermission() {
        return permission;
    }
}
