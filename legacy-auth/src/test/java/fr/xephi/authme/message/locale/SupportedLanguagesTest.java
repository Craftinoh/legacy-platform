package fr.xephi.authme.message.locale;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link SupportedLanguages}.
 */
public class SupportedLanguagesTest {

    @Test
    public void shouldNormalizeLocalesOntoTheLanguagePart() {
        assertThat(SupportedLanguages.normalize("en_US"), equalTo("en"));
        assertThat(SupportedLanguages.normalize("en_GB"), equalTo("en"));
        assertThat(SupportedLanguages.normalize("it_IT"), equalTo("it"));
        assertThat(SupportedLanguages.normalize("es_MX"), equalTo("es"));
        assertThat(SupportedLanguages.normalize("fr_CA"), equalTo("fr"));
        assertThat(SupportedLanguages.normalize("pt_PT"), equalTo("pt"));
        assertThat(SupportedLanguages.normalize("ru_RU"), equalTo("ru"));
        assertThat(SupportedLanguages.normalize("uk_UA"), equalTo("uk"));
        assertThat(SupportedLanguages.normalize("sr_SP"), equalTo("sr"));
    }

    @Test
    public void shouldKeepBrazilianPortugueseDistinctFromEuropeanPortuguese() {
        assertThat(SupportedLanguages.normalize("pt_BR"), equalTo("pt_br"));
        assertThat(SupportedLanguages.normalize("pt-br"), equalTo("pt_br"));
        assertThat(SupportedLanguages.normalize("PT_BR"), equalTo("pt_br"));
        assertThat(SupportedLanguages.normalize("pt_br"), equalTo("pt_br"));
        // European Portuguese must not be swallowed by the Brazilian rule
        assertThat(SupportedLanguages.normalize("pt"), equalTo("pt"));
        assertThat(SupportedLanguages.normalize("pt_PT"), equalTo("pt"));
    }

    @Test
    public void shouldAcceptCaseHyphenAndUnderscore() {
        assertThat(SupportedLanguages.normalize("EN-us"), equalTo("en"));
        assertThat(SupportedLanguages.normalize("  It_it  "), equalTo("it"));
        assertThat(SupportedLanguages.normalize("DE"), equalTo("de"));
    }

    @Test
    public void shouldRejectUnusableValues() {
        assertThat(SupportedLanguages.normalize(null), nullValue());
        assertThat(SupportedLanguages.normalize(""), nullValue());
        assertThat(SupportedLanguages.normalize("   "), nullValue());
        assertThat(SupportedLanguages.normalize("_IT"), nullValue());
    }

    @Test
    public void shouldNormalizeUnknownLanguagesWithoutFailing() {
        // The resolver does not decide whether a language exists; the message file handler falls
        // back when there is no such translation.
        assertThat(SupportedLanguages.normalize("KLINGON"), equalTo("klingon"));
        assertThat(SupportedLanguages.isSupported("klingon"), equalTo(false));
    }

    @Test
    public void shouldListAllProductionLanguages() {
        assertThat(SupportedLanguages.getCodes(), hasItems(
            "en", "it", "es", "fr", "de", "pt", "pt_br", "nl", "pl", "ro", "hu", "cs", "sk",
            "sl", "hr", "bg", "el", "da", "sv", "no", "fi", "is", "et", "lv", "lt", "ga", "mt",
            "ru", "uk", "tr", "sr"));
        assertThat(SupportedLanguages.getCodes().size(), equalTo(31));
    }

    @Test
    public void shouldRecognizeSupportedLanguages() {
        assertThat(SupportedLanguages.isSupported("pt_br"), equalTo(true));
        assertThat(SupportedLanguages.isSupported("en"), equalTo(true));
        assertThat(SupportedLanguages.isSupported(null), equalTo(false));
        assertThat(SupportedLanguages.isSupported("xx"), equalTo(false));
    }
}
