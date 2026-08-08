package it.legacynetwork.screenshare.service;

import it.legacynetwork.language.Language;
import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.api.ReportStatus;
import it.legacynetwork.screenshare.model.ScreenshareSession;
import it.legacynetwork.screenshare.model.ScreenshareStatus;
import it.legacynetwork.screenshare.support.FakePlayer;
import it.legacynetwork.screenshare.support.ScreenshareWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trasferimenti: nessuna sessione resta attiva se qualcuno non arriva.
 */
class ScreenshareTransferTest {

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

    @Test
    void seLoStaffNonArrivaLaSessioneFallisce() {
        world.transfers.refuse(staff.uniqueId());

        ScreenshareOperationResult result =
                world.service.start(staff, "Target", null).join();

        assertEquals(ScreenshareOperationStatus.TRANSFER_FAILED,
                result.getStatus());
        assertEquals(ScreenshareStatus.FAILED,
                result.getSession().orElseThrow().getStatus());
        assertTrue(world.sessions.findOpen().join().isEmpty(),
                "nessuna sessione deve restare aperta");
    }

    @Test
    void seIlBersaglioNonArrivaLaSessioneFallisce() {
        world.transfers.refuse(target.uniqueId());

        ScreenshareOperationResult result =
                world.service.start(staff, "Target", null).join();

        assertEquals(ScreenshareOperationStatus.TRANSFER_FAILED,
                result.getStatus());
        assertEquals(ScreenshareStatus.FAILED,
                result.getSession().orElseThrow().getStatus());
    }

    @Test
    void unFallimentoTecnicoNonProduceUnaViolazione() {
        world.transfers.refuse(target.uniqueId());

        world.service.start(staff, "Target", null).join();

        assertEquals(0, world.violations.count(),
                "un guasto non e' una violazione");
    }

    @Test
    void seIlBersaglioSparisceDuranteIlTrasferimentoEuUnaViolazione() {
        world.transfers.refuse(target.uniqueId());
        world.directory.remove(target);

        ScreenshareOperationResult result =
                world.service.start(staff, "Target", null).join();

        assertEquals(ScreenshareOperationStatus.TARGET_NOT_FOUND,
                result.getStatus(),
                "un bersaglio gia' assente non arriva nemmeno alla creazione");
    }

    @Test
    void unFallimentoRipristinaIlReport() {
        ReportId reportId = world.investigatingReport(target.uniqueId(),
                "Target", staff.uniqueId(), "Staff");
        world.transfers.refuse(target.uniqueId());

        world.service.start(staff, "Target", reportId.shortCode()).join();

        assertEquals(ReportStatus.INVESTIGATING, world.reportStatus(reportId));
    }

    @Test
    void unFallimentoRimuoveOgniVincolo() {
        world.transfers.refuse(target.uniqueId());

        world.service.start(staff, "Target", null).join();

        assertFalse(world.registry.isLocked(target.uniqueId()));
        assertFalse(world.registry.sessionOfStaff(staff.uniqueId())
                .isPresent());
    }

    @Test
    void ilRientroUsaIlPrimoServerDiRipiegoDisponibile() {
        world.service.start(staff, "Target", null).join();

        world.service.stop(staff.uniqueId(), "Staff", "Target",
                it.legacynetwork.screenshare.model.ScreenshareOutcome.CLEAN,
                false).join();

        assertTrue(world.transfers.movedTo(target.uniqueId(), "lobby-1"));
        assertFalse(world.transfers.movedTo(target.uniqueId(), "lobby-2"),
                "basta il primo che risponde");
    }

    @Test
    void senzaServerDiRipiegoRegistratiIlVincoloVieneComunqueRimosso() {
        world.transfers.unregister("lobby-1");
        world.transfers.unregister("lobby-2");
        world.service.start(staff, "Target", null).join();

        world.service.stop(staff.uniqueId(), "Staff", "Target",
                it.legacynetwork.screenshare.model.ScreenshareOutcome.CLEAN,
                false).join();

        assertFalse(world.registry.isLocked(target.uniqueId()));
    }

    @Test
    void unTrasferimentoScadutoChiudeLaSessione() {
        ScreenshareWorld slow = new ScreenshareWorld(
                ScreenshareWorld.configuration(), Language.ITALIAN, true);
        slow.directory.add(staff, target);
        // Sessione bloccata in trasferimento: il gateway non risponde mai.
        ScreenshareSession stuck = ScreenshareSession.builder()
                .id(it.legacynetwork.screenshare.model.ScreenshareSessionId
                        .random())
                .target(target.uniqueId(), "Target")
                .staff(staff.uniqueId(), "Staff")
                .serverId(ScreenshareWorld.SCREENSHARE_SERVER)
                .createdAt(slow.now())
                .expiresAt(slow.now().plus(Duration.ofHours(1)))
                .status(ScreenshareStatus.TRANSFERRING)
                .proxyId("proxy-test")
                .revision(0L)
                .build();
        slow.sessions.insert(stuck).join();

        slow.advance(Duration.ofSeconds(60));
        int closed = slow.service.tick().join();

        assertEquals(1, closed);
        assertEquals(ScreenshareStatus.FAILED, slow.service.find(stuck.getId())
                .join().orElseThrow().getStatus());
    }

    @Test
    void unaSessioneTroppoLungaVieneAnnullata() {
        ScreenshareSession session = world.service.start(staff, "Target", null)
                .join().getSession().orElseThrow();

        world.advance(Duration.ofHours(2));
        int closed = world.service.tick().join();

        assertEquals(1, closed);
        assertEquals(ScreenshareStatus.CANCELLED,
                world.service.find(session.getId()).join().orElseThrow()
                        .getStatus());
        assertEquals(0, world.violations.count(),
                "il tempo scaduto non e' una violazione del giocatore");
    }

    @Test
    void unaSessioneNeiTempiNonVieneToccata() {
        world.service.start(staff, "Target", null).join();

        world.advance(Duration.ofMinutes(1));

        assertEquals(0, world.service.tick().join());
        assertEquals(1, world.sessions.findOpen().join().size());
    }

    @Test
    void ilTrasferimentoVieneTentatoUnaSolaVoltaPerGiocatore() {
        world.service.start(staff, "Target", null).join();

        List<String> attempts = world.transfers.attempts();
        long staffMoves = attempts.stream()
                .filter(entry -> entry.startsWith(staff.uniqueId().toString()))
                .count();
        assertEquals(1L, staffMoves);
    }
}
