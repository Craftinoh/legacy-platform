package it.legacynetwork.chickenwars.velocity.transfer;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import it.legacynetwork.chickenwars.routing.RejoinVerdictCodec;
import it.legacynetwork.chickenwars.velocity.rejoin.BackendVerdictRegistry;

/**
 * Riceve dal backend l'esito della validazione di un rientro.
 *
 * <p>Accetta soltanto messaggi provenienti da un server, mai da un client: un
 * giocatore non puo' quindi fabbricare un'accettazione.</p>
 */
public final class BackendVerdictListener {

    private final BackendVerdictRegistry verdicts;

    public BackendVerdictListener(BackendVerdictRegistry verdicts) {
        if (verdicts == null) {
            throw new IllegalArgumentException("Registro esiti mancante");
        }
        this.verdicts = verdicts;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!RejoinVerdictCodec.CHANNEL.equals(
                event.getIdentifier().getId())) {
            return;
        }
        // Il canale e' interno: non deve mai proseguire verso il client.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }
        verdicts.complete(RejoinVerdictCodec.decode(event.getData()));
    }
}
