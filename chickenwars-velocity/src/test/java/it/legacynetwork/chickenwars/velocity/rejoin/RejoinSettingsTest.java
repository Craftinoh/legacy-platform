package it.legacynetwork.chickenwars.velocity.rejoin;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lettura della sezione {@code chickenwars-rejoin}.
 */
class RejoinSettingsTest {

    private Map<String, Object> section() {
        return new LinkedHashMap<String, Object>();
    }

    @Test
    void laSezioneAssenteProduceValoriUsabili() {
        RejoinSettings settings = RejoinSettings.fromMap(null);

        assertTrue(settings.isEnabled());
        assertEquals(RejoinSettings.DEFAULT_PERMISSION, settings.getPermission());
        assertTrue(settings.getLookupTimeoutMillis() > 0L);
        assertTrue(settings.getTransferTimeoutMillis() > 0L);
        assertTrue(settings.getHeartbeatTimeoutMillis() > 0L);
    }

    @Test
    void iValoriConfiguratiVengonoLetti() {
        Map<String, Object> raw = section();
        raw.put("enabled", Boolean.FALSE);
        raw.put("permission", "custom.rejoin");
        raw.put("lookup-timeout-millis", Integer.valueOf(1500));
        raw.put("transfer-timeout-millis", Integer.valueOf(2500));

        RejoinSettings settings = RejoinSettings.fromMap(raw);

        assertFalse(settings.isEnabled());
        assertEquals("custom.rejoin", settings.getPermission());
        assertEquals(1500L, settings.getLookupTimeoutMillis());
        assertEquals(2500L, settings.getTransferTimeoutMillis());
    }

    @Test
    void iValoriNonPositiviRicadonoSuiPredefiniti() {
        Map<String, Object> raw = section();
        raw.put("lookup-timeout-millis", Integer.valueOf(0));
        raw.put("transfer-timeout-millis", Integer.valueOf(-5));

        RejoinSettings settings = RejoinSettings.fromMap(raw);

        assertEquals(3000L, settings.getLookupTimeoutMillis());
        assertEquals(5000L, settings.getTransferTimeoutMillis());
    }

    @Test
    void unPermessoVuotoRicadeSulPredefinito() {
        Map<String, Object> raw = section();
        raw.put("permission", "   ");

        assertEquals(RejoinSettings.DEFAULT_PERMISSION,
                RejoinSettings.fromMap(raw).getPermission());
    }

    @Test
    void iValoriTestualiVengonoInterpretati() {
        Map<String, Object> raw = section();
        raw.put("enabled", "false");
        raw.put("reservation-ttl-millis", "45000");

        RejoinSettings settings = RejoinSettings.fromMap(raw);

        assertFalse(settings.isEnabled());
        assertEquals(45000L, settings.getReservationTtlMillis());
    }

    @Test
    void unValoreMalformatoNonRompeLaLettura() {
        Map<String, Object> raw = section();
        raw.put("reconnect-ttl-millis", "non-un-numero");

        assertEquals(120000L, RejoinSettings.fromMap(raw).getReconnectTtlMillis());
    }
}
