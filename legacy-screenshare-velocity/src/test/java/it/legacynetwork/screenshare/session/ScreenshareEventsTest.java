package it.legacynetwork.screenshare.session;

import it.legacynetwork.language.Language;
import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.api.ReportStatus;
import it.legacynetwork.screenshare.model.ScreenshareSession;
import it.legacynetwork.screenshare.model.ScreenshareStatus;
import it.legacynetwork.screenshare.service.ScreenshareOperationStatus;
import it.legacynetwork.screenshare.support.FakePlayer;
import it.legacynetwork.screenshare.support.ScreenshareWorld;
import it.legacynetwork.screenshare.violation.ScreenshareViolation;
import it.legacynetwork.screenshare.violation.ScreenshareViolationType;
import it.legacynetwork.screenshare.violation.SuggestedPunishmentCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Eventi di rete durante un controllo: cambi server, comandi, disconnessioni.
 */
class ScreenshareEventsTest {

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

    private ScreenshareSession start() {
        return world.service.start(staff, "Target", null).join()
                .getSession().orElseThrow();
    }

    // ---------------------------------------------------------- cambi server

    @Test
    void ilBersaglioNonPuoAndareAltrove() {
        start();

        ServerSwitchPolicy.Verdict verdict =
                world.switchPolicy.evaluate(target.uniqueId(), "bedwars-1");

        assertFalse(verdict.isAllowed());
        assertEquals("screenshare.target.server-blocked",
                verdict.getMessageKey().orElse(""));
    }

    @Test
    void ilServerDiControlloRestaRaggiungibile() {
        start();

        assertTrue(world.switchPolicy.evaluate(target.uniqueId(),
                ScreenshareWorld.SCREENSHARE_SERVER).isAllowed());
    }

    @Test
    void iServerDiRientroSiApronoSoloInChiusura() {
        start();

        assertFalse(world.switchPolicy.evaluate(target.uniqueId(), "lobby-1")
                .isAllowed());
        world.registry.allowCleanup(target.uniqueId());
        assertTrue(world.switchPolicy.evaluate(target.uniqueId(), "lobby-1")
                .isAllowed());
        assertFalse(world.switchPolicy.evaluate(target.uniqueId(), "bedwars-1")
                .isAllowed(), "solo i server di rientro configurati");
    }

    @Test
    void chiNonEuSottoControlloSiMuoveLiberamente() {
        start();

        assertTrue(world.switchPolicy.evaluate(staff.uniqueId(), "bedwars-1")
                .isAllowed());
    }

    @Test
    void loStaffPuoEssereLegatoSeLaConfigurazioneLoChiede() {
        ScreenshareWorld locked = new ScreenshareWorld(
                ScreenshareWorld.configuration(section ->
                        section.put("lock-staff-server", true)),
                Language.ITALIAN, true);
        locked.directory.add(staff, target);
        locked.service.start(staff, "Target", null).join();

        assertFalse(locked.switchPolicy.evaluate(staff.uniqueId(), "bedwars-1")
                .isAllowed());
        assertTrue(locked.switchPolicy.evaluate(staff.uniqueId(),
                ScreenshareWorld.SCREENSHARE_SERVER).isAllowed());
    }

    // -------------------------------------------------------------- comandi

    @Test
    void iComandiConsentitiPassano() {
        start();

        assertTrue(world.commandPolicy.isAllowed(target.uniqueId(),
                "msg Staff sono qui"));
        assertTrue(world.commandPolicy.isAllowed(target.uniqueId(), "/reply ok"));
    }

    @Test
    void gliAltriComandiVengonoBloccati() {
        start();

        assertFalse(world.commandPolicy.isAllowed(target.uniqueId(),
                "hub"));
        assertFalse(world.commandPolicy.isAllowed(target.uniqueId(),
                "/velocity:server lobby-1"));
    }

    @Test
    void chiNonEuSottoControlloUsaQualsiasiComando() {
        start();

        assertTrue(world.commandPolicy.isAllowed(staff.uniqueId(), "hub"));
    }

    @Test
    void lEtichettaDelComandoIgnoraBarraENamespace() {
        assertEquals("server", TargetCommandPolicy.label("/velocity:server x"));
        assertEquals("msg", TargetCommandPolicy.label("MSG ciao"));
        assertEquals("", TargetCommandPolicy.label(null));
    }

    // -------------------------------------------------------- disconnessioni

    @Test
    void ilBersaglioCheSiScollegaChiudeInViolazione() {
        ScreenshareSession session = start();
        world.directory.remove(target);

        world.service.onTargetDisconnect(target.uniqueId()).join();

        assertEquals(ScreenshareStatus.VIOLATION,
                world.service.find(session.getId()).join().orElseThrow()
                        .getStatus());
        assertEquals(1, world.violations.count());
    }

    @Test
    void laViolazioneEuUnaRichiestaStrutturata() {
        ScreenshareSession session = start();
        world.directory.remove(target);

        world.service.onTargetDisconnect(target.uniqueId()).join();

        ScreenshareViolation violation = world.violations.last();
        assertEquals(target.uniqueId(), violation.getTargetId());
        assertEquals("Target", violation.getTargetName());
        assertEquals(staff.uniqueId(), violation.getStaffId().orElse(null));
        assertEquals(session.getId(), violation.getSessionId());
        assertEquals(ScreenshareViolationType.TARGET_DISCONNECTED,
                violation.getType());
        assertEquals(SuggestedPunishmentCategory.SCREENSHARE_EVASION,
                violation.getSuggestedCategory());
        assertEquals(ScreenshareWorld.SCREENSHARE_SERVER,
                violation.getContext().get("server"));
        assertFalse(violation.getReportId().isPresent(),
                "questa sessione non era collegata a un report");
    }

    @Test
    void unSecondoDisconnectNonEmetteUnaSecondaViolazione() {
        start();
        world.directory.remove(target);

        world.service.onTargetDisconnect(target.uniqueId()).join();
        world.service.onTargetDisconnect(target.uniqueId()).join();

        assertEquals(1, world.violations.count());
    }

    @Test
    void laViolazioneRiportaIlReportAllIndagine() {
        ReportId reportId = world.investigatingReport(target.uniqueId(),
                "Target", staff.uniqueId(), "Staff");
        world.service.start(staff, "Target", reportId.shortCode()).join();
        world.directory.remove(target);

        world.service.onTargetDisconnect(target.uniqueId()).join();

        assertEquals(ReportStatus.INVESTIGATING, world.reportStatus(reportId));
    }

    @Test
    void laViolazioneRimuoveIlVincolo() {
        start();
        world.directory.remove(target);

        world.service.onTargetDisconnect(target.uniqueId()).join();

        assertFalse(world.registry.isLocked(target.uniqueId()));
    }

    @Test
    void loStaffCheSiScollegaAnnullaConLaPoliticaPredefinita() {
        ScreenshareSession session = start();
        world.directory.remove(staff);

        world.service.onStaffDisconnect(staff.uniqueId()).join();

        assertEquals(ScreenshareStatus.CANCELLED,
                world.service.find(session.getId()).join().orElseThrow()
                        .getStatus());
        assertEquals(0, world.violations.count());
    }

    @Test
    void conLaPoliticaDiAttesaLaSessioneRestaAperta() {
        ScreenshareWorld waiting = new ScreenshareWorld(
                ScreenshareWorld.configuration(section -> section.put(
                        "staff-disconnect-policy", "KEEP_ACTIVE_FOR_SECONDS")),
                Language.ITALIAN, true);
        waiting.directory.add(staff, target);
        ScreenshareSession session = waiting.service.start(staff, "Target",
                null).join().getSession().orElseThrow();
        waiting.directory.remove(staff);

        waiting.service.onStaffDisconnect(staff.uniqueId()).join();

        assertEquals(ScreenshareStatus.ACTIVE, waiting.service
                .find(session.getId()).join().orElseThrow().getStatus());
    }

    @Test
    void loStaffCheRientraInTempoSalvaLaSessione() {
        ScreenshareWorld waiting = new ScreenshareWorld(
                ScreenshareWorld.configuration(section -> section.put(
                        "staff-disconnect-policy", "KEEP_ACTIVE_FOR_SECONDS")),
                Language.ITALIAN, true);
        waiting.directory.add(staff, target);
        ScreenshareSession session = waiting.service.start(staff, "Target",
                null).join().getSession().orElseThrow();
        waiting.directory.remove(staff);
        waiting.service.onStaffDisconnect(staff.uniqueId()).join();

        waiting.directory.add(staff);
        waiting.service.onStaffReconnect(staff.uniqueId()).join();
        waiting.advance(Duration.ofMinutes(5));
        waiting.service.tick().join();

        assertEquals(ScreenshareStatus.ACTIVE, waiting.service
                .find(session.getId()).join().orElseThrow().getStatus());
    }

    @Test
    void unoStaffCheNonRientraFaScadereLaSessione() {
        ScreenshareWorld waiting = new ScreenshareWorld(
                ScreenshareWorld.configuration(section -> section.put(
                        "staff-disconnect-policy", "KEEP_ACTIVE_FOR_SECONDS")),
                Language.ITALIAN, true);
        waiting.directory.add(staff, target);
        ScreenshareSession session = waiting.service.start(staff, "Target",
                null).join().getSession().orElseThrow();
        waiting.directory.remove(staff);
        waiting.service.onStaffDisconnect(staff.uniqueId()).join();

        waiting.advance(Duration.ofSeconds(120));
        int closed = waiting.service.tick().join();

        assertEquals(1, closed);
        assertEquals(ScreenshareStatus.CANCELLED, waiting.service
                .find(session.getId()).join().orElseThrow().getStatus());
    }

    @Test
    void senzaSessioneUnaDisconnessioneNonFaNulla() {
        assertEquals(ScreenshareOperationStatus.NO_SESSION, world.service
                .onTargetDisconnect(target.uniqueId()).join().getStatus());
        assertEquals(ScreenshareOperationStatus.NO_SESSION, world.service
                .onStaffDisconnect(staff.uniqueId()).join().getStatus());
    }

    // --------------------------------------------------------------- avvio

    @Test
    void ilRipristinoNonAccusaSubitoChiEuAncoraOffline() {
        ScreenshareSession session=start(); world.directory.remove(target); assertEquals(0,world.recoverWithFreshRegistry());
        assertEquals(ScreenshareStatus.ACTIVE,world.service.find(session.getId()).join().orElseThrow().getStatus()); assertEquals(0,world.violations.count()); assertTrue(world.registry.isLocked(target.uniqueId()));
    }
    @Test
    void ilRientroDiEntrambeLePartiCompletaIlRecoveryUnaSolaVolta() {
        ScreenshareSession session=start(); world.directory.remove(target); world.directory.remove(staff); world.recoverWithFreshRegistry();
        world.directory.add(target); world.service.onPlayerReconnect(target.uniqueId()).join(); world.directory.add(staff); world.service.onPlayerReconnect(staff.uniqueId()).join(); world.service.onPlayerReconnect(staff.uniqueId()).join();
        assertEquals(ScreenshareStatus.ACTIVE,world.service.find(session.getId()).join().orElseThrow().getStatus());
        assertEquals(1L,world.events.all().stream().filter(e -> e.getSessionId().equals(session.getId())).filter(e -> e.getType()==it.legacynetwork.screenshare.model.ScreenshareEventType.RECOVERY_COMPLETED).count());
    }
    @Test
    void laScadenzaRecoveryDistingueLAssenzaDelBersaglio() {
        ScreenshareSession session=start(); world.directory.remove(target); world.recoverWithFreshRegistry(); world.advance(Duration.ofSeconds(121)); assertEquals(1,world.service.tick().join());
        assertEquals(ScreenshareStatus.VIOLATION,world.service.find(session.getId()).join().orElseThrow().getStatus()); assertEquals(ScreenshareViolationType.TARGET_MISSING_AFTER_RECOVERY,world.violations.last().getType());
    }
    @Test
    void loShutdownNonGeneraViolazioni() {
        ScreenshareSession session=start(); world.service.beginShutdown(); world.directory.remove(target); world.service.onTargetDisconnect(target.uniqueId()).join(); world.service.onStaffDisconnect(staff.uniqueId()).join();
        assertEquals(ScreenshareStatus.ACTIVE,world.service.find(session.getId()).join().orElseThrow().getStatus()); assertEquals(0,world.violations.count());
    }
    @Test
    void ilRipristinoRicostruisceIVincoliDelleSessioniValide() {
        ScreenshareSession session=start(); assertEquals(0,world.recoverWithFreshRegistry()); assertTrue(world.registry.isLocked(target.uniqueId())); assertEquals(ScreenshareStatus.ACTIVE,world.service.find(session.getId()).join().orElseThrow().getStatus());
    }
}
