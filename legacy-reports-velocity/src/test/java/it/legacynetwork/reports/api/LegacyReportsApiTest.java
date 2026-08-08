package it.legacynetwork.reports.api;

import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.model.ReportEvent;
import it.legacynetwork.reports.model.ReportEventType;
import it.legacynetwork.reports.model.ReportId;
import it.legacynetwork.reports.model.ReportStatus;
import it.legacynetwork.reports.repository.InMemoryReportEventRepository;
import it.legacynetwork.reports.repository.InMemoryReportRepository;
import it.legacynetwork.reports.service.DefaultLegacyReportsApi;
import it.legacynetwork.reports.service.ReportService;
import it.legacynetwork.reports.support.ReportsTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Superficie che LegacyScreenshare usera' davvero.
 */
class LegacyReportsApiTest {

    private InMemoryReportRepository reports;
    private InMemoryReportEventRepository events;
    private ReportService service;
    private LegacyReportsApi api;

    private final UUID staffId = UUID.randomUUID();
    private final UUID otherStaffId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reports = new InMemoryReportRepository();
        events = new InMemoryReportEventRepository();
        service = new ReportService(reports, events,
                () -> ReportsTestSupport.NOW, "proxy-test");
        api = new DefaultLegacyReportsApi(service);
    }

    private Report investigating() {
        Report report = ReportsTestSupport.report(UUID.randomUUID(),
                UUID.randomUUID(), ReportStatus.OPEN);
        service.create(report).join();
        service.investigate(report.getId(), staffId, "Staff").join();
        return service.find(report.getId()).join().orElseThrow();
    }

    @Test
    void unReportSiTrovaPerIdentificatore() {
        Report report = investigating();

        assertTrue(api.findReport(report.getId()).join().isPresent());
        assertFalse(api.findReport(ReportId.random()).join().isPresent());
    }

    @Test
    void unReportSiTrovaPerRiferimentoBreve() {
        Report report = investigating();

        assertEquals(report.getId(), api
                .findReportByReference(report.getId().shortCode()).join()
                .orElseThrow().getId());
        assertFalse(api.findReportByReference("non-esadecimale").join()
                .isPresent());
    }

    @Test
    void lAvvioDelControlloCollegaLaSessione() {
        Report report = investigating();
        UUID session = UUID.randomUUID();

        ReportOperationResult result = api.markScreenshareStarted(
                report.getId(), staffId, session).join();

        assertEquals(ReportOperationStatus.SUCCESS, result.getStatus());
        Report updated = result.getReport().orElseThrow();
        assertEquals(ReportStatus.SCREENSHARE, updated.getStatus());
        assertEquals(session, updated.getScreenshareId().orElse(null));
    }

    @Test
    void laChiusuraDelControlloTornaAllIndagineSenzaChiudereIlReport() {
        Report report = investigating();
        UUID session = UUID.randomUUID();
        api.markScreenshareStarted(report.getId(), staffId, session).join();

        ReportOperationResult result = api.markScreenshareEnded(report.getId(),
                staffId, session, "screenshare.outcome.clean").join();

        assertEquals(ReportOperationStatus.SUCCESS, result.getStatus());
        assertEquals(ReportStatus.INVESTIGATING,
                result.getReport().orElseThrow().getStatus());
    }

    @Test
    void laChiusuraPuoScegliereIlTipoDiEvento() {
        Report report = investigating();
        UUID session = UUID.randomUUID();
        api.markScreenshareStarted(report.getId(), staffId, session).join();

        api.markScreenshareEnded(report.getId(), staffId, session,
                "screenshare.outcome.violation",
                ReportEventType.SCREENSHARE_VIOLATION).join();

        ReportEvent last = events.all().get(events.all().size() - 1);
        assertEquals(ReportEventType.SCREENSHARE_VIOLATION, last.getType());
        assertEquals("screenshare.outcome.violation",
                last.getMessage().orElse(null));
    }

    @Test
    void unReportChiusoNonAccettaUnControllo() {
        Report report = investigating();
        service.dismiss(report.getId(), staffId, "Staff", "chiuso").join();

        ReportOperationResult result = api.markScreenshareStarted(
                report.getId(), staffId, UUID.randomUUID()).join();

        assertEquals(ReportOperationStatus.ALREADY_RESOLVED,
                result.getStatus());
    }

    @Test
    void unReportInesistenteNonPuoPassareAlControllo() {
        ReportOperationResult result = api.markScreenshareStarted(
                ReportId.random(), staffId, UUID.randomUUID()).join();

        assertEquals(ReportOperationStatus.NOT_FOUND, result.getStatus());
    }

    @Test
    void unoStafferDiversoNonPuoAvviareIlControllo() {
        Report report = investigating();

        ReportOperationResult result = api.markScreenshareStarted(
                report.getId(), otherStaffId, UUID.randomUUID()).join();

        assertEquals(ReportOperationStatus.ALREADY_ASSIGNED,
                result.getStatus());
        assertEquals(ReportStatus.INVESTIGATING, service
                .find(report.getId()).join().orElseThrow().getStatus());
    }

    @Test
    void unEventoDiAuditNonCambiaLoStato() {
        Report report = investigating();

        ReportOperationResult result = api.addAuditEvent(report.getId(),
                staffId, "Staff", ReportEventType.NOTE_ADDED,
                "target disconnesso").join();

        assertEquals(ReportOperationStatus.SUCCESS, result.getStatus());
        assertEquals(ReportStatus.INVESTIGATING, service
                .find(report.getId()).join().orElseThrow().getStatus());
    }

    @Test
    void ripetereLAvvioConLaStessaSessioneEuIdempotente() {
        Report report = investigating();
        UUID session = UUID.randomUUID();
        api.markScreenshareStarted(report.getId(), staffId, session).join();
        int before = events.all().size();

        ReportOperationResult result = api.markScreenshareStarted(
                report.getId(), staffId, session).join();

        assertEquals(ReportOperationStatus.UNCHANGED, result.getStatus());
        assertEquals(before, events.all().size());
    }
}
