package it.legacynetwork.screenshare.config;

import it.legacynetwork.language.Language;
import it.legacynetwork.screenshare.support.ScreenshareWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Il file spedito deve essere leggibile, e i valori non implementati vanno
 * rifiutati invece di essere accettati e ignorati.
 */
class ScreenshareConfigurationTest {

    @Test
    void iValoriPredefinitiSonoQuelliDelFile() {
        ScreenshareConfiguration configuration = ScreenshareWorld.configuration();

        assertEquals(Language.ITALIAN, configuration.getFallbackLanguage());
        assertEquals("screenshare-1", configuration.getServer());
        assertTrue(configuration.hasServer());
        assertEquals(2, configuration.getFallbackServers().size());
        assertEquals(15L, configuration.getTransferTimeout().getSeconds());
        assertEquals(60L, configuration.getMaximumSession().toMinutes());
        assertEquals(60L, configuration.getStaffReconnectGrace().getSeconds());
        assertEquals(120L, configuration.getRestartRecoveryGrace().getSeconds());
        assertEquals(StaffDisconnectPolicy.CANCEL,
                configuration.getStaffDisconnectPolicy());
        assertFalse(configuration.isLockStaffServer());
        assertFalse(configuration.isAllowMultipleSessionsPerStaff());
        assertEquals(8, configuration.getPageSize());
        assertEquals(200, configuration.getNoteMaxLength());
        assertEquals("legacyscreenshare.staff.start",
                configuration.getPermissions().getStart());
    }

    @Test
    void iComandiConsentitiArrivanoDalFile() {
        ScreenshareConfiguration configuration = ScreenshareWorld.configuration();

        assertTrue(configuration.getAllowedTargetCommands().contains("msg"));
        assertTrue(configuration.getAllowedTargetCommands().contains("helpop"));
        assertFalse(configuration.getAllowedTargetCommands().contains("hub"));
    }

    @Test
    void laPoliticaDiAttesaEuAccettata() {
        ScreenshareConfiguration configuration = ScreenshareWorld.configuration(
                section -> section.put("staff-disconnect-policy",
                        "KEEP_ACTIVE_FOR_SECONDS"));

        assertEquals(StaffDisconnectPolicy.KEEP_ACTIVE_FOR_SECONDS,
                configuration.getStaffDisconnectPolicy());
    }


    @Test
    void unaPoliticaSconosciutaVieneRifiutata() {
        assertThrows(ScreenshareConfigurationException.class,
                () -> ScreenshareWorld.configuration(section ->
                        section.put("staff-disconnect-policy", "INVENTATA")));
    }

    @Test
    void sonoDichiarateSoloLePoliticheSupportate() {
        assertEquals(java.util.List.of("CANCEL", "KEEP_ACTIVE_FOR_SECONDS"), StaffDisconnectPolicy.supportedNames());
        assertEquals(2, StaffDisconnectPolicy.values().length);
    }

    @Test
    void unaConfigurazioneVuotaUsaValoriSensati() {
        ScreenshareConfiguration configuration =
                ScreenshareConfiguration.fromRoot(ConfigSection.empty());

        assertEquals(Language.ENGLISH, configuration.getFallbackLanguage());
        assertFalse(configuration.hasServer());
        assertEquals(StaffDisconnectPolicy.CANCEL,
                configuration.getStaffDisconnectPolicy());
        assertTrue(configuration.getAllowedTargetCommands().isEmpty());
    }

    @Test
    void laSezioneTolleraValoriScrittiMale() {
        ConfigSection section = ConfigSection.of(
                java.util.Map.of("numero", "non-un-numero", "flag", "no"));

        assertEquals(3, section.number("numero", 3));
        assertFalse(section.flag("flag", true));
        assertEquals("assente", section.text("mancante", "assente"));
        assertTrue(section.section("mancante").isEmpty());
        assertTrue(section.list("mancante").isEmpty());
    }
}
