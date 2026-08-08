package it.legacynetwork.reports.service;

import it.legacynetwork.reports.api.ReportOperationResult;
import it.legacynetwork.reports.api.ReportOperationStatus;
import it.legacynetwork.reports.api.Report;
import it.legacynetwork.reports.model.ReportEvent;
import it.legacynetwork.reports.api.ReportEventType;
import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.api.ReportStatus;
import it.legacynetwork.reports.model.ReportTransitions;
import it.legacynetwork.reports.repository.InMemoryReportEventRepository;
import it.legacynetwork.reports.repository.InMemoryReportRepository;
import it.legacynetwork.reports.support.FailingReportRepository;
import it.legacynetwork.reports.support.ReportsTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Transizioni di stato: quelle previste devono passare, le altre no.
 */
class ReportServiceTest {

    private Instant now = ReportsTestSupport.NOW;

    private InMemoryReportRepository reports;
    private InMemoryReportEventRepository events;
    private ReportService service;

    private final UUID reporterId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private final UUID otherStaffId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reports = new InMemoryReportRepository();
        events = new InMemoryReportEventRepository();
        service = new ReportService(reports, events, () -> now, "proxy-test");
    }

    private Report open() {
        Report report = ReportsTestSupport.report(reporterId, targetId,
                ReportStatus.OPEN);
        service.create(report).join();
        return report;
    }

    private Report reload(ReportId id) {
        return service.find(id).join().orElseThrow(
                () -> new AssertionError("report scomparso"));
    }

    private Report investigating() {
        Report report = open();
        service.investigate(report.getId(), staffId, "Staff").join();
        return reload(report.getId());
    }

    private Report screenshare(UUID sessionId) {
        Report report = investigating();
        service.markScreenshareStarted(report.getId(), staffId, sessionId)
                .join();
        return reload(report.getId());
    }

    // -------------------------------------------------------- tabella stati

    @Test
    void laTabellaDelleTransizioniRispecchiaLeRegole() {
        assertTrue(ReportTransitions.isAllowed(ReportStatus.OPEN,
                ReportStatus.CLAIMED));
        assertTrue(ReportTransitions.isAllowed(ReportStatus.OPEN,
                ReportStatus.INVESTIGATING));
        assertTrue(ReportTransitions.isAllowed(ReportStatus.CLAIMED,
                ReportStatus.INVESTIGATING));
        assertTrue(ReportTransitions.isAllowed(ReportStatus.CLAIMED,
                ReportStatus.OPEN));
        assertTrue(ReportTransitions.isAllowed(ReportStatus.INVESTIGATING,
                ReportStatus.SCREENSHARE));
        assertTrue(ReportTransitions.isAllowed(ReportStatus.INVESTIGATING,
                ReportStatus.ACTION_TAKEN));
        assertTrue(ReportTransitions.isAllowed(ReportStatus.INVESTIGATING,
                ReportStatus.DISMISSED));
        assertTrue(ReportTransitions.isAllowed(ReportStatus.SCREENSHARE,
                ReportStatus.INVESTIGATING));
        assertTrue(ReportTransitions.isAllowed(ReportStatus.SCREENSHARE,
                ReportStatus.ACTION_TAKEN));
        assertTrue(ReportTransitions.isAllowed(ReportStatus.SCREENSHARE,
                ReportStatus.DISMISSED));
    }

    @Test
    void gliStatiFinaliNonHannoUscite() {
        assertTrue(ReportTransitions.allowedFrom(
                ReportStatus.ACTION_TAKEN).isEmpty());
        assertTrue(ReportTransitions.allowedFrom(
                ReportStatus.DISMISSED).isEmpty());
        assertTrue(ReportStatus.ACTION_TAKEN.isFinal());
        assertTrue(ReportStatus.DISMISSED.isFinal());
        assertFalse(ReportStatus.OPEN.isFinal());
    }

    @Test
    void unaTransizioneNonPrevistaVieneRifiutata() {
        Report report = open();

        ReportOperationResult result = service.markScreenshareStarted(
                report.getId(), staffId, UUID.randomUUID()).join();

        assertEquals(ReportOperationStatus.INVALID_TRANSITION,
                result.getStatus());
        assertEquals(ReportStatus.OPEN, reload(report.getId()).getStatus());
    }

    // ------------------------------------------------------------ presa in carico

    @Test
    void unReportApertoPuoEsserePresoInCarico() {
        Report report = open();

        ReportOperationResult result =
                service.claim(report.getId(), staffId, "Staff").join();

        assertEquals(ReportOperationStatus.SUCCESS, result.getStatus());
        Report updated = reload(report.getId());
        assertEquals(ReportStatus.CLAIMED, updated.getStatus());
        assertEquals(staffId, updated.getAssignedStaffId().orElse(null));
        assertEquals(1L, updated.getRevision());
    }

    @Test
    void unSecondoStafferNonPuoRubareIlReport() {
        Report report = open();
        service.claim(report.getId(), staffId, "Staff").join();

        ReportOperationResult result =
                service.claim(report.getId(), otherStaffId, "Altro").join();

        assertEquals(ReportOperationStatus.ALREADY_ASSIGNED,
                result.getStatus());
        assertEquals(staffId,
                reload(report.getId()).getAssignedStaffId().orElse(null));
    }

    @Test
    void prendereInCaricoDueVolteEuIdempotente() {
        Report report = open();
        service.claim(report.getId(), staffId, "Staff").join();

        ReportOperationResult result =
                service.claim(report.getId(), staffId, "Staff").join();

        assertEquals(ReportOperationStatus.UNCHANGED, result.getStatus());
        assertEquals(1L, reload(report.getId()).getRevision());
        assertEquals(2, events.all().size(), "nessun evento duplicato");
    }

    @Test
    void unaModificaConcorrenteVieneIntercettata() {
        Report report = open();
        Report stale = reload(report.getId());
        service.claim(report.getId(), staffId, "Staff").join();

        // Aggiornamento costruito sulla revisione ormai vecchia.
        boolean applied = reports.update(stale.toBuilder()
                .status(ReportStatus.INVESTIGATING)
                .revision(1L)
                .build(), ReportStatus.OPEN, 0L).join();

        assertFalse(applied);
        assertEquals(ReportStatus.CLAIMED, reload(report.getId()).getStatus());
    }

    @Test
    void ilRilascioRimetteIlReportInCoda() {
        Report report = open();
        service.claim(report.getId(), staffId, "Staff").join();

        ReportOperationResult result =
                service.release(report.getId(), staffId, "Staff", false).join();

        assertEquals(ReportOperationStatus.SUCCESS, result.getStatus());
        Report updated = reload(report.getId());
        assertEquals(ReportStatus.OPEN, updated.getStatus());
        assertFalse(updated.getAssignedStaffId().isPresent());
    }

    @Test
    void soloChiHaIlReportPuoRilasciarlo() {
        Report report = open();
        service.claim(report.getId(), staffId, "Staff").join();

        ReportOperationResult result = service
                .release(report.getId(), otherStaffId, "Altro", false).join();

        assertEquals(ReportOperationStatus.ALREADY_ASSIGNED,
                result.getStatus());
    }

    @Test
    void unAmministratorePuoForzareIlRilascio() {
        Report report = open();
        service.claim(report.getId(), staffId, "Staff").join();

        ReportOperationResult result = service
                .release(report.getId(), otherStaffId, "Admin", true).join();

        assertEquals(ReportOperationStatus.SUCCESS, result.getStatus());
        assertEquals(ReportStatus.OPEN, reload(report.getId()).getStatus());
    }

    // ------------------------------------------------------------- chiusura

    @Test
    void unReportInIndaginePuoEssereArchiviato() {
        Report report = investigating();

        ReportOperationResult result = service.dismiss(report.getId(), staffId,
                "Staff", "nessuna prova").join();

        assertEquals(ReportOperationStatus.SUCCESS, result.getStatus());
        Report updated = reload(report.getId());
        assertEquals(ReportStatus.DISMISSED, updated.getStatus());
        assertEquals("nessuna prova", updated.getResolution().orElse(null));
    }

    @Test
    void unReportChiusoNonSiRiapre() {
        Report report = investigating();
        service.dismiss(report.getId(), staffId, "Staff", "chiuso").join();

        ReportOperationResult claim =
                service.claim(report.getId(), staffId, "Staff").join();
        ReportOperationResult action = service.actionTaken(report.getId(),
                staffId, "Staff", "ban").join();

        assertEquals(ReportOperationStatus.ALREADY_RESOLVED, claim.getStatus());
        assertEquals(ReportOperationStatus.ALREADY_RESOLVED,
                action.getStatus());
        assertEquals(ReportStatus.DISMISSED, reload(report.getId()).getStatus());
    }

    @Test
    void chiudereDueVolteNonProduceUnSecondoEvento() {
        Report report = investigating();
        service.dismiss(report.getId(), staffId, "Staff", "chiuso").join();
        int before = events.all().size();

        ReportOperationResult result = service.dismiss(report.getId(), staffId,
                "Staff", "chiuso").join();

        assertEquals(ReportOperationStatus.UNCHANGED, result.getStatus());
        assertEquals(before, events.all().size());
    }

    @Test
    void unReportNonPresoInCaricoNonPuoEssereChiuso() {
        Report report = open();

        ReportOperationResult result = service.dismiss(report.getId(), staffId,
                "Staff", "chiuso").join();

        assertEquals(ReportOperationStatus.NOT_ASSIGNED, result.getStatus());
    }

    // ---------------------------------------------------------- screenshare

    @Test
    void ilControlloCollegaLaSessioneAlReport() {
        UUID session = UUID.randomUUID();
        Report report = screenshare(session);

        assertEquals(ReportStatus.SCREENSHARE, report.getStatus());
        assertEquals(session, report.getScreenshareId().orElse(null));
    }

    @Test
    void laFineDelControlloRiportaAllIndagine() {
        UUID session = UUID.randomUUID();
        Report report = screenshare(session);

        ReportOperationResult result = service.markScreenshareEnded(
                report.getId(), staffId, session, "screenshare.outcome.clean",
                ReportEventType.SCREENSHARE_ENDED).join();

        assertEquals(ReportOperationStatus.SUCCESS, result.getStatus());
        assertEquals(ReportStatus.INVESTIGATING,
                reload(report.getId()).getStatus());
    }

    @Test
    void ilControlloNonChiudeMaiIlReport() {
        UUID session = UUID.randomUUID();
        Report report = screenshare(session);

        service.markScreenshareEnded(report.getId(), staffId, session,
                "screenshare.outcome.violation",
                ReportEventType.SCREENSHARE_VIOLATION).join();

        assertNotEquals(ReportStatus.ACTION_TAKEN,
                reload(report.getId()).getStatus());
        assertEquals(ReportStatus.INVESTIGATING,
                reload(report.getId()).getStatus());
    }

    @Test
    void chiudereDueVolteIlControlloNonDuplicaLoStorico() {
        UUID session = UUID.randomUUID();
        Report report = screenshare(session);
        service.markScreenshareEnded(report.getId(), staffId, session,
                "screenshare.outcome.clean", null).join();
        int before = events.all().size();

        ReportOperationResult result = service.markScreenshareEnded(
                report.getId(), staffId, session, "screenshare.outcome.clean",
                null).join();

        assertEquals(ReportOperationStatus.UNCHANGED, result.getStatus());
        assertEquals(before, events.all().size());
    }

    @Test
    void unaSessioneDiversaNonPuoChiudereIlControllo() {
        Report report = screenshare(UUID.randomUUID());

        ReportOperationResult result = service.markScreenshareEnded(
                report.getId(), staffId, UUID.randomUUID(), "outcome", null)
                .join();

        assertEquals(ReportOperationStatus.INVALID_TRANSITION,
                result.getStatus());
        assertEquals(ReportStatus.SCREENSHARE,
                reload(report.getId()).getStatus());
    }

    // ---------------------------------------------------------------- audit

    @Test
    void ogniAzioneLasciaUnaRigaDiStorico() {
        Report report = open();
        service.claim(report.getId(), staffId, "Staff").join();
        service.investigate(report.getId(), staffId, "Staff").join();
        service.dismiss(report.getId(), staffId, "Staff", "chiuso").join();

        List<ReportEventType> types = new ArrayList<>();
        for (ReportEvent event : events.all()) {
            types.add(event.getType());
        }
        assertEquals(List.of(ReportEventType.CREATED, ReportEventType.CLAIMED,
                        ReportEventType.INVESTIGATION_STARTED,
                        ReportEventType.DISMISSED),
                types);
    }

    @Test
    void loStoricoRegistraStatoPrecedenteESuccessivo() {
        Report report = open();
        service.claim(report.getId(), staffId, "Staff").join();

        ReportEvent claimed = events.all().get(1);
        assertEquals(ReportStatus.OPEN,
                claimed.getPreviousStatus().orElse(null));
        assertEquals(ReportStatus.CLAIMED,
                claimed.getNewStatus().orElse(null));
        assertEquals(staffId, claimed.getActorId().orElse(null));
        assertEquals("proxy-test", claimed.getProxyId());
    }

    @Test
    void unaNotaNonCambiaLoStato() {
        Report report = investigating();

        ReportOperationResult result = service.addAuditEvent(report.getId(),
                staffId, "Staff", ReportEventType.NOTE_ADDED, "controllato")
                .join();

        assertEquals(ReportOperationStatus.SUCCESS, result.getStatus());
        assertEquals(ReportStatus.INVESTIGATING,
                reload(report.getId()).getStatus());
        assertEquals(ReportEventType.NOTE_ADDED,
                events.all().get(events.all().size() - 1).getType());
    }

    @Test
    void unReportInesistenteNonEsiste() {
        ReportOperationResult result = service.claim(ReportId.random(), staffId,
                "Staff").join();

        assertEquals(ReportOperationStatus.NOT_FOUND, result.getStatus());
    }

    @Test
    void unErroreDelloStorageDiventaUnEsitoDiErrore() {
        ReportService broken = new ReportService(new FailingReportRepository(),
                events, () -> now, "proxy-test");

        ReportOperationResult result =
                broken.claim(ReportId.random(), staffId, "Staff").join();

        assertEquals(ReportOperationStatus.REPOSITORY_ERROR,
                result.getStatus());
    }
}
