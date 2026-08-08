package it.legacynetwork.screenshare.velocity;

import com.velocitypowered.api.proxy.Player;
import it.legacynetwork.screenshare.message.ChatLine;
import it.legacynetwork.screenshare.platform.OnlinePlayer;

import java.util.UUID;

/**
 * Giocatore del proxy visto attraverso la porta del dominio.
 */
public final class VelocityOnlinePlayer implements OnlinePlayer {

    private final Player player;

    public VelocityOnlinePlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Giocatore mancante");
        }
        this.player = player;
    }

    @Override
    public UUID uniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String name() {
        return player.getUsername();
    }

    @Override
    public String serverId() {
        return player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse("");
    }

    @Override
    public boolean hasPermission(String node) {
        return player.hasPermission(node);
    }

    @Override
    public void send(ChatLine line) {
        player.sendMessage(AdventureRenderer.render(line));
    }
}
