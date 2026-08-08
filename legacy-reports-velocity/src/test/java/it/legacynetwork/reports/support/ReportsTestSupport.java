package it.legacynetwork.reports.support;

import it.legacynetwork.language.Language;
import it.legacynetwork.reports.config.ConfigSection;
import it.legacynetwork.reports.config.ReportsConfiguration;
import it.legacynetwork.reports.message.ReportLanguageResolver;
import it.legacynetwork.reports.message.ReportMessages;
import it.legacynetwork.reports.message.ReportPresenter;
import it.legacynetwork.reports.api.Report;
import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.api.ReportSnapshot;
import it.legacynetwork.reports.api.ReportStatus;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Aiuti condivisi dai test.
 *
 * <p>La configurazione non viene inventata: viene letto il {@code config.yml}
 * realmente spedito nell'artefatto, cosi' un valore predefinito sbagliato fa
 * fallire i test invece di restare nascosto.</p>
 */
public final class ReportsTestSupport {

    public static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    private ReportsTestSupport() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> rawConfiguration() {
        try (InputStream stream = ReportsTestSupport.class
                .getResourceAsStream("/config.yml")) {
            if (stream == null) {
                throw new IllegalStateException("config.yml non incluso");
            }
            return (Map<String, Object>) new Yaml().load(stream);
        } catch (Exception unreadable) {
            throw new IllegalStateException("config.yml illeggibile",
                    unreadable);
        }
    }

    public static ReportsConfiguration configuration() {
        return ReportsConfiguration.fromRoot(
                ConfigSection.of(rawConfiguration()));
    }

    /**
     * Configurazione reale con una modifica mirata alla sezione reports.
     */
    @SuppressWarnings("unchecked")
    public static ReportsConfiguration configuration(
            Consumer<Map<String, Object>> tweakReports) {
        Map<String, Object> root = rawConfiguration();
        tweakReports.accept((Map<String, Object>) root.get("reports"));
        return ReportsConfiguration.fromRoot(ConfigSection.of(root));
    }

    public static ReportMessages messages(Language fallback) {
        return ReportMessages.load(fallback);
    }

    public static ReportPresenter presenter(ReportsConfiguration configuration) {
        return new ReportPresenter(
                messages(configuration.getFallbackLanguage()),
                configuration.getReasons());
    }

    public static ReportLanguageResolver languages(Language fallback) {
        return new ReportLanguageResolver(null, fallback);
    }

    public static ReportLanguageResolver languages(Language fallback,
                                                   Language everyone) {
        return new ReportLanguageResolver(playerId -> everyone, fallback);
    }

    public static Report report(UUID reporterId, UUID targetId,
                                ReportStatus status) {
        return Report.builder()
                .id(ReportId.random())
                .reporter(reporterId, "Reporter")
                .target(targetId, "Target")
                .reasonId("cheating")
                .snapshot(new ReportSnapshot("lobby-1", 42L, "proxy", NOW))
                .createdAt(NOW)
                .updatedAt(NOW)
                .status(status)
                .revision(0L)
                .build();
    }
}
