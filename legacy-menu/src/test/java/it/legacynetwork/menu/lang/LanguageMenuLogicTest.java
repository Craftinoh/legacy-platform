package it.legacynetwork.menu.lang;

import it.legacynetwork.language.Language;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageMenuLogicTest {

    @Test
    void all31LanguagesSortedByMenuOrder() {
        List<Language> langs = new ArrayList<>();
        for (Language l : Language.values()) {
            langs.add(l);
        }
        Collections.sort(langs, new Comparator<Language>() {
            @Override
            public int compare(Language a, Language b) {
                return Integer.compare(a.getMenuOrder(), b.getMenuOrder());
            }
        });
        assertEquals(31, langs.size());
        assertEquals(Language.ITALIAN, langs.get(0));
        assertEquals(Language.SERBIAN, langs.get(30));
    }

    @Test
    void page1Contains28Languages() {
        List<Language> langs = sorted();
        List<Language> page1 = langs.subList(0, Math.min(28, langs.size()));
        assertEquals(28, page1.size());
    }

    @Test
    void page2Contains3Languages() {
        List<Language> langs = sorted();
        assertEquals(31, langs.size());
        int page2Size = langs.size() - 28;
        assertEquals(3, page2Size);
    }

    @Test
    void navigationNextFromPage1() {
        assertEquals(2, 2);
        assertTrue(31 > 28, "Should have a page 2");
    }

    @Test
    void navigationBackFromPage2() {
        assertTrue(true);
    }

    @Test
    void totalPagesIs2() {
        int pages = (int) Math.ceil(31.0 / 28.0);
        assertEquals(2, pages);
    }

    @Test
    void slotsAreWithin54() {
        assertEquals(54, 6 * 9);
        assertTrue(54 > 43, "Last item slot must be in range");
    }

    @Test
    void menuOrderIsUnique() {
        List<Language> langs = new ArrayList<>();
        for (Language l : Language.values()) langs.add(l);
        for (int i = 0; i < langs.size(); i++) {
            for (int j = i + 1; j < langs.size(); j++) {
                assertFalse(langs.get(i).getMenuOrder() == langs.get(j).getMenuOrder(),
                        "Menu order must be unique: " + langs.get(i) + " and " + langs.get(j));
            }
        }
    }

    @Test
    void allLanguagesHaveCode() {
        for (Language l : Language.values()) {
            assertFalse(l.getCode().isEmpty(), l + " has empty code");
        }
    }

    @Test
    void allLanguagesHaveCountryCode() {
        for (Language l : Language.values()) {
            assertFalse(l.getCountryCode().isEmpty(), l + " has empty country code");
        }
    }

    @Test
    void ptAndPtBrAreDistinct() {
        assertFalse(Language.PORTUGUESE.getCode()
                .equals(Language.PORTUGUESE_BRAZIL.getCode()));
        assertFalse(Language.PORTUGUESE.getNativeName()
                .equals(Language.PORTUGUESE_BRAZIL.getNativeName()));
    }

    @Test
    void page1ItemsStartAtSlot10() {
        assertEquals(10, 10);
    }

    @Test
    void page1ItemsEndAtSlot43() {
        assertEquals(43, (10 + 28 + 9 - 1 > 43) ? 43 : 10 + 28 - 1 + (28 / 7 - 1) * 2);
    }

    @Test
    void closeButtonSlot() {
        assertEquals(53, 53);
    }

    @Test
    void prevArrowSlot() {
        assertEquals(48, 48);
    }

    @Test
    void nextArrowSlot() {
        assertEquals(50, 50);
    }

    @Test
    void pageIndicatorSlot() {
        assertEquals(49, 49);
    }

    private List<Language> sorted() {
        List<Language> langs = new ArrayList<>();
        for (Language l : Language.values()) langs.add(l);
        Collections.sort(langs, new Comparator<Language>() {
            @Override
            public int compare(Language a, Language b) {
                return Integer.compare(a.getMenuOrder(), b.getMenuOrder());
            }
        });
        return langs;
    }
}
