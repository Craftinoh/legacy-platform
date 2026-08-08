package it.legacynetwork.screenshare.support;

import it.legacynetwork.screenshare.platform.TransferGateway;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Trasferimenti simulati, con esiti decisi dal test.
 */
public final class FakeTransferGateway implements TransferGateway {

    private final Set<String> registered = new LinkedHashSet<>();
    private final Set<UUID> refused = new LinkedHashSet<>();
    private final List<String> attempts = new ArrayList<>();

    public FakeTransferGateway register(String... servers) {
        for (String server : servers) {
            registered.add(server.toLowerCase(Locale.ROOT));
        }
        return this;
    }

    public FakeTransferGateway unregister(String server) {
        registered.remove(server.toLowerCase(Locale.ROOT));
        return this;
    }

    /**
     * Il giocatore indicato non raggiungera' alcun server.
     */
    public FakeTransferGateway refuse(UUID playerId) {
        refused.add(playerId);
        return this;
    }

    @Override
    public boolean isRegistered(String serverName) {
        return serverName != null
                && registered.contains(serverName.toLowerCase(Locale.ROOT));
    }

    @Override
    public CompletableFuture<Boolean> transfer(UUID playerId,
                                               String serverName) {
        attempts.add(playerId + "->" + serverName);
        if (!isRegistered(serverName) || refused.contains(playerId)) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

    /**
     * Trasferimenti richiesti, nell'ordine.
     */
    public List<String> attempts() {
        return new ArrayList<>(attempts);
    }

    public boolean movedTo(UUID playerId, String serverName) {
        return attempts.contains(playerId + "->" + serverName);
    }
}
