package it.legacynetwork.chickenwars.velocity.transfer;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import it.legacynetwork.chickenwars.velocity.rejoin.ProxyTransferGateway;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Trasferimento reale tramite l'API del proxy.
 *
 * <p>Lo spostamento usa {@link Player#createConnectionRequest} e non il canale
 * BungeeCord: su Velocity il messaggio {@code Connect} sarebbe un ripiego
 * inutile.</p>
 *
 * <p>Il token di prenotazione viaggia invece sul protocollo gia' compreso dal
 * backend, che lo attende sul canale {@code BungeeCord} con sottocanale
 * {@code ChickenWarsReservation}. Viene inviato dopo la connessione, quando il
 * server ha certamente un canale aperto per quel giocatore: il backend accetta
 * indifferentemente token prima o dopo l'ingresso.</p>
 */
public final class VelocityTransferGateway implements ProxyTransferGateway {

    private static final ChannelIdentifier CHANNEL =
            new LegacyChannelIdentifier("BungeeCord");
    private static final String SUBCHANNEL = "ChickenWarsReservation";

    private final ProxyServer proxy;

    public VelocityTransferGateway(ProxyServer proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException("Proxy mancante");
        }
        this.proxy = proxy;
    }

    @Override
    public boolean isServerRegistered(String serverName) {
        return resolve(serverName).isPresent();
    }

    @Override
    public boolean isAlreadyConnectedTo(UUID playerId, String serverName) {
        Optional<Player> player = proxy.getPlayer(playerId);
        if (!player.isPresent() || serverName == null) {
            return false;
        }
        Optional<ServerConnection> current = player.get().getCurrentServer();
        return current.isPresent() && serverName.equalsIgnoreCase(
                current.get().getServerInfo().getName());
    }

    @Override
    public CompletionStage<Boolean> transfer(UUID playerId, String serverName,
                                             String reservationId,
                                             String arenaId) {
        Optional<Player> player = proxy.getPlayer(playerId);
        Optional<RegisteredServer> target = resolve(serverName);
        if (!player.isPresent() || !target.isPresent()) {
            // Disconnesso durante il lookup, oppure server non registrato.
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        final RegisteredServer server = target.get();
        final byte[] token = encode(playerId, reservationId, arenaId);

        return player.get().createConnectionRequest(server).connect()
                .thenApply(result -> {
                    if (result == null || !result.isSuccessful()) {
                        return Boolean.FALSE;
                    }
                    // Solo ora il backend puo' ricevere il token: il claim e la
                    // validazione restano interamente suoi.
                    return Boolean.valueOf(
                            server.sendPluginMessage(CHANNEL, token));
                })
                .exceptionally(failure -> Boolean.FALSE);
    }

    @Override
    public CompletionStage<Boolean> evacuate(UUID playerId,
                                             String rejectedServerName) {
        Optional<Player> player = proxy.getPlayer(playerId);
        if (!player.isPresent()) {
            // Gia' uscito: non c'e' nulla da cui allontanarlo.
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        RegisteredServer fallback = firstFallbackOtherThan(rejectedServerName);
        if (fallback == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        return player.get().createConnectionRequest(fallback).connect()
                .thenApply(result -> Boolean.valueOf(
                        result != null && result.isSuccessful()))
                .exceptionally(failure -> Boolean.FALSE);
    }

    /**
     * Primo server dell'ordine di ripiego configurato, escluso quello che ha
     * rifiutato il giocatore.
     */
    private RegisteredServer firstFallbackOtherThan(String rejectedServerName) {
        for (String candidate
                : proxy.getConfiguration().getAttemptConnectionOrder()) {
            if (candidate == null
                    || candidate.equalsIgnoreCase(rejectedServerName)) {
                continue;
            }
            Optional<RegisteredServer> server = proxy.getServer(candidate);
            if (server.isPresent()) {
                return server.get();
            }
        }
        return null;
    }

    /**
     * Compone il payload nella forma che il backend gia' interpreta.
     */
    private byte[] encode(UUID playerId, String reservationId, String arenaId) {
        try {
            ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
            DataOutputStream payload = new DataOutputStream(payloadBytes);
            payload.writeUTF(playerId.toString());
            payload.writeUTF(reservationId);
            payload.writeUTF(arenaId);
            byte[] encoded = payloadBytes.toByteArray();

            ByteArrayOutputStream messageBytes = new ByteArrayOutputStream();
            DataOutputStream message = new DataOutputStream(messageBytes);
            message.writeUTF(SUBCHANNEL);
            message.writeShort(encoded.length);
            message.write(encoded);
            return messageBytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException(
                    "Token prenotazione non serializzabile", impossible);
        }
    }

    private Optional<RegisteredServer> resolve(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            return Optional.empty();
        }
        return proxy.getServer(serverName.trim());
    }

    /**
     * Canale usato per consegnare il token, registrato dal plugin all'avvio.
     */
    public static ChannelIdentifier channel() {
        return CHANNEL;
    }
}
