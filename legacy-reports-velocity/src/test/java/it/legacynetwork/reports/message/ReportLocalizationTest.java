package it.legacynetwork.reports.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.reports.api.ReportOperationStatus;
import it.legacynetwork.reports.api.ReportEventType;
import it.legacynetwork.reports.api.ReportStatus;
import it.legacynetwork.reports.support.ReportsTestSupport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * I testi spediti devono coprire ogni chiave usata dal codice, in tutte le
 * lingue incluse, e la catena di fallback deve funzionare per le altre.
 */
class ReportLocalizationTest {

    private static final String BASE =
            "src/main/resources/reports/translations/messages_";

    private static final String[] SHIPPED = {"it", "en", "es"};

    private Properties bundle(String code) throws IOException {
        File file = new File(BASE + code + ".properties");
        assertTrue(file.isFile(), "bundle mancante: " + file.getPath());
        Properties properties = new Properties();
        try (InputStream stream = new FileInputStream(file)) {
            properties.load(new InputStreamReader(stream,
                    StandardCharsets.UTF_8));
        }
        return properties;
    }

    @Test
    void iBundleSpeditiEspongonoLeStesseChiavi() throws IOException {
        TreeSet<String> reference =
                new TreeSet<>(bundle("en").stringPropertyNames());
        for (String code : SHIPPED) {
            assertEquals(reference,
                    new TreeSet<>(bundle(code).stringPropertyNames()),
                    "chiavi diverse nel bundle " + code);
        }
    }

    @Test
    void nessunValoreEuVuoto() throws IOException {
        for (String code : SHIPPED) {
            Properties properties = bundle(code);
            for (String key : properties.stringPropertyNames()) {
                assertFalse(properties.getProperty(key).trim().isEmpty(),
                        "valore vuoto in " + code + ": " + key);
            }
        }
    }

    @Test
    void ogniStatoHaUnNomeTradotto() throws IOException {
        Properties english = bundle("en");
        for (ReportStatus status : ReportStatus.values()) {
            assertTrue(english.getProperty(status.messageKey()) != null,
                    "manca " + status.messageKey());
        }
    }

    @Test
    void ogniTipoDiEventoHaUnaDescrizione() throws IOException {
        Properties english = bundle("en");
        for (ReportEventType type : ReportEventType.values()) {
            assertTrue(english.getProperty(type.messageKey()) != null,
                    "manca " + type.messageKey());
        }
    }

    @Test
    void ogniEsitoDelServizioHaUnMessaggio() throws IOException {
        Properties english = bundle("en");
        for (ReportOperationStatus status : ReportOperationStatus.values()) {
            assertTrue(english.getProperty(status.messageKey()) != null,
                    "manca " + status.messageKey());
        }
    }

    @Test
    void ogniMotivoConfiguratoHaLaSuaChiave() throws IOException {
        Properties english = bundle("en");
        List<String> missing = new ArrayList<>();
        for (it.legacynetwork.reports.model.ReportReason reason
                : ReportsTestSupport.configuration().getReasons().all()) {
            if (english.getProperty(reason.getDisplayKey()) == null) {
                missing.add(reason.getDisplayKey());
            }
        }
        assertTrue(missing.isEmpty(), "chiavi motivo mancanti: " + missing);
    }

    @Test
    void ogniChiaveCitataNelCodiceEsisteNeiBundle() throws IOException {
        Properties english = bundle("en");
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> reference
                : SourceKeys.literalKeys().entrySet()) {
            if (english.getProperty(reference.getKey()) == null) {
                missing.add(reference.getKey() + " (" + reference.getValue()
                        + ")");
            }
        }
        assertTrue(missing.isEmpty(),
                "chiavi usate nel codice ma assenti dai bundle: " + missing);
    }

    @Test
    void leTreLingueSpediteRisolvonoLeProprieChiavi() {
        ReportMessages messages = ReportMessages.load(Language.ITALIAN);

        String italian = messages.get(Language.ITALIAN,
                "reports.error.self-report");
        String english = messages.get(Language.ENGLISH,
                "reports.error.self-report");
        String spanish = messages.get(Language.SPANISH,
                "reports.error.self-report");

        assertNotEquals(italian, english);
        assertNotEquals(english, spanish);
        assertNotEquals(italian, spanish);
        for (String text : new String[]{italian, english, spanish}) {
            assertFalse(text.startsWith("missing:"), text);
        }
    }

    @Test
    void unaLinguaSenzaBundleUsaIlFallbackConfigurato() {
        ReportMessages messages = ReportMessages.load(Language.ITALIAN);

        String russian = messages.get(Language.RUSSIAN,
                "reports.error.self-report");

        assertEquals(messages.get(Language.ITALIAN,
                "reports.error.self-report"), russian);
        assertTrue(messages.has(Language.RUSSIAN, "reports.error.self-report"));
    }

    @Test
    void unaChiaveInesistenteRestaRiconoscibile() {
        ReportMessages messages = ReportMessages.load(Language.ENGLISH);

        assertFalse(messages.has(Language.ENGLISH, "reports.error.inventata"));
        assertEquals("missing:reports.error.inventata",
                messages.get(Language.ENGLISH, "reports.error.inventata"));
    }

    @Test
    void iSegnapostoVengonoSostituiti() {
        ReportMessages messages = ReportMessages.load(Language.ENGLISH);

        String text = messages.get(Language.ENGLISH, "reports.error.cooldown",
                PlaceholderValues.builder().put("seconds", 12).build());

        assertTrue(text.contains("12"), text);
        assertFalse(text.contains("{seconds}"), text);
    }

    @Test
    void senzaLinguaSiUsaIlFallback() {
        ReportMessages messages = ReportMessages.load(Language.SPANISH);

        assertEquals(messages.get(Language.SPANISH, "reports.status.open"),
                messages.get(null, "reports.status.open"));
    }
}
