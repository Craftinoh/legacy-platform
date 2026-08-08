package it.legacynetwork.screenshare.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.screenshare.model.ScreenshareEventType;
import it.legacynetwork.screenshare.model.ScreenshareOutcome;
import it.legacynetwork.screenshare.model.ScreenshareStatus;
import it.legacynetwork.screenshare.service.ScreenshareOperationStatus;
import it.legacynetwork.screenshare.violation.ScreenshareViolationType;
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
 * lingue incluse, e la catena di fallback deve reggere per le altre.
 */
class ScreenshareLocalizationTest {

    private static final String BASE =
            "src/main/resources/screenshare/translations/messages_";

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
    void ogniStatoEsitoEdEventoHaUnTesto() throws IOException {
        Properties english = bundle("en");
        List<String> missing = new ArrayList<>();
        for (ScreenshareStatus status : ScreenshareStatus.values()) {
            if (english.getProperty(status.messageKey()) == null) {
                missing.add(status.messageKey());
            }
        }
        for (ScreenshareOutcome outcome : ScreenshareOutcome.values()) {
            if (english.getProperty(outcome.messageKey()) == null) {
                missing.add(outcome.messageKey());
            }
        }
        for (ScreenshareEventType type : ScreenshareEventType.values()) {
            if (english.getProperty(type.messageKey()) == null) {
                missing.add(type.messageKey());
            }
        }
        for (ScreenshareViolationType type
                : ScreenshareViolationType.values()) {
            if (english.getProperty(type.messageKey()) == null) {
                missing.add(type.messageKey());
            }
        }
        for (ScreenshareOperationStatus status
                : ScreenshareOperationStatus.values()) {
            if (english.getProperty(status.messageKey()) == null) {
                missing.add(status.messageKey());
            }
        }
        assertTrue(missing.isEmpty(), "chiavi mancanti: " + missing);
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
        ScreenshareMessages messages =
                ScreenshareMessages.load(Language.ITALIAN);

        String italian = messages.get(Language.ITALIAN,
                "screenshare.error.self-target");
        String english = messages.get(Language.ENGLISH,
                "screenshare.error.self-target");
        String spanish = messages.get(Language.SPANISH,
                "screenshare.error.self-target");

        assertNotEquals(italian, english);
        assertNotEquals(english, spanish);
        assertNotEquals(italian, spanish);
        for (String text : new String[]{italian, english, spanish}) {
            assertFalse(text.startsWith("missing:"), text);
        }
    }

    @Test
    void unaLinguaSenzaBundleUsaIlFallbackConfigurato() {
        ScreenshareMessages messages =
                ScreenshareMessages.load(Language.ITALIAN);

        assertEquals(messages.get(Language.ITALIAN,
                        "screenshare.error.self-target"),
                messages.get(Language.TURKISH,
                        "screenshare.error.self-target"));
        assertTrue(messages.has(Language.TURKISH,
                "screenshare.error.self-target"));
    }

    @Test
    void unaChiaveInesistenteRestaRiconoscibile() {
        ScreenshareMessages messages =
                ScreenshareMessages.load(Language.ENGLISH);

        assertFalse(messages.has(Language.ENGLISH,
                "screenshare.error.inventata"));
    }

    @Test
    void iSegnapostoVengonoSostituiti() {
        ScreenshareMessages messages =
                ScreenshareMessages.load(Language.ENGLISH);

        String text = messages.get(Language.ENGLISH,
                "screenshare.error.note-too-long",
                PlaceholderValues.builder().put("limit", 42).build());

        assertTrue(text.contains("42"), text);
        assertFalse(text.contains("{limit}"), text);
    }

    @Test
    void iMessaggiDelBersaglioSonoDistintiDaQuelliDelloStaff()
            throws IOException {
        Properties english = bundle("en");

        assertNotEquals(english.getProperty("screenshare.target.session-ended"),
                english.getProperty(
                        "screenshare.staff.session-completed"));
        assertTrue(english.getProperty("screenshare.target.instructions")
                .length() > 0);
    }

    @Test
    void senzaLinguaSiUsaIlFallback() {
        ScreenshareMessages messages =
                ScreenshareMessages.load(Language.SPANISH);

        assertEquals(messages.get(Language.SPANISH,
                        "screenshare.status.active"),
                messages.get(null, "screenshare.status.active"));
    }
}
