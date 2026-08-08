package it.legacynetwork.chickenwars.velocity.rejoin;

import java.util.Map;

/**
 * Impostazioni della sezione {@code chickenwars-rejoin}.
 *
 * <p>Nessun indirizzo, porta o nome server: quelli arrivano soltanto dal
 * registry pubblicato dalle istanze.</p>
 */
public final class RejoinSettings {

    /** Permesso usato quando la configurazione non ne indica uno. */
    public static final String DEFAULT_PERMISSION = "chickenwars.command.rejoin";

    private final boolean enabled;
    private final String permission;
    private final long lookupTimeoutMillis;
    private final long transferTimeoutMillis;
    private final long heartbeatTimeoutMillis;
    private final long reservationTtlMillis;
    private final long reconnectTtlMillis;

    public RejoinSettings(boolean enabled, String permission,
                          long lookupTimeoutMillis, long transferTimeoutMillis,
                          long heartbeatTimeoutMillis, long reservationTtlMillis,
                          long reconnectTtlMillis) {
        this.enabled = enabled;
        this.permission = permission == null || permission.trim().isEmpty()
                ? DEFAULT_PERMISSION : permission.trim();
        this.lookupTimeoutMillis = positive(lookupTimeoutMillis, 3000L);
        this.transferTimeoutMillis = positive(transferTimeoutMillis, 5000L);
        this.heartbeatTimeoutMillis = positive(heartbeatTimeoutMillis, 15000L);
        this.reservationTtlMillis = positive(reservationTtlMillis, 30000L);
        this.reconnectTtlMillis = positive(reconnectTtlMillis, 120000L);
    }

    private static long positive(long value, long fallback) {
        return value > 0L ? value : fallback;
    }

    /**
     * Legge la sezione, applicando i valori predefiniti alle chiavi assenti.
     */
    public static RejoinSettings fromMap(Map<String, Object> section) {
        if (section == null) {
            return new RejoinSettings(true, DEFAULT_PERMISSION,
                    0L, 0L, 0L, 0L, 0L);
        }
        return new RejoinSettings(
                bool(section.get("enabled"), true),
                string(section.get("permission")),
                number(section.get("lookup-timeout-millis")),
                number(section.get("transfer-timeout-millis")),
                number(section.get("heartbeat-timeout-millis")),
                number(section.get("reservation-ttl-millis")),
                number(section.get("reconnect-ttl-millis")));
    }

    private static boolean bool(Object raw, boolean fallback) {
        if (raw instanceof Boolean) {
            return ((Boolean) raw).booleanValue();
        }
        return raw == null ? fallback : Boolean.parseBoolean(String.valueOf(raw));
    }

    private static String string(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    private static long number(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException invalid) {
            return 0L;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getPermission() {
        return permission;
    }

    public long getLookupTimeoutMillis() {
        return lookupTimeoutMillis;
    }

    public long getTransferTimeoutMillis() {
        return transferTimeoutMillis;
    }

    public long getHeartbeatTimeoutMillis() {
        return heartbeatTimeoutMillis;
    }

    public long getReservationTtlMillis() {
        return reservationTtlMillis;
    }

    public long getReconnectTtlMillis() {
        return reconnectTtlMillis;
    }
}
