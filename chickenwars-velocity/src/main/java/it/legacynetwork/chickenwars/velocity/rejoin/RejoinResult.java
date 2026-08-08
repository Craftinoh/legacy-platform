package it.legacynetwork.chickenwars.velocity.rejoin;

/**
 * Esito completo di una richiesta di rientro, con il contesto da mostrare.
 *
 * <p>Porta i valori dei segnaposto {@code {server}}, {@code {arena}} e
 * {@code {reason}}: il comando puo' comporre il messaggio senza conoscere il
 * percorso seguito.</p>
 */
public final class RejoinResult {

    private final RejoinOutcome outcome;
    private final String serverName;
    private final String arenaId;
    private final String reason;

    private RejoinResult(RejoinOutcome outcome, String serverName,
                         String arenaId, String reason) {
        this.outcome = outcome;
        this.serverName = serverName == null ? "" : serverName;
        this.arenaId = arenaId == null ? "" : arenaId;
        this.reason = reason == null ? "" : reason;
    }

    public static RejoinResult of(RejoinOutcome outcome) {
        return new RejoinResult(outcome, "", "", "");
    }

    public static RejoinResult of(RejoinOutcome outcome, String serverName,
                                  String arenaId) {
        return new RejoinResult(outcome, serverName, arenaId, "");
    }

    public static RejoinResult rejected(String serverName, String arenaId,
                                        String reason) {
        return new RejoinResult(RejoinOutcome.BACKEND_REJECTED, serverName,
                arenaId, reason);
    }

    public RejoinOutcome getOutcome() {
        return outcome;
    }

    public String getServerName() {
        return serverName;
    }

    public String getArenaId() {
        return arenaId;
    }

    /**
     * Identificatore stabile del motivo di rifiuto, mai un dettaglio tecnico.
     */
    public String getReason() {
        return reason;
    }

    public String getMessageKey() {
        return outcome.getMessageKey();
    }

    public boolean isSuccessful() {
        return outcome.isSuccessful();
    }

    @Override
    public String toString() {
        return "RejoinResult{" + outcome + ", server=" + serverName
                + ", arena=" + arenaId + ", motivo=" + reason + '}';
    }
}
