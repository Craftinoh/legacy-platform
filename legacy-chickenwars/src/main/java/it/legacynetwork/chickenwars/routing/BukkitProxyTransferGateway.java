package it.legacynetwork.chickenwars.routing;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Adapter main-thread per il canale proxy standard, senza indirizzi hardcoded. */
public final class BukkitProxyTransferGateway implements TransferGateway {
    public interface OnlinePlayerLookup { Player find(UUID playerId); }
    private final JavaPlugin plugin;
    private final OnlinePlayerLookup players;
    public BukkitProxyTransferGateway(JavaPlugin plugin, OnlinePlayerLookup players) {
        if (plugin == null || players == null) throw new IllegalArgumentException("Gateway incompleto");
        this.plugin = plugin; this.players = players;
    }
    @Override public void transfer(UUID playerId, String serverName,
                                   String reservationId, String arenaId) {
        Player player = players.find(playerId);
        if (player == null || !player.isOnline()) return;
        try {
            ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
            DataOutputStream payload = new DataOutputStream(payloadBytes);
            payload.writeUTF(playerId.toString());
            payload.writeUTF(reservationId);
            payload.writeUTF(arenaId);

            ByteArrayOutputStream forwardBytes = new ByteArrayOutputStream();
            DataOutputStream forward = new DataOutputStream(forwardBytes);
            forward.writeUTF("Forward"); forward.writeUTF(serverName);
            forward.writeUTF("ChickenWarsReservation");
            byte[] encoded = payloadBytes.toByteArray();
            forward.writeShort(encoded.length); forward.write(encoded);
            player.sendPluginMessage(plugin, "BungeeCord",
                    forwardBytes.toByteArray());

            sendConnect(player, serverName);
        } catch (IOException exception) {
            throw new IllegalStateException("Trasferimento proxy non serializzabile", exception);
        }
    }

    @Override public void connect(UUID playerId, String serverName) {
        Player player = players.find(playerId);
        if (player == null || !player.isOnline() || serverName == null
                || serverName.trim().isEmpty()) return;
        try { sendConnect(player, serverName); }
        catch (IOException exception) {
            throw new IllegalStateException(
                    "Trasferimento lobby non serializzabile", exception);
        }
    }

    private void sendConnect(Player player, String serverName)
            throws IOException {
        ByteArrayOutputStream connectBytes = new ByteArrayOutputStream();
        DataOutputStream connect = new DataOutputStream(connectBytes);
        connect.writeUTF("Connect"); connect.writeUTF(serverName);
        player.sendPluginMessage(plugin, "BungeeCord",
                connectBytes.toByteArray());
    }
}
