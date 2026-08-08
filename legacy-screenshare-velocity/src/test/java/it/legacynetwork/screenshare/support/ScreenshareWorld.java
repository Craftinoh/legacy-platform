package it.legacynetwork.screenshare.support;

import it.legacynetwork.language.Language;
import it.legacynetwork.reports.api.LegacyReportsApi;
import it.legacynetwork.reports.api.Report;
import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.api.ReportSnapshot;
import it.legacynetwork.reports.api.ReportStatus;
import it.legacynetwork.reports.repository.InMemoryReportEventRepository;
import it.legacynetwork.reports.repository.InMemoryReportRepository;
import it.legacynetwork.reports.service.DefaultLegacyReportsApi;
import it.legacynetwork.reports.service.ReportService;
import it.legacynetwork.screenshare.config.ConfigSection;
import it.legacynetwork.screenshare.config.ScreenshareConfiguration;
import it.legacynetwork.screenshare.message.ScreenshareLanguageResolver;
import it.legacynetwork.screenshare.message.ScreenshareMessages;
import it.legacynetwork.screenshare.message.ScreensharePresenter;
import it.legacynetwork.screenshare.reports.ReportLink;
import it.legacynetwork.screenshare.repository.InMemoryScreenshareEventRepository;
import it.legacynetwork.screenshare.repository.InMemoryScreenshareRepository;
import it.legacynetwork.screenshare.service.ScreenshareService;
import it.legacynetwork.screenshare.session.ActiveSessionRegistry;
import it.legacynetwork.screenshare.session.ServerSwitchPolicy;
import it.legacynetwork.screenshare.session.TargetCommandPolicy;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Mondo di prova completo: sessioni, trasferimenti, violazioni e report.
 *
 * <p>La configurazione non viene inventata: viene letto il {@code config.yml}
 * realmente spedito. Il lato report usa lo stesso {@code ReportService} del
 * plugin vicino, non un finto: cosi' l'integrazione provata e' quella vera.</p>
 */
public final class ScreenshareWorld {

    public static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    public static final String SCREENSHARE_SERVER = "screenshare-1";

    private final AtomicReference<Instant> clock = new AtomicReference<>(NOW);

    public final ScreenshareConfiguration configuration;
    public final InMemoryScreenshareRepository sessions =
            new InMemoryScreenshareRepository();
    public final InMemoryScreenshareEventRepository events =
            new InMemoryScreenshareEventRepository();
    public final FakeDirectory directory = new FakeDirectory();
    public final FakeTransferGateway transfers = new FakeTransferGateway();
    public final RecordingViolationHandler violations =
            new RecordingViolationHandler();
    public final ActiveSessionRegistry registry = new ActiveSessionRegistry();
    public final ScreenshareMessages messages;
    public final ScreensharePresenter presenter;
    public final ScreenshareLanguageResolver languages;
    public final ScreenshareService service;
    public final ServerSwitchPolicy switchPolicy;
    public final TargetCommandPolicy commandPolicy;

    public final InMemoryReportRepository reportRepository =
            new InMemoryReportRepository();
    public final InMemoryReportEventRepository reportEvents =
            new InMemoryReportEventRepository();
    public final ReportService reportService;

    public ScreenshareWorld() {
        this(configuration(), Language.ITALIAN, true);
    }

    public ScreenshareWorld(ScreenshareConfiguration configuration,
                            Language playerLanguage, boolean withReports) {
        this.configuration = configuration;
        this.messages = ScreenshareMessages.load(
                configuration.getFallbackLanguage());
        this.presenter = new ScreensharePresenter(messages);
        this.languages = new ScreenshareLanguageResolver(
                playerId -> playerLanguage,
                configuration.getFallbackLanguage());
        this.reportService = new ReportService(reportRepository, reportEvents,
                clock::get, "proxy-test");
        LegacyReportsApi api = withReports
                ? new DefaultLegacyReportsApi(reportService) : null;
        this.service = new ScreenshareService(configuration, sessions, events,
                transfers, directory, presenter, languages,
                api == null ? ReportLink.unavailable() : new ReportLink(api),
                violations, registry, clock::get);
        this.switchPolicy = new ServerSwitchPolicy(registry, configuration);
        this.commandPolicy = new TargetCommandPolicy(registry, configuration);
        transfers.register(SCREENSHARE_SERVER, "lobby-1", "lobby-2");
    }

    public Instant now() {
        return clock.get();
    }

    public void advance(java.time.Duration amount) {
        clock.set(clock.get().plus(amount));
    }

    public ScreenshareMessages messages() {
        return messages;
    }

    /**
     * Ripristino come all'avvio del proxy: il registro riparte vuoto.
     *
     * @return il numero di sessioni chiuse d'ufficio
     */
    public int recoverWithFreshRegistry() {
        return service.recover().join();
    }

    /**
     * Report gia' in indagine e assegnato allo staffer indicato.
     */
    public ReportId investigatingReport(UUID targetId, String targetName,
                                        UUID staffId, String staffName) {
        Report report = Report.builder()
                .id(ReportId.random())
                .reporter(UUID.randomUUID(), "Reporter")
                .target(targetId, targetName)
                .reasonId("cheating")
                .snapshot(new ReportSnapshot("lobby-1", 42L, "proxy-test",
                        now()))
                .createdAt(now())
                .updatedAt(now())
                .status(ReportStatus.OPEN)
                .revision(0L)
                .build();
        reportService.create(report).join();
        reportService.investigate(report.getId(), staffId, staffName).join();
        return report.getId();
    }

    public ReportStatus reportStatus(ReportId id) {
        return reportService.find(id).join().orElseThrow().getStatus();
    }

    // -------------------------------------------------------- configurazione

    @SuppressWarnings("unchecked")
    public static Map<String, Object> rawConfiguration() {
        try (InputStream stream = ScreenshareWorld.class
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

    public static ScreenshareConfiguration configuration() {
        return ScreenshareConfiguration.fromRoot(
                ConfigSection.of(rawConfiguration()));
    }

    /**
     * Configurazione reale con una modifica mirata alla sezione screenshare.
     */
    @SuppressWarnings("unchecked")
    public static ScreenshareConfiguration configuration(
            Consumer<Map<String, Object>> tweak) {
        Map<String, Object> root = rawConfiguration();
        tweak.accept((Map<String, Object>) root.get("screenshare"));
        return ScreenshareConfiguration.fromRoot(ConfigSection.of(root));
    }
}
