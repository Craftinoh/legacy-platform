package it.legacynetwork.chickenwars.chicken;

import java.util.ArrayList;
import java.util.List;

/**
 * Distribuisce le sconfitte delle Galline Reali ai destinatari registrati.
 *
 * <p>Il dispatcher non decide nulla: la garanzia di unicita' e' data da
 * {@link RoyalChicken#markDefeated()}, che consente una sola notifica per
 * gallina anche in presenza di eventi Bukkit duplicati.</p>
 *
 * <p>Un destinatario che solleva un'eccezione non impedisce agli altri di
 * ricevere la notifica.</p>
 */
public final class RoyalDefeatDispatcher {

    private final List<RoyalDefeatListener> listeners =
            new ArrayList<RoyalDefeatListener>();

    /**
     * Registra un destinatario, ignorando i duplicati.
     */
    public synchronized void register(RoyalDefeatListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void unregister(RoyalDefeatListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifica una sconfitta a tutti i destinatari.
     *
     * @return il numero di destinatari raggiunti senza errori
     */
    public int dispatch(RoyalDefeat defeat) {
        if (defeat == null) {
            return 0;
        }
        List<RoyalDefeatListener> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<RoyalDefeatListener>(listeners);
        }
        int notified = 0;
        for (RoyalDefeatListener listener : snapshot) {
            try {
                listener.onRoyalDefeated(defeat);
                notified++;
            } catch (RuntimeException ignored) {
                // Un destinatario difettoso non deve fermare gli altri.
            }
        }
        return notified;
    }

    public synchronized int size() {
        return listeners.size();
    }

    public synchronized void clear() {
        listeners.clear();
    }
}
