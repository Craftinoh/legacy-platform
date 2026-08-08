package it.legacynetwork.chickenwars.routing;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Invia l'esito al proxy sul canale dedicato al rientro.
 *
 * <p>Il messaggio viaggia sulla connessione del giocatore appena arrivato: il
 * proxy lo riceve come proveniente da quel server e puo' quindi distinguere un
 * rifiuto di ChickenWars da un fallimento di connessione.</p>
 */
public final class BukkitRejoinVerdictSink implements RejoinVerdictSink {

    /** Risolve il giocatore connesso, senza dipendere da Bukkit nei test. */
    public interface OnlinePlayerLookup {
        Player find(UUID playerId);
    }

    private final JavaPlugin plugin;
    private final OnlinePlayerLookup players;

    public BukkitRejoinVerdictSink(JavaPlugin plugin,
                                   OnlinePlayerLookup players) {
        if (plugin == null || players == null) {
            throw new IllegalArgumentException("Sink esito incompleto");
        }
        this.plugin = plugin;
        this.players = players;
    }

    @Override
    public void report(UUID playerId, boolean accepted, String reason,
                       String arenaId) {
        if (playerId == null) {
            return;
        }
        Player player = players.find(playerId);
        if (player == null || !player.isOnline()) {
            // Uscito prima della validazione: il proxy chiudera' comunque il
            // tentativo con il proprio timeout.
            return;
        }
        player.sendPluginMessage(plugin, RejoinVerdictCodec.CHANNEL,
                RejoinVerdictCodec.encode(playerId, accepted, reason, arenaId));
    }
}
