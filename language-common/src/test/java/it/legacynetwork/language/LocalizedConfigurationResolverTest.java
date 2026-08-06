package it.legacynetwork.language;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizedConfigurationResolverTest {

    private Map<String, Object> buildTranslations() {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> en = new LinkedHashMap<>();
        en.put("name", "&aHello");
        en.put("lore", Arrays.asList("&7Line 1", "&7Line 2"));
        Map<String, Object> it = new LinkedHashMap<>();
        it.put("name", "&aCiao");
        it.put("lore", Arrays.asList("&7Riga 1", "&7Riga 2"));
        root.put("en", en);
        root.put("it", it);
        return root;
    }

    @Test
    void resolveStringExactLanguage() {
        Map<String, Object> t = buildTranslations();
        assertEquals("&aCiao", LocalizedConfigurationResolver.resolveString(t, "it", "name"));
    }

    @Test
    void resolveStringFallbackEnglish() {
        Map<String, Object> t = buildTranslations();
        assertEquals("&aHello", LocalizedConfigurationResolver.resolveString(t, "es", "name"));
    }

    @Test
    void resolveStringReturnsNullForMissingKey() {
        Map<String, Object> t = buildTranslations();
        assertNull(LocalizedConfigurationResolver.resolveString(t, "en", "missing"));
    }

    @Test
    void resolveStringNullTranslationsReturnsNull() {
        assertNull(LocalizedConfigurationResolver.resolveString(null, "en", "name"));
    }

    @Test
    void resolveStringListExactLanguage() {
        Map<String, Object> t = buildTranslations();
        List<String> result = LocalizedConfigurationResolver.resolveStringList(t, "it", "lore");
        assertEquals(2, result.size());
        assertEquals("&7Riga 1", result.get(0));
    }

    @Test
    void resolveStringListFallbackEnglish() {
        Map<String, Object> t = buildTranslations();
        List<String> result = LocalizedConfigurationResolver.resolveStringList(t, "es", "lore");
        assertEquals(2, result.size());
        assertTrue(result.get(0).contains("Line"));
    }

    @Test
    void resolveStringListReturnsEmptyForMissing() {
        Map<String, Object> t = buildTranslations();
        List<String> result = LocalizedConfigurationResolver.resolveStringList(t, "en", "nope");
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveTitleExact() {
        Map<String, Object> titles = new LinkedHashMap<>();
        titles.put("en", "&8Menu");
        titles.put("it", "&8Menù");
        assertEquals("&8Menù", LocalizedConfigurationResolver.resolveTitle(titles, "it"));
    }

    @Test
    void resolveTitleFallbackEnglish() {
        Map<String, Object> titles = new LinkedHashMap<>();
        titles.put("en", "&8Menu");
        assertEquals("&8Menu", LocalizedConfigurationResolver.resolveTitle(titles, "es"));
    }

    @Test
    void resolveTitleNullTitlesReturnsNull() {
        assertNull(LocalizedConfigurationResolver.resolveTitle(null, "en"));
    }

    @Test
    void translateReturnsCorrectMap() {
        Map<String, Object> t = buildTranslations();
        Map<String, ?> it = LocalizedConfigurationResolver.translate(t, "it");
        assertNotNull(it);
        assertEquals("&aCiao", it.get("name"));
    }

    @Test
    void translateFallbackToEnglish() {
        Map<String, Object> t = buildTranslations();
        Map<String, ?> es = LocalizedConfigurationResolver.translate(t, "es");
        assertNotNull(es);
        assertEquals("&aHello", es.get("name"));
    }

    @Test
    void resolveLegacyUsesTranslationsFirst() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name", "&cLegacyName");
        Map<String, Object> translations = new LinkedHashMap<>();
        Map<String, Object> it = new LinkedHashMap<>();
        it.put("name", "&aNuovoNome");
        translations.put("it", it);
        root.put("translations", translations);
        String result = LocalizedConfigurationResolver.resolveLegacy(
                root, "it", "name", "translations", "name");
        assertEquals("&aNuovoNome", result);
    }

    @Test
    void resolveLegacyFallsBackToLegacy() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name", "&cLegacyName");
        root.put("translations", new LinkedHashMap<>());
        String result = LocalizedConfigurationResolver.resolveLegacy(
                root, "es", "name", "translations", "name");
        assertEquals("&cLegacyName", result);
    }

    @Test
    void ptAndPtBrAreDistinct() {
        Map<String, Object> t = new LinkedHashMap<>();
        Map<String, Object> pt = new LinkedHashMap<>();
        pt.put("name", "Nome PT");
        Map<String, Object> ptbr = new LinkedHashMap<>();
        ptbr.put("name", "Nome BR");
        t.put("pt", pt);
        t.put("pt_br", ptbr);
        assertFalse(LocalizedConfigurationResolver.resolveString(t, "pt", "name")
                .equals(LocalizedConfigurationResolver.resolveString(t, "pt_br", "name")));
    }

    @Test
    void inputNotModified() {
        Map<String, Object> t = buildTranslations();
        LocalizedConfigurationResolver.resolveString(t, "it", "name");
        Map<String, Object> it = (Map<String, Object>) t.get("it");
        assertEquals("&aCiao", it.get("name"));
    }

    @Test
    void placeholderNotModified() {
        Map<String, Object> t = new LinkedHashMap<>();
        Map<String, Object> en = new LinkedHashMap<>();
        en.put("name", "&aHello {player}");
        t.put("en", en);
        String result = LocalizedConfigurationResolver.resolveString(t, "en", "name");
        assertTrue(result.contains("{player}"));
    }
}
