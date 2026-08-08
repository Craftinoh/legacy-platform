package it.legacynetwork.reports.message;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nessun testo visibile deve nascere in Java.
 *
 * <p>Il controllo e' strutturale: se un giorno qualcuno scrive una frase dentro
 * {@code ChatLine.text(...)} invece di passare da NetworkLanguage, questo test
 * lo dice per nome e riga.</p>
 */
class NoHardcodedTextTest {

    @Test
    void nessunaRigaDiChatNasceDaTestoScrittoInJava() throws IOException {
        List<String> offenders = SourceKeys.hardcodedChatText();

        assertTrue(offenders.isEmpty(),
                "testo visibile scritto in Java: " + offenders);
    }

    @Test
    void ilControlloTrovaDavveroLeChiavi() throws IOException {
        assertFalse(SourceKeys.literalKeys().isEmpty(),
                "la scansione dei sorgenti deve trovare le chiavi usate");
    }
}
