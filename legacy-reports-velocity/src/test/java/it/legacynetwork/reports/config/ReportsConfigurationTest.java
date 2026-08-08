package it.legacynetwork.reports.config;

import it.legacynetwork.language.Language;
import it.legacynetwork.reports.model.ReportReason;
import it.legacynetwork.reports.support.ReportsTestSupport;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Il file di configurazione spedito deve essere leggibile e completo.
 */
class ReportsConfigurationTest {

    @Test
    void ilFileSpeditoDefinisceIMotivi() {
        ReportsConfiguration configuration = ReportsTestSupport.configuration();

        assertFalse(configuration.getReasons().isEmpty());
        assertTrue(configuration.getReasons().find("cheating").isPresent());
        assertTrue(configuration.getReasons().find("hack").isPresent(),
                "gli alias devono funzionare");
        assertFalse(configuration.getReasons().find("inventato").isPresent());
    }

    @Test
    void unMotivoDisattivatoNonEsistePerIlComando() {
        ReportsConfiguration configuration = ReportsTestSupport.configuration(
                reports -> {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> reasons =
                            (java.util.Map<String, Object>)
                                    reports.get("reasons");
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> cheating =
                            (java.util.Map<String, Object>)
                                    reasons.get("cheating");
                    cheating.put("enabled", false);
                });

        assertFalse(configuration.getReasons().find("cheating").isPresent());
        assertTrue(configuration.getReasons().findById("cheating").isPresent(),
                "resta visibile per i report storici");
    }

    @Test
    void iValoriPredefinitiSonoQuelliDelFile() {
        ReportsConfiguration configuration = ReportsTestSupport.configuration();

        assertEquals(Language.ITALIAN, configuration.getFallbackLanguage());
        assertEquals(8, configuration.getPageSize());
        assertEquals(60L, configuration.getCooldown().getSeconds());
        assertEquals(30L, configuration.getDuplicateWindow().toMinutes());
        assertEquals(3, configuration.getMaxOpenPerReporter());
        assertEquals(200, configuration.getDetailsMaxLength());
        assertTrue(configuration.isProtectStaff());
        assertEquals("legacyreports.command.report",
                configuration.getPermissions().getReport());
        assertEquals("legacyreports.admin",
                configuration.getPermissions().getAdmin());
    }

    @Test
    void unaConfigurazioneVuotaUsaValoriSensati() {
        ReportsConfiguration configuration =
                ReportsConfiguration.fromRoot(ConfigSection.empty());

        assertEquals(Language.ENGLISH, configuration.getFallbackLanguage());
        assertTrue(configuration.getReasons().isEmpty());
        assertEquals(8, configuration.getPageSize());
        assertEquals("legacyreports.staff.view",
                configuration.getPermissions().getStaffView());
    }

    @Test
    void unMotivoSenzaChiaveNeRicavaUnaPredefinita() {
        ConfigSection reasons = ConfigSection.of(Collections.singletonMap(
                "teaming", Collections.emptyMap()));

        Optional<ReportReason> reason =
                ReportReasonCatalog.fromSection(reasons).find("teaming");

        assertTrue(reason.isPresent());
        assertEquals("reports.reason.teaming", reason.get().getDisplayKey());
    }

    @Test
    void laSezioneTolleraValoriScrittiMale() {
        ConfigSection section = ConfigSection.of(
                java.util.Map.of("numero", "non-un-numero", "flag", "yes"));

        assertEquals(7, section.number("numero", 7));
        assertTrue(section.flag("flag", false));
        assertEquals("assente", section.text("mancante", "assente"));
        assertTrue(section.section("mancante").isEmpty());
    }
}
