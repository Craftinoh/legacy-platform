package it.legacynetwork.reports.api;

import java.time.Instant;

/**
 * Fotografia dei dati che il proxy possiede davvero al momento della
 * segnalazione.
 *
 * <p>Volutamente povera: Velocity conosce il server a cui il giocatore e'
 * collegato, la latenza della connessione e il proxy che ha ricevuto il
 * comando. Inventario, stato di combattimento, cronologia chat e verdetti
 * anticheat vivono altrove e non esiste, oggi, un provider condiviso che li
 * esponga: nessun campo qui finge di averli.</p>
 */
public final class ReportSnapshot {

    private final String serverId;
    private final long targetPingMillis;
    private final String proxyId;
    private final Instant capturedAt;

    public ReportSnapshot(String serverId, long targetPingMillis,
                          String proxyId, Instant capturedAt) {
        if (capturedAt == null) {
            throw new IllegalArgumentException("Istante dello snapshot mancante");
        }
        this.serverId = serverId == null ? "" : serverId.trim();
        this.targetPingMillis = Math.max(0L, targetPingMillis);
        this.proxyId = proxyId == null ? "" : proxyId.trim();
        this.capturedAt = capturedAt;
    }

    public String getServerId() {
        return serverId;
    }

    public long getTargetPingMillis() {
        return targetPingMillis;
    }

    public String getProxyId() {
        return proxyId;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }
}
