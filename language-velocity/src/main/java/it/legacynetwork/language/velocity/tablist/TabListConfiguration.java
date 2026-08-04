package it.legacynetwork.language.velocity.tablist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TabListConfiguration {
    private final boolean enabled;
    private final int sendDelayMillis;
    private final int updateTicks;
    private final boolean debug;
    private final String fallbackLanguage;
    private final String fallbackServerName;
    private final Map<String, TabListLanguageSection> languages;

    public TabListConfiguration(boolean enabled,
                                 int sendDelayMillis,
                                 int updateTicks,
                                 boolean debug,
                                 String fallbackLanguage,
                                 String fallbackServerName,
                                 Map<String, TabListLanguageSection> languages) {
        this.enabled = enabled;
        this.sendDelayMillis = sendDelayMillis;
        this.updateTicks = updateTicks;
        this.debug = debug;
        this.fallbackLanguage = fallbackLanguage;
        this.fallbackServerName = fallbackServerName;
        this.languages = Collections.unmodifiableMap(new LinkedHashMap<>(languages));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSendDelayMillis() {
        return sendDelayMillis;
    }

    public int getUpdateTicks() {
        return updateTicks;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getFallbackLanguage() {
        return fallbackLanguage;
    }

    public String getFallbackServerName() {
        return fallbackServerName;
    }

    public TabListLanguageSection getLanguage(String code) {
        TabListLanguageSection section = languages.get(code);
        if (section == null) {
            section = languages.get("en");
        }
        return section;
    }

    public Map<String, TabListLanguageSection> getLanguages() {
        return languages;
    }
}
