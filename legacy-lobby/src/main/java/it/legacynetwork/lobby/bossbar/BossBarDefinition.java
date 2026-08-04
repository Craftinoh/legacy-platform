package it.legacynetwork.lobby.bossbar;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BossBarDefinition {
    private final String id;
    private final boolean enabled;
    private final int priority;
    private final int displayTicks;
    private final Map<String, String> languageTexts;
    private final BossBarProgress progress;

    public BossBarDefinition(String id,
                             boolean enabled,
                             int priority,
                             int displayTicks,
                             Map<String, String> languageTexts,
                             BossBarProgress progress) {
        this.id = id;
        this.enabled = enabled;
        this.priority = priority;
        this.displayTicks = displayTicks;
        this.languageTexts = Collections.unmodifiableMap(
                new LinkedHashMap<>(languageTexts));
        this.progress = progress;
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPriority() {
        return priority;
    }

    public int getDisplayTicks() {
        return displayTicks;
    }

    public String getText(String languageCode) {
        String text = languageTexts.get(languageCode);
        if (text == null) {
            text = languageTexts.get("en");
        }
        if (text == null && !languageTexts.isEmpty()) {
            text = languageTexts.values().iterator().next();
        }
        return text;
    }

    public boolean hasLanguage(String languageCode) {
        return languageTexts.containsKey(languageCode);
    }

    public Map<String, String> getLanguageTexts() {
        return languageTexts;
    }

    public BossBarProgress getProgress() {
        return progress;
    }
}
