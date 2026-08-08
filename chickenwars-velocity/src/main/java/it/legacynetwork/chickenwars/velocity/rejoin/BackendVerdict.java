package it.legacynetwork.chickenwars.velocity.rejoin;

import it.legacynetwork.chickenwars.routing.RejoinVerdictCodec;

/**
 * Risposta della validazione ChickenWars dopo l'arrivo del giocatore.
 */
public final class BackendVerdict {

    private static final BackendVerdict ACCEPTED =
            new BackendVerdict(true, "", "");
    private static final BackendVerdict TIMED_OUT =
            new BackendVerdict(false, RejoinVerdictCodec.REASON_TIMEOUT, "");

    private final boolean accepted;
    private final String reason;
    private final String arenaId;

    private BackendVerdict(boolean accepted, String reason, String arenaId) {
        this.accepted = accepted;
        this.reason = reason == null ? "" : reason;
        this.arenaId = arenaId == null ? "" : arenaId;
    }

    public static BackendVerdict accepted() {
        return ACCEPTED;
    }

    public static BackendVerdict rejected(String reason, String arenaId) {
        return new BackendVerdict(false, reason, arenaId);
    }

    /** Nessuna risposta entro il tempo previsto dal protocollo. */
    public static BackendVerdict timedOut() {
        return TIMED_OUT;
    }

    public boolean isAccepted() {
        return accepted;
    }

    /** Identificatore stabile del motivo, tradotto dal proxy. */
    public String getReason() {
        return reason;
    }

    public String getArenaId() {
        return arenaId;
    }

    public boolean isTimeout() {
        return !accepted
                && RejoinVerdictCodec.REASON_TIMEOUT.equals(reason);
    }

    @Override
    public String toString() {
        return "BackendVerdict{accettato=" + accepted
                + ", motivo=" + reason + '}';
    }
}
