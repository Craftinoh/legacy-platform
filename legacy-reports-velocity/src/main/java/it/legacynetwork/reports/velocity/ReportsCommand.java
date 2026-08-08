package it.legacynetwork.reports.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import it.legacynetwork.reports.command.ReportsCommandHandler;

import java.util.List;

/**
 * Adapter Velocity di {@code /reports}.
 */
public final class ReportsCommand implements SimpleCommand {

    private final ReportsCommandHandler handler;

    public ReportsCommand(ReportsCommandHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Comandi staff mancanti");
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
