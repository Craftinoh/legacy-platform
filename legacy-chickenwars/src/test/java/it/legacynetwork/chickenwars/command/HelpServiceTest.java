package it.legacynetwork.chickenwars.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Copre la risoluzione delle sezioni di guida.
 *
 * <p>La regressione principale e' l'argomento assente: quando non ripiegava
 * esplicitamente sull'indice, la guida entrava in ricorsione infinita e il
 * comando terminava con {@code StackOverflowError}.</p>
 */
class HelpServiceTest {

    @Test
    void senzaArgomentoMostraLIndice() {
        assertSame(HelpTopic.GENERAL, HelpService.resolve(null, false));
        assertSame(HelpTopic.GENERAL, HelpService.resolve("", false));
        assertSame(HelpTopic.GENERAL, HelpService.resolve("   ", false));
        assertSame(HelpTopic.GENERAL, HelpService.resolve(null, true));
    }

    @Test
    void unaSezioneSconosciutaNonVieneRisolta() {
        assertNull(HelpService.resolve("inesistente", true));
        assertNull(HelpService.resolve("qwerty", false));
    }

    @Test
    void leSezioniLibereSonoAccessibiliATutti() {
        assertSame(HelpTopic.GAME, HelpService.resolve("gioco", false));
        assertSame(HelpTopic.CHICKEN, HelpService.resolve("gallina", false));
        assertSame(HelpTopic.CHICKEN, HelpService.resolve("chicken", false));
    }

    @Test
    void leSezioniRiservateRichiedonoIPermessi() {
        assertNull(HelpService.resolve("admin", false));
        assertNull(HelpService.resolve("setup", false));
        assertNull(HelpService.resolve("mondi", false));

        assertSame(HelpTopic.ADMIN, HelpService.resolve("admin", true));
        assertSame(HelpTopic.SETUP, HelpService.resolve("setup", true));
        assertSame(HelpTopic.WORLDS, HelpService.resolve("mondi", true));
    }

    @Test
    void laRisoluzioneIgnoraMaiuscoleESpazi() {
        assertSame(HelpTopic.GAME, HelpService.resolve("  GIOCO  ", false));
        assertSame(HelpTopic.WORLDS, HelpService.resolve("Worlds", true));
    }

    @Test
    void soloLeSezioniStaffRichiedonoUnPermesso() {
        assertNull(HelpTopic.GENERAL.getPermission());
        assertNull(HelpTopic.GAME.getPermission());
        assertNull(HelpTopic.CHICKEN.getPermission());

        assertSame(HelpTopic.ADMIN_PERMISSION, HelpTopic.ADMIN.getPermission());
        assertSame(HelpTopic.ADMIN_PERMISSION, HelpTopic.SETUP.getPermission());
        assertSame(HelpTopic.ADMIN_PERMISSION, HelpTopic.WORLDS.getPermission());
    }
}
