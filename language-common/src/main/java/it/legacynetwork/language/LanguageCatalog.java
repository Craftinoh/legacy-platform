package it.legacynetwork.language;

import java.util.List;

public interface LanguageCatalog {

    Language getByCode(String code);

    List<Language> getAvailable();

    List<Language> getEnabled();

    Language getFallback();
}
