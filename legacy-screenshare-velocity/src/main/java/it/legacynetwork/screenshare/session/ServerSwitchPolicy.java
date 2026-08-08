package it.legacynetwork.screenshare.session;

import it.legacynetwork.screenshare.config.ScreenshareConfiguration;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Decide se un cambio server puo' avvenire durante un controllo.
 *
 * <p>Questo e' tutto cio' che un proxy puo' fare: impedire al giocatore di
 * andare altrove. Non e' un freeze — movimento, inventario e interazioni
 * restano affare del server, che qui non e' raggiungibile.</p>
 */
public final class ServerSwitchPolicy {

    private final ActiveSessionRegistry registry;
    private final ScreenshareConfiguration configuration;

    public ServerSwitchPolicy(ActiveSessionRegistry registry,
                              ScreenshareConfiguration configuration) {
        if (registry == null || configuration == null) {
            throw new IllegalArgumentException(
                    "Politica sui cambi server incompleta");
        }
        this.registry = registry;
        this.configuration = configuration;
    }

    /**
     * Valuta la richiesta di un giocatore.
     *
     * @param playerId chi sta cambiando server
     * @param requestedServer server richiesto
     */
    public Verdict evaluate(UUID playerId, String requestedServer) {
        Optional<ActiveSessionRegistry.TargetLock> lock =
                registry.lockOf(playerId);
        if (lock.isPresent()) {
            return evaluateTarget(lock.get(), requestedServer);
        }
        if (configuration.isLockStaffServer()
                && registry.sessionOfStaff(playerId).isPresent()) {
            return matches(requestedServer, configuration.getServer())
                    ? Verdict.allowed()
                    : Verdict.denied("screenshare.staff.server-blocked");
        }
        return Verdict.allowed();
    }

    private Verdict evaluateTarget(ActiveSessionRegistry.TargetLock lock,
                                   String requestedServer) {
        if (matches(requestedServer, lock.getScreenshareServer())) {
            return Verdict.allowed();
        }
        if (lock.isCleanup() && fallbackServers().contains(
                normalize(requestedServer))) {
            return Verdict.allowed();
        }
        return Verdict.denied("screenshare.target.server-blocked");
    }

    private Set<String> fallbackServers() {
        Set<String> servers = new LinkedHashSet<>();
        for (String server : configuration.getFallbackServers()) {
            servers.add(normalize(server));
        }
        return servers;
    }

    private static boolean matches(String requested, String expected) {
        return !normalize(expected).isEmpty()
                && normalize(requested).equals(normalize(expected));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /** Esito della valutazione. */
    public static final class Verdict {

        private static final Verdict ALLOWED = new Verdict(true, null);

        private final boolean allowed;
        private final String messageKey;

        private Verdict(boolean allowed, String messageKey) {
            this.allowed = allowed;
            this.messageKey = messageKey;
        }

        static Verdict allowed() {
            return ALLOWED;
        }

        static Verdict denied(String messageKey) {
            return new Verdict(false, messageKey);
        }

        public boolean isAllowed() {
            return allowed;
        }

        /**
         * Chiave del messaggio da mostrare quando il cambio viene annullato.
         */
        public Optional<String> getMessageKey() {
            return Optional.ofNullable(messageKey);
        }
    }
}
