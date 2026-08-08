package it.legacynetwork.reports.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import it.legacynetwork.reports.message.ChatLine;
import it.legacynetwork.reports.platform.CommandActor;

import java.util.UUID;

/**
 * Mittente di un comando Velocity visto attraverso la porta del dominio.
 *
 * <p>La console non ha UUID: e' l'unico dettaglio che la logica deve
 * conoscere.</p>
 */
public final class VelocityCommandActor implements CommandActor {

    private static final String CONSOLE_NAME = "CONSOLE";

    private final CommandSource source;

    public VelocityCommandActor(CommandSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Mittente mancante");
        }
        this.source = source;
    }

    @Override
    public UUID uniqueId() {
        return source instanceof Player ? ((Player) source).getUniqueId()
                : null;
    }

    @Override
    public String name() {
        return source instanceof Player ? ((Player) source).getUsername()
                : CONSOLE_NAME;
    }

    @Override
    public boolean hasPermission(String node) {
        return source.hasPermission(node);
    }

    @Override
    public void send(ChatLine line) {
        source.sendMessage(AdventureRenderer.render(line));
    }
}
