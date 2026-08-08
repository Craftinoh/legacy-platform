package it.legacynetwork.screenshare.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import it.legacynetwork.screenshare.platform.TransferGateway;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Spostamento fra server tramite l'API del proxy.
 *
 * <p>Usa {@link Player#createConnectionRequest(RegisteredServer)}: e' l'unico
 * modo affidabile di muovere un giocatore da Velocity, e il suo esito dice
 * davvero se il giocatore e' arrivato.</p>
 */
public final class VelocityTransferGateway implements TransferGateway {

    private final ProxyServer proxy;

    public VelocityTransferGateway(ProxyServer proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException("Proxy mancante");
        }
        this.proxy = proxy;
    }

    @Override
    public boolean isRegistered(String serverName) {
        return resolve(serverName).isPresent();
    }

    @Override
    public CompletableFuture<Boolean> transfer(UUID playerId,
                                               String serverName) {
        Optional<Player> player = proxy.getPlayer(playerId);
        Optional<RegisteredServer> target = resolve(serverName);
        if (!player.isPresent() || !target.isPresent()) {
            // Scollegato durante l'attesa, oppure server non registrato.
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        Optional<String> current = player.get().getCurrentServer()
                .map(connection -> connection.getServerInfo().getName());
        if (current.isPresent()
                && current.get().equalsIgnoreCase(serverName)) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }
        return player.get().createConnectionRequest(target.get()).connect()
                .thenApply(result -> Boolean.valueOf(
                        result != null && result.isSuccessful()))
                .exceptionally(failure -> Boolean.FALSE)
                .toCompletableFuture();
    }

    private Optional<RegisteredServer> resolve(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            return Optional.empty();
        }
        return proxy.getServer(serverName.trim());
    }
}
