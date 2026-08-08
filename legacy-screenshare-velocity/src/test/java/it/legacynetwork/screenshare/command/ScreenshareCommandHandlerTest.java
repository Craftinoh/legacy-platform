package it.legacynetwork.screenshare.command;

import it.legacynetwork.language.Language;
import it.legacynetwork.screenshare.model.ScreenshareStatus;
import it.legacynetwork.screenshare.support.FakeConsole;
import it.legacynetwork.screenshare.support.FakePlayer;
import it.legacynetwork.screenshare.support.ScreenshareWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comandi staff: permessi, argomenti e delega al servizio.
 */
class ScreenshareCommandHandlerTest {

    private ScreenshareWorld world;
    private ScreenshareCommandHandler handler;
    private FakePlayer staff;
    private FakePlayer target;

    @BeforeEach
    void setUp() {
        world = new ScreenshareWorld();
        staff = new FakePlayer("Staff").allow(
                "legacyscreenshare.staff.view",
                "legacyscreenshare.staff.start",
                "legacyscreenshare.staff.stop",
                "legacyscreenshare.staff.note");
        target = new FakePlayer("Target");
        world.directory.add(staff, target);
        handler = new ScreenshareCommandHandler(world.configuration,
                world.service, world.directory, world.presenter,
                world.languages);
    }

    private String expected(String key) {
        return world.messages().get(Language.ITALIAN, key);
    }

    @Test
    void senzaPermessoNonSiVedeNulla() {
        FakePlayer stranger = new FakePlayer("Stranger");

        handler.execute(stranger, new String[]{"list"}).join();

        assertEquals(expected("screenshare.error.no-permission"),
                stranger.text());
        assertTrue(handler.suggest(stranger, new String[]{""}).isEmpty());
    }

    @Test
    void senzaArgomentiSiVedeLElencoDeiComandi() {
        handler.execute(staff, new String[0]).join();

        assertEquals(7, staff.received().size());
        assertTrue(staff.text().contains(
                expected("screenshare.command.header")));
    }

    @Test
    void laConsoleNonPuoAvviareUnControllo() {
        FakeConsole console = new FakeConsole();

        handler.execute(console, new String[]{"start", "Target"}).join();

        assertTrue(console.text().contains(world.messages().get(
                world.messages().getFallback(),
                "screenshare.error.player-only")));
        assertTrue(world.sessions.findOpen().join().isEmpty());
    }

    @Test
    void lAvvioPassaDalServizio() {
        handler.execute(staff, new String[]{"start", "Target"}).join();

        assertEquals(1, world.sessions.findOpen().join().size());
        assertEquals(ScreenshareStatus.ACTIVE, world.sessions
                .findOpenByTarget(target.uniqueId()).join().orElseThrow()
                .getStatus());
    }

    @Test
    void unEsitoSconosciutoVieneRifiutato() {
        handler.execute(staff, new String[]{"start", "Target"}).join();
        staff.clear();

        handler.execute(staff, new String[]{"stop", "Target", "boh"}).join();

        assertTrue(staff.text().contains("CLEAN"));
        assertEquals(ScreenshareStatus.ACTIVE, world.sessions
                .findOpenByTarget(target.uniqueId()).join().orElseThrow()
                .getStatus());
    }

    @Test
    void laChiusuraPassaDalServizio() {
        handler.execute(staff, new String[]{"start", "Target"}).join();

        handler.execute(staff, new String[]{"stop", "Target", "CLEAN"}).join();

        assertTrue(world.sessions.findOpen().join().isEmpty());
    }

    @Test
    void lAnnullamentoRichiedeUnMotivo() {
        handler.execute(staff, new String[]{"start", "Target"}).join();
        staff.clear();

        handler.execute(staff, new String[]{"cancel", "Target"}).join();

        assertEquals(7, staff.received().size(), "viene mostrato l'uso");
        assertEquals(1, world.sessions.findOpen().join().size());
    }

    @Test
    void laNotaRichiedeUnTesto() {
        handler.execute(staff, new String[]{"start", "Target"}).join();
        staff.clear();

        handler.execute(staff, new String[]{"note", "Target"}).join();

        assertEquals(7, staff.received().size());
    }

    @Test
    void unaNotaTroppoLungaVieneRifiutata() {
        handler.execute(staff, new String[]{"start", "Target"}).join();
        staff.clear();
        StringBuilder tooLong = new StringBuilder();
        for (int index = 0; index < 60; index++) {
            tooLong.append("nota");
        }

        handler.execute(staff,
                new String[]{"note", "Target", tooLong.toString()}).join();

        assertTrue(staff.text().contains("200"));
    }

    @Test
    void loStatoSenzaSessioneLoDice() {
        handler.execute(staff, new String[]{"status", "Target"}).join();

        assertTrue(staff.text().contains("Target"));
        assertTrue(staff.text().startsWith("&c"));
    }

    @Test
    void loStatoMostraLaSchedaELoStorico() {
        handler.execute(staff, new String[]{"start", "Target"}).join();
        staff.clear();

        handler.execute(staff, new String[]{"status", "Target"}).join();

        assertTrue(staff.text().contains(
                expected("screenshare.info.history-header")));
        assertTrue(staff.text().contains("Target"));
    }

    @Test
    void lElencoVuotoLoDice() {
        handler.execute(staff, new String[]{"list"}).join();

        assertTrue(staff.text().contains(
                expected("screenshare.list.empty")));
    }

    @Test
    void unaPaginaNonValidaVieneRifiutata() {
        handler.execute(staff, new String[]{"list", "zero"}).join();

        assertEquals(expected("screenshare.error.invalid-page"), staff.text());
    }

    @Test
    void iSuggerimentiPropongonoSottocomandiNomiEdEsiti() {
        assertTrue(handler.suggest(staff, new String[]{"st"})
                .contains("start"));
        assertTrue(handler.suggest(staff, new String[]{"start", "Tar"})
                .contains("Target"));
        assertTrue(handler.suggest(staff,
                new String[]{"stop", "Target", "CL"}).contains("CLEAN"));
        assertFalse(handler.suggest(staff,
                new String[]{"stop", "Target", "CL"}).contains("CANCELLED"),
                "gli esiti automatici non si scelgono a mano");
    }

    @Test
    void ogniMessaggioArrivaNellaLinguaDelDestinatario() {
        ScreenshareWorld english = new ScreenshareWorld(
                ScreenshareWorld.configuration(), Language.ENGLISH, true);
        english.directory.add(staff, target);
        ScreenshareCommandHandler handlerEn = new ScreenshareCommandHandler(
                english.configuration, english.service, english.directory,
                english.presenter, english.languages);
        staff.clear();

        handlerEn.execute(staff, new String[]{"start", "Staff"}).join();

        assertEquals(english.messages().get(Language.ENGLISH,
                "screenshare.error.self-target"), staff.text());
    }
}
