package it.legacynetwork.reports.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import it.legacynetwork.reports.platform.OnlinePlayer;
import it.legacynetwork.reports.platform.PlayerDirectory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Elenco dei giocatori collegati al proxy.
 *
 * <p>Solo chi e' online: questa versione non risolve UUID remoti e non
 * interroga alcun servizio esterno.</p>
 */
public final class VelocityPlayerDirectory implements PlayerDirectory {

    private final ProxyServer proxy;

    public VelocityPlayerDirectory(ProxyServer proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException("Proxy mancante");
        }
        this.proxy = proxy;
    }

    @Override
    public Optional<OnlinePlayer> findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return proxy.getPlayer(name.trim()).map(VelocityOnlinePlayer::new);
    }

    @Override
    public Optional<OnlinePlayer> findById(UUID playerId) {
        return playerId == null ? Optional.empty()
                : proxy.getPlayer(playerId).map(VelocityOnlinePlayer::new);
    }

    @Override
    public List<OnlinePlayer> withPermission(String node) {
        List<OnlinePlayer> matches = new ArrayList<>();
        for (Player player : proxy.getAllPlayers()) {
            if (player.hasPermission(node)) {
                matches.add(new VelocityOnlinePlayer(player));
            }
        }
        return Collections.unmodifiableList(matches);
    }

    @Override
    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (Player player : proxy.getAllPlayers()) {
            names.add(player.getUsername());
        }
        return Collections.unmodifiableList(names);
    }
}
