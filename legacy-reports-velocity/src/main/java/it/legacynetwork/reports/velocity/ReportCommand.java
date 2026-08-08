package it.legacynetwork.reports.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import it.legacynetwork.reports.command.ReportCommandHandler;

import java.util.List;

/**
 * Adapter Velocity di {@code /report}.
 *
 * <p>Volutamente sottile: converte il mittente e delega. Ogni regola vive in
 * {@link ReportCommandHandler}.</p>
 */
public final class ReportCommand implements SimpleCommand {

    private final ReportCommandHandler handler;

    public ReportCommand(ReportCommandHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Comando report mancante");
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
        // Il permesso viene valutato dentro il gestore per poter distinguere
        // "solo giocatori" da "permesso assente" con due messaggi diversi.
        return true;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return handler.suggest(new VelocityCommandActor(invocation.source()),
                invocation.arguments());
    }
}
