package it.legacynetwork.reports.command;

import it.legacynetwork.language.Language;
import it.legacynetwork.reports.config.ReportsConfiguration;
import it.legacynetwork.reports.message.ReportLanguageResolver;
import it.legacynetwork.reports.message.ReportMessages;
import it.legacynetwork.reports.message.ReportPresenter;
import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.model.ReportStatus;
import it.legacynetwork.reports.notification.StaffNotificationPreferences;
import it.legacynetwork.reports.repository.InMemoryReportEventRepository;
import it.legacynetwork.reports.repository.InMemoryReportRepository;
import it.legacynetwork.reports.service.ReportService;
import it.legacynetwork.reports.support.FakeConsole;
import it.legacynetwork.reports.support.FakeDirectory;
import it.legacynetwork.reports.support.FakePlayer;
import it.legacynetwork.reports.support.ReportsTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comandi staff: permessi, argomenti e delega al servizio.
 */
class ReportsCommandHandlerTest {

    private InMemoryReportRepository reports;
    private InMemoryReportEventRepository events;
    private ReportService service;
    private ReportMessages messages;
    private FakeDirectory directory;
    private StaffNotificationPreferences preferences;
    private ReportsCommandHandler handler;

    private FakePlayer staff;
    private FakePlayer target;

    @BeforeEach
    void setUp() {
        ReportsConfiguration configuration = ReportsTestSupport.configuration();
        reports = new InMemoryReportRepository();
        events = new InMemoryReportEventRepository();
        service = new ReportService(reports, events,
                () -> ReportsTestSupport.NOW, "proxy-test");
        messages = ReportMessages.load(configuration.getFallbackLanguage());
        ReportPresenter presenter = new ReportPresenter(messages,
                configuration.getReasons());
        ReportLanguageResolver languages = new ReportLanguageResolver(
                playerId -> Language.ITALIAN,
                configuration.getFallbackLanguage());
        preferences = new StaffNotificationPreferences(true);
        directory = new FakeDirectory();
        staff = new FakePlayer("Staff").allow("legacyreports.staff.view",
                "legacyreports.staff.claim", "legacyreports.staff.resolve",
                "legacyreports.staff.history");
        target = new FakePlayer("Target");
        directory.add(staff, target);
        handler = new ReportsCommandHandler(configuration, service, directory,
                presenter, languages, preferences);
    }

    private Report open() {
        Report report = ReportsTestSupport.report(UUID.randomUUID(),
                target.uniqueId(), ReportStatus.OPEN);
        service.create(report).join();
        return report;
    }

    private String expected(String key) {
        return messages.get(Language.ITALIAN, key);
    }

    @Test
    void senzaPermessoNonSiVedeNulla() {
        FakePlayer stranger = new FakePlayer("Stranger");

        handler.execute(stranger, new String[]{"list"}).join();

        assertEquals(expected("reports.error.no-permission"), stranger.text());
        assertTrue(handler.suggest(stranger, new String[]{""}).isEmpty());
    }

    @Test
    void senzaArgomentiSiVedeLElencoDeiComandi() {
        handler.execute(staff, new String[0]).join();

        assertEquals(10, staff.received().size());
        assertTrue(staff.text().contains(
                expected("reports.command.reports.header")));
    }

    @Test
    void lElencoMostraIReportAperti() {
        Report report = open();

        handler.execute(staff, new String[]{"list"}).join();

        assertTrue(staff.text().contains(report.getId().shortCode()));
    }

    @Test
    void lElencoVuotoLoDice() {
        handler.execute(staff, new String[]{"list"}).join();

        assertTrue(staff.text().contains(expected("reports.list.empty")));
    }

    @Test
    void unaPaginaNonValidaVieneRifiutata() {
        handler.execute(staff, new String[]{"list", "zero"}).join();

        assertEquals(expected("reports.error.invalid-page"), staff.text());
    }

    @Test
    void laSchedaMostraLoStorico() {
        Report report = open();

        handler.execute(staff, new String[]{"info",
                report.getId().shortCode()}).join();

        assertTrue(staff.text().contains(
                expected("reports.info.history-header")));
        assertTrue(staff.text().contains(report.getId().shortCode()));
    }

    @Test
    void unIdentificatoreSconosciutoLoDice() {
        handler.execute(staff, new String[]{"info", "abcdef12"}).join();

        assertTrue(staff.text().contains("abcdef12"));
        assertTrue(staff.text().startsWith("&c"));
    }

    @Test
    void ilClaimPassaDalServizio() {
        Report report = open();

        handler.execute(staff, new String[]{"claim",
                report.getId().shortCode()}).join();

        assertEquals(ReportStatus.CLAIMED, service.find(report.getId()).join()
                .orElseThrow().getStatus());
    }

    @Test
    void laConsoleNonPuoPrendereInCarico() {
        Report report = open();
        FakeConsole console = new FakeConsole();

        handler.execute(console, new String[]{"claim",
                report.getId().shortCode()}).join();

        assertEquals(ReportStatus.OPEN, service.find(report.getId()).join()
                .orElseThrow().getStatus());
        assertTrue(console.text().contains(
                messages.get(messages.getFallback(),
                        "reports.error.player-only")));
    }

    @Test
    void lIndagineEuUnaTransizioneCentralizzata() {
        Report report = open();

        handler.execute(staff, new String[]{"investigate",
                report.getId().shortCode()}).join();

        assertEquals(ReportStatus.INVESTIGATING, service.find(report.getId())
                .join().orElseThrow().getStatus());
    }

    @Test
    void ilRilascioRimetteInCoda() {
        Report report = open();
        handler.execute(staff, new String[]{"claim",
                report.getId().shortCode()}).join();

        handler.execute(staff, new String[]{"release",
                report.getId().shortCode()}).join();

        assertEquals(ReportStatus.OPEN, service.find(report.getId()).join()
                .orElseThrow().getStatus());
    }

    @Test
    void archiviareRichiedeUnMotivo() {
        Report report = open();
        handler.execute(staff, new String[]{"investigate",
                report.getId().shortCode()}).join();
        staff.clear();

        handler.execute(staff, new String[]{"dismiss",
                report.getId().shortCode()}).join();

        assertEquals(ReportStatus.INVESTIGATING, service.find(report.getId())
                .join().orElseThrow().getStatus());
        assertEquals(10, staff.received().size(), "viene mostrato l'uso");
    }

    @Test
    void archiviareChiudeIlReport() {
        Report report = open();
        handler.execute(staff, new String[]{"investigate",
                report.getId().shortCode()}).join();

        handler.execute(staff, new String[]{"dismiss",
                report.getId().shortCode(), "nessuna", "prova"}).join();

        Report updated = service.find(report.getId()).join().orElseThrow();
        assertEquals(ReportStatus.DISMISSED, updated.getStatus());
        assertEquals("nessuna prova", updated.getResolution().orElse(null));
    }

    @Test
    void ilProvvedimentoChiudeIlReport() {
        Report report = open();
        handler.execute(staff, new String[]{"investigate",
                report.getId().shortCode()}).join();

        handler.execute(staff, new String[]{"action",
                report.getId().shortCode(), "ban", "definitivo"}).join();

        assertEquals(ReportStatus.ACTION_TAKEN, service.find(report.getId())
                .join().orElseThrow().getStatus());
    }

    @Test
    void loStoricoDelGiocatoreRichiedeIlSuoPermesso() {
        FakePlayer viewer = new FakePlayer("Viewer")
                .allow("legacyreports.staff.view");
        directory.add(viewer);

        handler.execute(viewer, new String[]{"player", "Target"}).join();

        assertEquals(expected("reports.error.no-permission"), viewer.text());
    }

    @Test
    void loStoricoDelGiocatoreMostraISuoiReport() {
        Report report = open();

        handler.execute(staff, new String[]{"player", "Target"}).join();

        assertTrue(staff.text().contains(report.getId().shortCode()));
        assertTrue(staff.text().contains("Target"));
    }

    @Test
    void loStoricoDiUnGiocatoreAssenteLoDice() {
        handler.execute(staff, new String[]{"player", "Fantasma"}).join();

        assertTrue(staff.text().contains("Fantasma"));
    }

    @Test
    void laPreferenzaSulleNotificheSiPuoInvertire() {
        handler.execute(staff, new String[]{"notifications"}).join();

        assertFalse(preferences.isEnabled(staff.uniqueId()));
        assertEquals(expected("reports.success.notifications-off"),
                staff.text());

        staff.clear();
        handler.execute(staff, new String[]{"notifications"}).join();
        assertTrue(preferences.isEnabled(staff.uniqueId()));
    }

    @Test
    void unSottocomandoSconosciutoMostraLUso() {
        handler.execute(staff, new String[]{"inventato"}).join();

        assertEquals(10, staff.received().size());
    }

    @Test
    void iSuggerimentiPropongonoISottocomandi() {
        assertTrue(handler.suggest(staff, new String[]{"cl"})
                .contains("claim"));
        assertTrue(handler.suggest(staff, new String[]{"player", "Tar"})
                .contains("Target"));
    }

    @Test
    void unAmministratoreEreditaOgniPermesso() {
        FakePlayer admin = new FakePlayer("Admin").allow("legacyreports.admin");
        directory.add(admin);
        Report report = open();

        handler.execute(admin, new String[]{"claim",
                report.getId().shortCode()}).join();

        assertEquals(ReportStatus.CLAIMED, service.find(report.getId()).join()
                .orElseThrow().getStatus());
    }
}
