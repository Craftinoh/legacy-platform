package it.legacynetwork.chickenwars.routing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Protocollo dell'esito di rientro, dal backend al proxy.
 *
 * <p>E' l'unica differenza fra "il proxy non e' riuscito a collegare" e "il
 * server e' stato raggiunto e ha rifiutato": senza questa risposta il proxy non
 * potrebbe distinguere i due casi.</p>
 *
 * <p>Il motivo viaggia come identificatore stabile, non come testo: la
 * traduzione avviene sul proxy e nessun dettaglio tecnico raggiunge il
 * giocatore.</p>
 */
public final class RejoinVerdictCodec {

    /** Canale dedicato, registrato da entrambi i lati. */
    public static final String CHANNEL = "chickenwars:rejoin";

    /** Prenotazione assente o non corrispondente al giocatore. */
    public static final String REASON_NO_RESERVATION = "no-reservation";
    /** Prenotazione scaduta oppure gia' reclamata. */
    public static final String REASON_RESERVATION_CLAIMED = "reservation-claimed";
    /** Arena o partita non corrispondenti alla prenotazione. */
    public static final String REASON_WRONG_MATCH = "wrong-match";
    /** Il backend ha rifiutato il reconnect della sessione. */
    public static final String REASON_RECONNECT_REFUSED = "reconnect-refused";
    /** Profilo del giocatore non disponibile. */
    public static final String REASON_PROFILE_UNAVAILABLE = "profile-unavailable";
    /** Nessuna risposta entro il tempo previsto. */
    public static final String REASON_TIMEOUT = "timeout";

    /**
     * Esito decodificato.
     */
    public static final class Verdict {

        private final UUID playerId;
        private final boolean accepted;
        private final String reason;
        private final String arenaId;

        public Verdict(UUID playerId, boolean accepted, String reason,
                       String arenaId) {
            if (playerId == null) {
                throw new IllegalArgumentException("Esito senza giocatore");
            }
            this.playerId = playerId;
            this.accepted = accepted;
            this.reason = reason == null ? "" : reason;
            this.arenaId = arenaId == null ? "" : arenaId;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public boolean isAccepted() {
            return accepted;
        }

        /** Identificatore stabile del motivo, vuoto se accettato. */
        public String getReason() {
            return reason;
        }

        public String getArenaId() {
            return arenaId;
        }

        @Override
        public String toString() {
            return "Verdict{" + playerId + ", accettato=" + accepted
                    + ", motivo=" + reason + '}';
        }
    }

    private RejoinVerdictCodec() {
    }

    /**
     * Serializza un esito.
     */
    public static byte[] encode(UUID playerId, boolean accepted, String reason,
                                String arenaId) {
        if (playerId == null) {
            throw new IllegalArgumentException("Esito senza giocatore");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF(playerId.toString());
            out.writeBoolean(accepted);
            out.writeUTF(reason == null ? "" : reason);
            out.writeUTF(arenaId == null ? "" : arenaId);
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException(
                    "Esito rientro non serializzabile", impossible);
        }
    }

    /**
     * Interpreta un esito ricevuto.
     *
     * @return l'esito, oppure {@code null} se il payload non e' riconoscibile
     */
    public static Verdict decode(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return null;
        }
        try {
            DataInputStream in = new DataInputStream(
                    new ByteArrayInputStream(payload));
            UUID playerId = UUID.fromString(in.readUTF());
            boolean accepted = in.readBoolean();
            String reason = in.readUTF();
            String arenaId = in.readUTF();
            return new Verdict(playerId, accepted, reason, arenaId);
        } catch (IOException malformed) {
            return null;
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }
}
