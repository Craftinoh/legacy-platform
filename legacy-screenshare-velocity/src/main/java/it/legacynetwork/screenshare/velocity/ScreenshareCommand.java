package it.legacynetwork.screenshare.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import it.legacynetwork.screenshare.command.ScreenshareCommandHandler;

import java.util.List;

/**
 * Adapter Velocity di {@code /ss} e del suo alias {@code /screenshare}.
 */
public final class ScreenshareCommand implements SimpleCommand {

    private final ScreenshareCommandHandler handler;

    public ScreenshareCommand(ScreenshareCommandHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Comando screenshare mancante");
        }
        this.handler = handler;
    }

    @Override
    public void execute(Invocation invocation) {
        handler.execute(new VelocityCommandActor(invocation.source()),
                invocation.arguments());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // Il permesso viene valutato dentro il gestore, che risponde con un
        // messaggio localizzato invece del testo predefinito del proxy.
        return true;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return handler.suggest(new VelocityCommandActor(invocation.source()),
                invocation.arguments());
    }
}
