package it.legacynetwork.screenshare.violation;

import it.legacynetwork.screenshare.model.ScreenshareSessionId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La porta delle violazioni registra e basta.
 *
 * <p>LegacyPunishments non esiste ancora: nessun comando di ban viene eseguito
 * e nessun finto plugin di punizioni viene simulato.</p>
 */
class ViolationHandlingTest {

    private ScreenshareViolation violation() {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("server", "screenshare-1");
        context.put("status", "ACTIVE");
        return new ScreenshareViolation(UUID.randomUUID(), "Target",
                UUID.randomUUID(), "Staff", ScreenshareSessionId.random(),
                UUID.randomUUID(),
                ScreenshareViolationType.TARGET_DISCONNECTED,
                Instant.parse("2026-01-01T12:00:00Z"), context,
                SuggestedPunishmentCategory.SCREENSHARE_EVASION);
    }

    @Test
    void laRichiestaPortaTuttoIlContestoOsservato() {
        ScreenshareViolation request = violation();

        assertEquals("Target", request.getTargetName());
        assertTrue(request.getStaffId().isPresent());
        assertTrue(request.getReportId().isPresent());
        assertEquals(SuggestedPunishmentCategory.SCREENSHARE_EVASION,
                request.getSuggestedCategory());
        assertEquals("screenshare-1", request.getContext().get("server"));
        assertEquals(Instant.parse("2026-01-01T12:00:00Z"),
                request.getOccurredAt());
    }

    @Test
    void ilContestoEuImmutabile() {
        ScreenshareViolation request = violation();

        assertThrows(UnsupportedOperationException.class,
                () -> request.getContext().put("x", "y"));
    }

    @Test
    void ilGestorePredefinitoScriveUnaSolaRigaTecnica() {
        List<String> written = new ArrayList<>();
        ScreenshareViolationHandler handler =
                new AuditOnlyScreenshareViolationHandler(written::add);

        handler.handle(violation());

        assertEquals(1, written.size());
        String line = written.get(0);
        assertTrue(line.contains("screenshare-violation"));
        assertTrue(line.contains("TARGET_DISCONNECTED"));
        assertTrue(line.contains("suggested=SCREENSHARE_EVASION"));
        assertTrue(line.contains("server=screenshare-1"));
    }

    @Test
    void ilGestorePredefinitoNonNominaAlcunaPunizione() {
        List<String> written = new ArrayList<>();
        new AuditOnlyScreenshareViolationHandler(written::add)
                .handle(violation());

        String line = written.get(0).toLowerCase(java.util.Locale.ROOT);
        assertFalse(line.contains("/ban"));
        assertFalse(line.contains("tempban"));
        assertFalse(line.contains("kick"));
    }

    @Test
    void ogniTipoDiViolazioneHaUnaChiaveDiTraduzione() {
        for (ScreenshareViolationType type
                : ScreenshareViolationType.values()) {
            assertTrue(type.messageKey().startsWith("screenshare.violation."));
        }
    }

    @Test
    void ilCodiceNonEseguiraMaiUnComandoDiPunizione() throws IOException {
        List<String> offenders = SourceScan.punishmentCalls();

        assertTrue(offenders.isEmpty(),
                "il plugin non deve eseguire comandi: " + offenders);
    }
}
