package it.legacynetwork.reports.notification;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Preferenza di notifica dello staff, valida per la sessione.
 *
 * <p>La rete non ha, oggi, un sistema di preferenze condiviso: tenerla in
 * memoria e' una scelta dichiarata, non una dimenticanza. Chi disattiva le
 * notifiche le ritrova attive al riavvio del proxy.</p>
 */
public final class StaffNotificationPreferences {

    private final boolean enabledByDefault;
    private final Set<UUID> flipped = ConcurrentHashMap.newKeySet();

    public StaffNotificationPreferences(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    /**
     * Indica se lo staffer vuole ricevere le notifiche.
     */
    public boolean isEnabled(UUID staffId) {
        if (staffId == null) {
            return enabledByDefault;
        }
        return flipped.contains(staffId) != enabledByDefault;
    }

    /**
     * Inverte la preferenza.
     *
     * @return il nuovo stato
     */
    public boolean toggle(UUID staffId) {
        if (staffId == null) {
            return enabledByDefault;
        }
        if (!flipped.add(staffId)) {
            flipped.remove(staffId);
        }
        return isEnabled(staffId);
    }

    /**
     * Dimentica la preferenza di chi si disconnette.
     */
    public void forget(UUID staffId) {
        if (staffId != null) {
            flipped.remove(staffId);
        }
    }
}
