package it.legacynetwork.reports.command;

import it.legacynetwork.language.Language;
import it.legacynetwork.reports.config.ReportsConfiguration;
import it.legacynetwork.reports.message.ReportLanguageResolver;
import it.legacynetwork.reports.message.ReportMessages;
import it.legacynetwork.reports.message.ReportPresenter;
import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.model.ReportEvent;
import it.legacynetwork.reports.model.ReportEventType;
import it.legacynetwork.reports.model.ReportStatus;
import it.legacynetwork.reports.notification.StaffNotificationPreferences;
import it.legacynetwork.reports.notification.StaffNotificationService;
import it.legacynetwork.reports.repository.InMemoryReportEventRepository;
import it.legacynetwork.reports.repository.InMemoryReportRepository;
import it.legacynetwork.reports.repository.ReportPage;
import it.legacynetwork.reports.service.ReportService;
import it.legacynetwork.reports.support.FailingReportRepository;
import it.legacynetwork.reports.support.FakeConsole;
import it.legacynetwork.reports.support.FakeDirectory;
import it.legacynetwork.reports.support.FakePlayer;
import it.legacynetwork.reports.support.ReportsTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comportamento di {@code /report} in ogni ramo previsto.
 *
 * <p>I testi attesi non sono copiati a mano: vengono chiesti alla stessa
 * {@link ReportMessages} usata dal comando, quindi il test verifica la chiave e
 * la lingua, non una frase riscritta due volte.</p>
 */
class ReportCommandHandlerTest {

    private Instant now = ReportsTestSupport.NOW;

    private InMemoryReportRepository reports;
    private InMemoryReportEventRepository events;
    private FakeDirectory directory;
    private ReportMessages messages;
    private ReportService service;

    private FakePlayer reporter;
    private FakePlayer target;

    @BeforeEach
    void setUp() {
        reports = new InMemoryReportRepository();
        events = new InMemoryReportEventRepository();
        directory = new FakeDirectory();
        reporter = new FakePlayer("Reporter")
                .allow("legacyreports.command.report");
        target = new FakePlayer("Target").on("bedwars-3").ping(120L);
        directory.add(reporter, target);
    }

    private ReportCommandHandler handler(ReportsConfiguration configuration,
                                         Language playerLanguage) {
        return handler(configuration, playerLanguage, false);
    }

    private ReportCommandHandler handler(ReportsConfiguration configuration,
                                         Language playerLanguage,
                                         boolean brokenStorage) {
        service = new ReportService(
                brokenStorage ? new FailingReportRepository() : reports,
                events, () -> now, "proxy-test");
        messages = ReportMessages.load(configuration.getFallbackLanguage());
        ReportPresenter presenter = new ReportPresenter(messages,
                configuration.getReasons());
        ReportLanguageResolver languages = new ReportLanguageResolver(
                playerId -> playerLanguage,
                configuration.getFallbackLanguage());
        StaffNotificationPreferences preferences =
                new StaffNotificationPreferences(true);
        StaffNotificationService notifications = new StaffNotificationService(
                directory, presenter, languages, preferences,
                configuration.getPermissions().getStaffView());
        return new ReportCommandHandler(configuration, service, directory,
                presenter, languages, notifications, new CooldownRegistry(),
                () -> now);
    }

    private ReportCommandHandler handler() {
        return handler(ReportsTestSupport.configuration(), Language.ITALIAN);
    }

    private ReportCommandHandler handler(Consumer<java.util.Map<String, Object>> tweak) {
        return handler(ReportsTestSupport.configuration(tweak),
                Language.ITALIAN);
    }

    private String expected(String key) {
        return messages.get(Language.ITALIAN, key);
    }

    @Test
    void laConsoleNonPuoSegnalare() {
        ReportCommandHandler handler = handler();
        FakeConsole console = new FakeConsole();

        handler.execute(console, new String[]{"Target", "cheating"}).join();

        assertEquals(messages.get(messages.getFallback(),
                        "reports.error.player-only"),
                console.text());
    }

    @Test
    void senzaPermessoIlComandoVieneRifiutato() {
        ReportCommandHandler handler = handler();
        FakePlayer stranger = new FakePlayer("Stranger");
        directory.add(stranger);

        handler.execute(stranger, new String[]{"Target", "cheating"}).join();

        assertEquals(expected("reports.error.no-permission"), stranger.text());
    }

    @Test
    void unaSegnalazioneValidaVieneRegistrata() {
        ReportCommandHandler handler = handler();

        handler.execute(reporter, new String[]{"Target", "hack"}).join();

        ReportPage page = reports.listByStatuses(
                ReportService.ACTIVE_STATUSES, 1, 10).join();
        assertEquals(1, page.getItems().size());
        Report stored = page.getItems().get(0);
        assertEquals(ReportStatus.OPEN, stored.getStatus());
        assertEquals("cheating", stored.getReasonId());
        assertEquals(target.uniqueId(), stored.getTargetId());
        assertEquals("bedwars-3", stored.getSnapshot().getServerId());
        assertEquals(120L, stored.getSnapshot().getTargetPingMillis());
        assertTrue(reporter.text().contains(stored.getId().shortCode()));
    }

    @Test
    void laCreazioneLasciaUnEventoDiStorico() {
        ReportCommandHandler handler = handler();

        handler.execute(reporter, new String[]{"Target", "cheating"}).join();

        List<ReportEvent> history = events.all();
        assertEquals(1, history.size());
        assertEquals(ReportEventType.CREATED, history.get(0).getType());
        assertEquals(ReportStatus.OPEN,
                history.get(0).getNewStatus().orElse(null));
    }

    @Test
    void unBersaglioNonCollegatoNonEuSegnalabile() {
        ReportCommandHandler handler = handler();

        handler.execute(reporter, new String[]{"Sconosciuto", "cheating"})
                .join();

        assertTrue(reporter.text().contains("Sconosciuto"));
        assertTrue(reports.listByStatuses(ReportService.ACTIVE_STATUSES, 1, 10)
                .join().isEmpty());
    }

    @Test
    void nonCiSiPuoSegnalareDaSoli() {
        ReportCommandHandler handler = handler();

        handler.execute(reporter, new String[]{"Reporter", "cheating"}).join();

        assertEquals(expected("reports.error.self-report"), reporter.text());
    }

    @Test
    void unMotivoSconosciutoVieneRifiutato() {
        ReportCommandHandler handler = handler();

        handler.execute(reporter, new String[]{"Target", "inventato"}).join();

        assertTrue(reporter.text().contains("cheating"),
                "l'elenco dei motivi disponibili deve comparire");
        assertTrue(reports.listByStatuses(ReportService.ACTIVE_STATUSES, 1, 10)
                .join().isEmpty());
    }

    @Test
    void senzaArgomentiVieneMostratoLUso() {
        ReportCommandHandler handler = handler();

        handler.execute(reporter, new String[0]).join();

        assertEquals(2, reporter.received().size());
        assertTrue(reporter.text().contains(
                expected("reports.command.report.usage")));
    }

    @Test
    void ilBersaglioProtettoNonEuSegnalabile() {
        target.allow("legacyreports.protected");
        ReportCommandHandler handler = handler();

        handler.execute(reporter, new String[]{"Target", "cheating"}).join();

        assertTrue(reporter.text().contains("Target"));
        assertTrue(reports.listByStatuses(ReportService.ACTIVE_STATUSES, 1, 10)
                .join().isEmpty());
    }

    @Test
    void ilCooldownBloccaLaSecondaSegnalazione() {
        ReportCommandHandler handler = handler();
        FakePlayer other = new FakePlayer("Altro");
        directory.add(other);

        handler.execute(reporter, new String[]{"Target", "cheating"}).join();
        reporter.clear();
        handler.execute(reporter, new String[]{"Altro", "cheating"}).join();

        assertTrue(reporter.text().startsWith("&c"),
                "il messaggio di attesa e' un errore: " + reporter.text());
        assertEquals(1, reports.listByStatuses(ReportService.ACTIVE_STATUSES,
                1, 10).join().getItems().size());
    }

    @Test
    void unaSegnalazioneRipetutaSulloStessoBersaglioEuUnDuplicato() {
        ReportCommandHandler handler = handler();

        handler.execute(reporter, new String[]{"Target", "cheating"}).join();
        now = now.plus(Duration.ofMinutes(5));
        reporter.clear();
        handler.execute(reporter, new String[]{"Target", "cheating"}).join();

        Report first = reports.listByStatuses(ReportService.ACTIVE_STATUSES, 1,
                10).join().getItems().get(0);
        assertTrue(reporter.text().contains(first.getId().shortCode()));
        assertEquals(1, reports.listByStatuses(ReportService.ACTIVE_STATUSES,
                1, 10).join().getTotalItems());
    }

    @Test
    void ilLimiteDiReportApertiVieneRispettato() {
        ReportCommandHandler handler = handler(reports -> {
            reports.put("max-open-per-reporter", 1);
            reports.put("cooldown-seconds", 0);
        });
        FakePlayer other = new FakePlayer("Altro");
        directory.add(other);

        handler.execute(reporter, new String[]{"Target", "cheating"}).join();
        reporter.clear();
        handler.execute(reporter, new String[]{"Altro", "cheating"}).join();

        assertEquals(1, reports.listByStatuses(ReportService.ACTIVE_STATUSES,
                1, 10).join().getTotalItems());
        assertTrue(reporter.text().contains("1"));
    }

    @Test
    void unMotivoConDettagliObbligatoriLiRichiede() {
        ReportCommandHandler handler = handler();

        handler.execute(reporter, new String[]{"Target", "other"}).join();

        assertEquals(expected("reports.error.details-required"),
                reporter.text());
    }

    @Test
    void iDettagliVengonoRegistrati() {
        ReportCommandHandler handler = handler();

        handler.execute(reporter,
                new String[]{"Target", "other", "sta", "usando", "bug"}).join();

        Report stored = reports.listByStatuses(ReportService.ACTIVE_STATUSES, 1,
                10).join().getItems().get(0);
        assertEquals("sta usando bug", stored.getDetails().orElse(""));
    }

    @Test
    void iDettagliTroppoLunghiVengonoRifiutati() {
        ReportCommandHandler handler = handler(reports ->
                reports.put("details", java.util.Collections.singletonMap(
                        "max-length", 5)));

        handler.execute(reporter,
                new String[]{"Target", "other", "descrizione", "molto", "lunga"})
                .join();

        assertTrue(reporter.text().contains("5"));
        assertTrue(reports.listByStatuses(ReportService.ACTIVE_STATUSES, 1, 10)
                .join().isEmpty());
    }

    @Test
    void unErroreDelloStorageDiventaUnMessaggioLocalizzato() {
        ReportCommandHandler handler = handler(
                ReportsTestSupport.configuration(), Language.ITALIAN, true);

        handler.execute(reporter, new String[]{"Target", "cheating"}).join();

        assertEquals(expected("reports.error.repository-error"),
                reporter.text());
        assertFalse(reporter.text().toLowerCase(java.util.Locale.ROOT)
                        .contains("exception"),
                "nessun dettaglio tecnico deve finire in chat");
    }

    @Test
    void ogniDestinatarioLeggeNellaPropriaLingua() {
        ReportCommandHandler handler = handler(
                ReportsTestSupport.configuration(), Language.ENGLISH);

        handler.execute(reporter, new String[]{"Reporter", "cheating"}).join();

        assertEquals(messages.get(Language.ENGLISH,
                        "reports.error.self-report"),
                reporter.text());
    }

    @Test
    void loStaffCollegatoRiceveLaNotifica() {
        FakePlayer staff = new FakePlayer("Staff")
                .allow("legacyreports.staff.view");
        directory.add(staff);
        ReportCommandHandler handler = handler();

        handler.execute(reporter, new String[]{"Target", "cheating"}).join();

        assertEquals(2, staff.received().size(),
                "notifica e riga dei pulsanti");
        assertTrue(staff.text().contains("Target"));
    }

    @Test
    void iSuggerimentiPropongonoNomiEMotivi() {
        ReportCommandHandler handler = handler();

        assertTrue(handler.suggest(reporter, new String[]{"Tar"})
                .contains("Target"));
        assertTrue(handler.suggest(reporter, new String[]{"Target", "che"})
                .contains("cheating"));
        assertTrue(handler.suggest(new FakePlayer("Nessuno"),
                        new String[]{"Tar"}).isEmpty(),
                "senza permesso non si suggerisce nulla");
    }
}
