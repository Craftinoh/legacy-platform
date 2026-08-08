package it.legacynetwork.screenshare.service;

import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.api.ReportStatus;
import it.legacynetwork.screenshare.model.ScreenshareEventType;
import it.legacynetwork.screenshare.model.ScreenshareOutcome;
import it.legacynetwork.screenshare.model.ScreenshareSession;
import it.legacynetwork.screenshare.model.ScreenshareStatus;
import it.legacynetwork.screenshare.model.ScreenshareTransitions;
import it.legacynetwork.screenshare.support.FakePlayer;
import it.legacynetwork.screenshare.support.ScreenshareWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ciclo di vita di una sessione di controllo.
 */
class ScreenshareSessionTest {

    private ScreenshareWorld world;
    private FakePlayer staff;
    private FakePlayer target;

    @BeforeEach
    void setUp() {
        world = new ScreenshareWorld();
        staff = new FakePlayer("Staff")
                .allow("legacyscreenshare.staff.start",
                        "legacyscreenshare.staff.stop",
                        "legacyscreenshare.staff.view");
        target = new FakePlayer("Target");
        world.directory.add(staff, target);
    }

    private ScreenshareSession start() {
        ScreenshareOperationResult result =
                world.service.start(staff, "Target", null).join();
        assertTrue(result.isApplied(), "avvio fallito: " + result.getStatus());
        return result.getSession().orElseThrow();
    }

    private ScreenshareSession reload(ScreenshareSession session) {
        return world.service.find(session.getId()).join().orElseThrow();
    }

    // -------------------------------------------------------------- creazione

    @Test
    void unControlloParteEDiventaAttivo() {
        ScreenshareSession session = start();

        assertEquals(ScreenshareStatus.ACTIVE, session.getStatus());
        assertEquals(ScreenshareWorld.SCREENSHARE_SERVER,
                session.getServerId());
        assertTrue(session.getStartedAt().isPresent());
        assertTrue(world.transfers.movedTo(staff.uniqueId(),
                ScreenshareWorld.SCREENSHARE_SERVER));
        assertTrue(world.transfers.movedTo(target.uniqueId(),
                ScreenshareWorld.SCREENSHARE_SERVER));
    }

    @Test
    void loStaffSiSpostaPrimaDelBersaglio() {
        start();

        List<String> attempts = world.transfers.attempts();
        assertEquals(staff.uniqueId() + "->"
                + ScreenshareWorld.SCREENSHARE_SERVER, attempts.get(0));
        assertEquals(target.uniqueId() + "->"
                + ScreenshareWorld.SCREENSHARE_SERVER, attempts.get(1));
    }

    @Test
    void unBersaglioGiaControllatoNonSiControllaDueVolte() {
        start();
        FakePlayer other = new FakePlayer("Altro")
                .allow("legacyscreenshare.staff.start");
        world.directory.add(other);

        ScreenshareOperationResult result =
                world.service.start(other, "Target", null).join();

        assertEquals(ScreenshareOperationStatus.TARGET_BUSY,
                result.getStatus());
    }

    @Test
    void unoStafferOccupatoNonNeApreUnSecondo() {
        start();
        FakePlayer second = new FakePlayer("Secondo");
        world.directory.add(second);

        ScreenshareOperationResult result =
                world.service.start(staff, "Secondo", null).join();

        assertEquals(ScreenshareOperationStatus.STAFF_BUSY,
                result.getStatus());
    }

    @Test
    void nonCiSiControllaDaSoli() {
        ScreenshareOperationResult result =
                world.service.start(staff, "Staff", null).join();

        assertEquals(ScreenshareOperationStatus.SELF_TARGET,
                result.getStatus());
    }

    @Test
    void unBersaglioNonCollegatoNonEuControllabile() {
        ScreenshareOperationResult result =
                world.service.start(staff, "Fantasma", null).join();

        assertEquals(ScreenshareOperationStatus.TARGET_NOT_FOUND,
                result.getStatus());
    }

    @Test
    void senzaServerConfiguratoIlControlloNonParte() {
        ScreenshareWorld bare = new ScreenshareWorld(
                ScreenshareWorld.configuration(section ->
                        section.put("server", "")),
                it.legacynetwork.language.Language.ITALIAN, true);
        bare.directory.add(staff, target);

        ScreenshareOperationResult result =
                bare.service.start(staff, "Target", null).join();

        assertEquals(ScreenshareOperationStatus.SERVER_NOT_CONFIGURED,
                result.getStatus());
    }

    @Test
    void unServerNonRegistratoBloccaLAvvio() {
        world.transfers.unregister(ScreenshareWorld.SCREENSHARE_SERVER);

        ScreenshareOperationResult result =
                world.service.start(staff, "Target", null).join();

        assertEquals(ScreenshareOperationStatus.SERVER_NOT_REGISTERED,
                result.getStatus());
    }

    // ---------------------------------------------------------------- report

    @Test
    void unReportValidoPassaAlControllo() {
        ReportId reportId = world.investigatingReport(target.uniqueId(),
                "Target", staff.uniqueId(), "Staff");

        ScreenshareOperationResult result = world.service.start(staff,
                "Target", reportId.shortCode()).join();

        assertTrue(result.isApplied());
        assertEquals(ReportStatus.SCREENSHARE, world.reportStatus(reportId));
        assertEquals(reportId.value(),
                result.getSession().orElseThrow().getReportId().orElse(null));
    }

    @Test
    void unReportDiUnAltroGiocatoreVieneRifiutato() {
        ReportId reportId = world.investigatingReport(UUID.randomUUID(),
                "Altro", staff.uniqueId(), "Staff");

        ScreenshareOperationResult result = world.service.start(staff,
                "Target", reportId.shortCode()).join();

        assertEquals(ScreenshareOperationStatus.REPORT_TARGET_MISMATCH,
                result.getStatus());
        assertTrue(world.sessions.findOpen().join().isEmpty());
    }

    @Test
    void unReportInesistenteVieneRifiutato() {
        ScreenshareOperationResult result = world.service.start(staff,
                "Target", "abcdef12").join();

        assertEquals(ScreenshareOperationStatus.REPORT_NOT_FOUND,
                result.getStatus());
    }

    @Test
    void unReportChiusoVieneRifiutato() {
        ReportId reportId = world.investigatingReport(target.uniqueId(),
                "Target", staff.uniqueId(), "Staff");
        world.reportService.dismiss(reportId, staff.uniqueId(), "Staff",
                "chiuso").join();

        ScreenshareOperationResult result = world.service.start(staff,
                "Target", reportId.shortCode()).join();

        assertEquals(ScreenshareOperationStatus.REPORT_FINAL,
                result.getStatus());
    }

    @Test
    void senzaLApiDeiReportUnRiferimentoVieneRifiutato() {
        ScreenshareWorld isolated = new ScreenshareWorld(
                ScreenshareWorld.configuration(),
                it.legacynetwork.language.Language.ITALIAN, false);
        isolated.directory.add(staff, target);

        ScreenshareOperationResult result = isolated.service.start(staff,
                "Target", "abcdef12").join();

        assertEquals(ScreenshareOperationStatus.REPORTS_UNAVAILABLE,
                result.getStatus());
    }

    // -------------------------------------------------------------- chiusura

    @Test
    void unControlloPulitoRiportaIlReportAllIndagine() {
        ReportId reportId = world.investigatingReport(target.uniqueId(),
                "Target", staff.uniqueId(), "Staff");
        world.service.start(staff, "Target", reportId.shortCode()).join();

        ScreenshareOperationResult result = world.service.stop(
                staff.uniqueId(), "Staff", "Target", ScreenshareOutcome.CLEAN,
                false).join();

        assertTrue(result.isApplied());
        assertEquals(ScreenshareStatus.COMPLETED,
                result.getSession().orElseThrow().getStatus());
        assertEquals(ReportStatus.INVESTIGATING, world.reportStatus(reportId));
    }

    @Test
    void unControlloNonChiudeMaiIlReport() {
        ReportId reportId = world.investigatingReport(target.uniqueId(),
                "Target", staff.uniqueId(), "Staff");
        world.service.start(staff, "Target", reportId.shortCode()).join();

        world.service.stop(staff.uniqueId(), "Staff", "Target",
                ScreenshareOutcome.VIOLATION, false).join();

        assertEquals(ReportStatus.INVESTIGATING, world.reportStatus(reportId));
        assertFalse(world.reportStatus(reportId).isFinal());
    }

    @Test
    void laCancellazioneRiportaIlReportAllIndagine() {
        ReportId reportId = world.investigatingReport(target.uniqueId(),
                "Target", staff.uniqueId(), "Staff");
        world.service.start(staff, "Target", reportId.shortCode()).join();

        ScreenshareOperationResult result = world.service.cancel(
                staff.uniqueId(), "Staff", "Target", "falso allarme", false)
                .join();

        assertEquals(ScreenshareStatus.CANCELLED,
                result.getSession().orElseThrow().getStatus());
        assertEquals(ReportStatus.INVESTIGATING, world.reportStatus(reportId));
    }

    @Test
    void unAltroStafferNonChiudeIlControlloAltrui() {
        start();

        ScreenshareOperationResult result = world.service.stop(
                UUID.randomUUID(), "Altro", "Target",
                ScreenshareOutcome.CLEAN, false).join();

        assertEquals(ScreenshareOperationStatus.NOT_OWNER, result.getStatus());
        assertEquals(ScreenshareStatus.ACTIVE, world.sessions.findOpenByTarget(
                target.uniqueId()).join().orElseThrow().getStatus());
    }

    @Test
    void unAmministratorePuoChiudereIlControlloAltrui() {
        start();

        ScreenshareOperationResult result = world.service.stop(
                UUID.randomUUID(), "Admin", "Target",
                ScreenshareOutcome.CLEAN, true).join();

        assertTrue(result.isApplied());
    }

    @Test
    void chiudereDueVolteNonProduceUnSecondoEffetto() {
        start();
        world.service.stop(staff.uniqueId(), "Staff", "Target",
                ScreenshareOutcome.CLEAN, false).join();

        ScreenshareOperationResult second = world.service.stop(
                staff.uniqueId(), "Staff", "Target", ScreenshareOutcome.CLEAN,
                false).join();

        assertEquals(ScreenshareOperationStatus.NO_SESSION,
                second.getStatus());
    }

    @Test
    void unaNotaNonCambiaLoStato() {
        ScreenshareSession session = start();

        ScreenshareOperationResult result = world.service.note(
                staff.uniqueId(), "Staff", "Target", "schermo condiviso")
                .join();

        assertTrue(result.isApplied());
        ScreenshareSession updated = reload(session);
        assertEquals(ScreenshareStatus.ACTIVE, updated.getStatus());
        assertTrue(updated.getNotes().orElse("").contains("schermo condiviso"));
    }

    // ------------------------------------------------------------ transizioni

    @Test
    void laTabellaDelleTransizioniRispecchiaLeRegole() {
        assertTrue(ScreenshareTransitions.isAllowed(ScreenshareStatus.CREATED,
                ScreenshareStatus.TRANSFERRING));
        assertTrue(ScreenshareTransitions.isAllowed(
                ScreenshareStatus.TRANSFERRING, ScreenshareStatus.ACTIVE));
        assertTrue(ScreenshareTransitions.isAllowed(ScreenshareStatus.ACTIVE,
                ScreenshareStatus.COMPLETED));
        assertFalse(ScreenshareTransitions.isAllowed(ScreenshareStatus.ACTIVE,
                ScreenshareStatus.TRANSFERRING));
        for (ScreenshareStatus status : ScreenshareStatus.values()) {
            if (status.isFinal()) {
                assertTrue(ScreenshareTransitions.allowedFrom(status).isEmpty(),
                        "stato finale con uscite: " + status);
            }
        }
    }

    @Test
    void ogniPassaggioLasciaUnaRigaDiStorico() {
        ScreenshareSession session = start();
        world.service.stop(staff.uniqueId(), "Staff", "Target",
                ScreenshareOutcome.CLEAN, false).join();

        List<ScreenshareEventType> types = new ArrayList<>();
        for (var event : world.events.all()) {
            if (event.getSessionId().equals(session.getId())) {
                types.add(event.getType());
            }
        }
        assertEquals(List.of(ScreenshareEventType.CREATED,
                        ScreenshareEventType.TRANSFER_STARTED,
                        ScreenshareEventType.SESSION_ACTIVE,
                        ScreenshareEventType.COMPLETED),
                types);
    }

    @Test
    void ilVincoloVieneRimossoAllaChiusura() {
        ScreenshareSession session = start();
        assertTrue(world.registry.isLocked(target.uniqueId()));

        world.service.stop(staff.uniqueId(), "Staff", "Target",
                ScreenshareOutcome.CLEAN, false).join();

        assertFalse(world.registry.isLocked(target.uniqueId()));
        assertEquals(Optional.empty(),
                world.registry.sessionOfStaff(staff.uniqueId()));
        assertTrue(world.transfers.movedTo(target.uniqueId(), "lobby-1"),
                "il bersaglio va riportato su un server di rientro");
        assertEquals(ScreenshareStatus.COMPLETED, reload(session).getStatus());
    }
}
