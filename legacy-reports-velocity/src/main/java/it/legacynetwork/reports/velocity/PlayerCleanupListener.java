package it.legacynetwork.reports.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import it.legacynetwork.reports.command.CooldownRegistry;
import it.legacynetwork.reports.notification.StaffNotificationPreferences;

/**
 * Libera lo stato di sessione quando un giocatore si scollega.
 *
 * <p>Attesa fra due segnalazioni e preferenza sulle notifiche vivono in
 * memoria: senza questa pulizia crescerebbero per tutta la vita del proxy.</p>
 */
public final class PlayerCleanupListener {

    private final CooldownRegistry cooldowns;
    private final StaffNotificationPreferences preferences;

    public PlayerCleanupListener(CooldownRegistry cooldowns,
                                 StaffNotificationPreferences preferences) {
        this.cooldowns = cooldowns;
        this.preferences = preferences;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        cooldowns.forget(event.getPlayer().getUniqueId());
        preferences.forget(event.getPlayer().getUniqueId());
    }
}
