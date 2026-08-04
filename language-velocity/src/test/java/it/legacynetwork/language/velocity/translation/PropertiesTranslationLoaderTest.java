package it.legacynetwork.language.velocity.translation;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.TranslationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesTranslationLoaderTest {
    @Test
    void loadsUtf8Properties() throws Exception {
        TranslationService translations = new PropertiesTranslationLoader(
                getClass().getClassLoader()).load();

        assertTrue(translations.translate(Language.ITALIAN, "error.internal")
                .contains("è"));
    }
}
