package it.legacynetwork.reports.command;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attesa fra due segnalazioni dello stesso giocatore.
 *
 * <p>Vive in memoria per sessione: serve a contenere lo spam, non a punire, e
 * un riavvio del proxy che la azzera non e' un problema. Il limite serio sui
 * report ripetuti e' il controllo duplicati, che passa dal database.</p>
 */
public final class CooldownRegistry {

    private final Map<UUID, Instant> lastUse = new ConcurrentHashMap<>();

    /**
     * Secondi ancora da attendere, {@code 0} se il comando e' consentito.
     */
    public long remainingSeconds(UUID playerId, Instant now,
                                 Duration cooldown) {
        if (playerId == null || cooldown == null || cooldown.isZero()
                || cooldown.isNegative()) {
            return 0L;
        }
        Instant last = lastUse.get(playerId);
        if (last == null) {
            return 0L;
        }
        Instant ready = last.plus(cooldown);
        if (!now.isBefore(ready)) {
            return 0L;
        }
        long millis = ready.toEpochMilli() - now.toEpochMilli();
        return (millis + 999L) / 1000L;
    }

    /**
     * Registra l'uso riuscito del comando.
     */
    public void record(UUID playerId, Instant now) {
        if (playerId != null) {
            lastUse.put(playerId, now);
        }
    }

    /**
     * Dimentica il giocatore che si disconnette.
     */
    public void forget(UUID playerId) {
        if (playerId != null) {
            lastUse.remove(playerId);
        }
    }
}
