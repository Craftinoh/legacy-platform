package it.legacynetwork.reports.api;

/**
 * Plugin che pubblica la propria {@link LegacyReportsApi} sul proxy.
 *
 * <p>Stesso meccanismo gia' usato per NetworkLanguage: chi vuole l'API chiede al
 * plugin manager l'istanza di {@code legacyreports} e la verifica contro questa
 * interfaccia. Nessuna reflection, nessun registro globale.</p>
 */
public interface LegacyReportsApiHolder {

    /**
     * API da usare, oppure {@code null} finche' il plugin non e' pronto.
     */
    LegacyReportsApi reportsApi();
}
