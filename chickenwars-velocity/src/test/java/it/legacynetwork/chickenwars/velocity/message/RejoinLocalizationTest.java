package it.legacynetwork.chickenwars.velocity.message;

import it.legacynetwork.chickenwars.velocity.rejoin.RejoinOutcome;
import it.legacynetwork.language.Language;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ogni testo mostrato dal comando deve esistere nei bundle spediti.
 *
 * <p>Le chiavi non sono elencate a mano: vengono lette da
 * {@link RejoinOutcome}, quindi il test non puo' restare verde su una lista
 * inventata.</p>
 */
class RejoinLocalizationTest {

    private static final String BASE =
            "src/main/resources/chickenwars/translations/messages_";

    private Properties load(String code) throws IOException {
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
    void ogniEsitoHaUnTestoInEntrambeLeLingue() throws IOException {
        Properties italian = load("it");
        Properties english = load("en");

        List<String> missing = new ArrayList<String>();
        for (RejoinOutcome outcome : RejoinOutcome.values()) {
            String key = outcome.getMessageKey();
            if (italian.getProperty(key) == null) {
                missing.add("it:" + key);
            }
            if (english.getProperty(key) == null) {
                missing.add("en:" + key);
            }
        }
        assertTrue(missing.isEmpty(), "traduzioni mancanti: " + missing);
    }

    @Test
    void leDueLingueEspongonoLeStesseChiavi() throws IOException {
        assertEquals(new TreeSet<String>(load("it").stringPropertyNames()),
                new TreeSet<String>(load("en").stringPropertyNames()));
    }

    @Test
    void nessunTestoEuVuoto() throws IOException {
        for (String code : new String[]{"it", "en"}) {
            Properties bundle = load(code);
            for (String key : bundle.stringPropertyNames()) {
                assertFalse(bundle.getProperty(key).trim().isEmpty(),
                        "valore vuoto in " + code + ": " + key);
            }
        }
    }

    @Test
    void leChiaviObbligatorieSonoPresenti() throws IOException {
        Properties english = load("en");
        for (String key : new String[]{"rejoin.searching",
                "rejoin.already-in-progress", "rejoin.no-session",
                "rejoin.expired", "rejoin.match-ended", "rejoin.eliminated",
                "rejoin.instance-offline", "rejoin.instance-unavailable",
                "rejoin.reservation-failed", "rejoin.transfer-started",
                "rejoin.transfer-failed", "rejoin.backend-rejected",
                "rejoin.no-permission", "rejoin.player-only"}) {
            assertTrue(english.getProperty(key) != null,
                    "chiave richiesta assente: " + key);
        }
    }

    @Test
    void ilServizioRisolveLeChiaviDaiBundleInclusi() {
        RejoinMessages messages = RejoinMessages.load(Language.ENGLISH);

        String text = messages.get(Language.ENGLISH, "rejoin.transfer-started");

        assertFalse(text.trim().isEmpty());
        assertFalse(text.contains("rejoin.transfer-started"),
                "la chiave grezza non deve essere mostrata");
    }

    @Test
    void unaLinguaSenzaBundleRicadeSulFallback() {
        RejoinMessages messages = RejoinMessages.load(Language.ITALIAN);

        assertEquals(Language.ITALIAN, messages.getFallback());
        assertFalse(messages.get(null, "rejoin.no-session").trim().isEmpty());
    }
}
