package it.legacynetwork.screenshare.reports;

import it.legacynetwork.language.Language;
import it.legacynetwork.reports.api.LegacyReportsApi;
import it.legacynetwork.reports.api.ReportOperationResult;
import it.legacynetwork.reports.api.ReportOperationStatus;
import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.model.ReportEventType;
import it.legacynetwork.reports.model.ReportId;
import it.legacynetwork.reports.model.ReportStatus;
import it.legacynetwork.screenshare.model.ScreenshareOutcome;
import it.legacynetwork.screenshare.service.ScreenshareOperationStatus;
import it.legacynetwork.screenshare.support.FakePlayer;
import it.legacynetwork.screenshare.support.ScreenshareWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integrazione con LegacyReports attraverso la sua API pubblica.
 *
 * <p>Non esiste alcuna copia locale di {@code Report}: qui si usano i tipi
 * reali del plugin vicino.</p>
 */
class ReportIntegrationTest {

    private ScreenshareWorld world;
    private FakePlayer staff;
    private FakePlayer target;

    @BeforeEach
    void setUp() {
        world = new ScreenshareWorld();
        staff = new FakePlayer("Staff");
        target = new FakePlayer("Target");
        world.directory.add(staff, target);
    }

    private ReportId linkedReport() {
        return world.investigatingReport(target.uniqueId(), "Target",
                staff.uniqueId(), "Staff");
    }

    @Test
    void lAvvioMarcaIlReportComeInControllo() {
        ReportId reportId = linkedReport();

        world.service.start(staff, "Target", reportId.shortCode()).join();

        assertEquals(ReportStatus.SCREENSHARE, world.reportStatus(reportId));
        Report report = world.reportService.find(reportId).join().orElseThrow();
        assertTrue(report.getScreenshareId().isPresent());
    }

    @Test
    void loStoricoDelReportRegistraLAvvioELaChiusura() {
        ReportId reportId = linkedReport();
        world.service.start(staff, "Target", reportId.shortCode()).join();
        world.service.stop(staff.uniqueId(), "Staff", "Target",
                ScreenshareOutcome.CLEAN, false).join();

        boolean started = world.reportEvents.all().stream().anyMatch(event ->
                event.getType() == ReportEventType.SCREENSHARE_STARTED);
        boolean ended = world.reportEvents.all().stream().anyMatch(event ->
                event.getType() == ReportEventType.SCREENSHARE_ENDED);

        assertTrue(started);
        assertTrue(ended);
    }

    @Test
    void unaViolazioneUsaIlProprioTipoDiEvento() {
        ReportId reportId = linkedReport();
        world.service.start(staff, "Target", reportId.shortCode()).join();
        world.directory.remove(target);

        world.service.onTargetDisconnect(target.uniqueId()).join();

        assertTrue(world.reportEvents.all().stream().anyMatch(event ->
                event.getType() == ReportEventType.SCREENSHARE_VIOLATION));
        assertEquals(ReportStatus.INVESTIGATING, world.reportStatus(reportId));
    }

    @Test
    void unAnnullamentoUsaIlProprioTipoDiEvento() {
        ReportId reportId = linkedReport();
        world.service.start(staff, "Target", reportId.shortCode()).join();

        world.service.cancel(staff.uniqueId(), "Staff", "Target", "errore",
                false).join();

        assertTrue(world.reportEvents.all().stream().anyMatch(event ->
                event.getType() == ReportEventType.SCREENSHARE_CANCELLED));
    }

    @Test
    void unFallimentoUsaIlProprioTipoDiEvento() {
        ReportId reportId = linkedReport();
        world.transfers.refuse(target.uniqueId());

        world.service.start(staff, "Target", reportId.shortCode()).join();

        assertTrue(world.reportEvents.all().stream().anyMatch(event ->
                event.getType() == ReportEventType.SCREENSHARE_FAILED));
        assertEquals(ReportStatus.INVESTIGATING, world.reportStatus(reportId));
    }

    @Test
    void unaNotaFinisceAncheNelloStoricoDelReport() {
        ReportId reportId = linkedReport();
        world.service.start(staff, "Target", reportId.shortCode()).join();

        world.service.note(staff.uniqueId(), "Staff", "Target",
                "cartelle controllate").join();

        assertTrue(world.reportEvents.all().stream().anyMatch(event ->
                event.getType() == ReportEventType.NOTE_ADDED
                        && "cartelle controllate".equals(
                                event.getMessage().orElse(""))));
    }

    @Test
    void seIlReportRifiutaIlCollegamentoLaSessioneNonResta() {
        ReportId reportId = linkedReport();
        // Un altro staffer ha gia' portato il report al controllo.
        world.reportService.markScreenshareStarted(reportId, staff.uniqueId(),
                UUID.randomUUID()).join();

        var result = world.service.start(staff, "Target",
                reportId.shortCode()).join();

        assertEquals(ScreenshareOperationStatus.INVALID_TRANSITION,
                result.getStatus());
        assertTrue(world.sessions.findOpen().join().isEmpty());
    }

    @Test
    void senzaApiIlCollegamentoNonEuDisponibile() {
        ReportLink link = ReportLink.unavailable();

        assertFalse(link.isAvailable());
        assertFalse(link.findReport("abcdef12").join().isPresent());
        assertFalse(link.markStarted(ReportId.random(), UUID.randomUUID(),
                UUID.randomUUID()).join().isPresent());
        assertFalse(link.markEnded(ReportId.random(), UUID.randomUUID(),
                UUID.randomUUID(), "outcome",
                ReportEventType.SCREENSHARE_ENDED).join().isPresent());
        assertFalse(link.addAudit(ReportId.random(), UUID.randomUUID(), "x",
                ReportEventType.NOTE_ADDED, "y").join().isPresent());
    }

    @Test
    void unApiInErroreNonPropagaLEccezione() {
        ReportLink link = new ReportLink(new FailingReportsApi());

        assertTrue(link.isAvailable());
        assertFalse(link.findReport("abcdef12").join().isPresent());
        assertFalse(link.markStarted(ReportId.random(), UUID.randomUUID(),
                UUID.randomUUID()).join().isPresent());
    }

    @Test
    void unaSessioneSenzaReportNonToccaNulla() {
        ScreenshareWorld isolated = new ScreenshareWorld(
                ScreenshareWorld.configuration(), Language.ITALIAN, true);
        isolated.directory.add(staff, target);

        isolated.service.start(staff, "Target", null).join();
        isolated.service.stop(staff.uniqueId(), "Staff", "Target",
                ScreenshareOutcome.CLEAN, false).join();

        assertTrue(isolated.reportEvents.all().isEmpty());
    }

    /** API che fallisce sempre: nessun errore deve arrivare in chat. */
    private static final class FailingReportsApi implements LegacyReportsApi {

        @Override
        public CompletableFuture<Optional<Report>> findReport(ReportId id) {
            return failed();
        }

        @Override
        public CompletableFuture<Optional<Report>> findReportByReference(
                String reference) {
            return failed();
        }

        @Override
        public CompletableFuture<ReportOperationResult> markScreenshareStarted(
                ReportId id, UUID staffId, UUID sessionId) {
            return failed();
        }

        @Override
        public CompletableFuture<ReportOperationResult> markScreenshareEnded(
                ReportId id, UUID staffId, UUID sessionId, String outcomeKey) {
            return failed();
        }

        @Override
        public CompletableFuture<ReportOperationResult> markScreenshareEnded(
                ReportId id, UUID staffId, UUID sessionId, String outcomeKey,
                ReportEventType auditType) {
            return failed();
        }

        @Override
        public CompletableFuture<ReportOperationResult> addAuditEvent(
                ReportId id, UUID actorId, String actorName,
                ReportEventType type, String message) {
            return failed();
        }

        private static <T> CompletableFuture<T> failed() {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(
                    new IllegalStateException("API non raggiungibile"));
            return future;
        }
    }

    @Test
    void loStatoDiErroreDelReportRestaLeggibile() {
        assertEquals("reports.error.already-resolved",
                ReportOperationStatus.ALREADY_RESOLVED.messageKey());
    }
}
