package it.legacynetwork.language;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationServiceTest {
    @Test
    void replacesPlaceholdersAndReportsMissingKeys() {
        Map<String, String> values = Collections.singletonMap(
                "welcome", "Welcome {player} to {server}");
        Map<Language, TranslationBundle> bundles =
                new EnumMap<Language, TranslationBundle>(Language.class);
        bundles.put(Language.ENGLISH, new TranslationBundle(values));
        TranslationService service = new TranslationService(bundles);

        assertEquals("Welcome Alex to lobby-01", service.translate(
                Language.ENGLISH,
                "welcome",
                PlaceholderValues.builder().player("Alex").server("lobby-01").build()));
        assertEquals("missing:translation.key",
                service.translate(Language.ENGLISH, "translation.key"));
    }
}
