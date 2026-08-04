package it.legacynetwork.items.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CustomItemAction {
    private final CustomItemActionType type;
    private final String value;
    private final Map<String, String> translatedValues;
    private final int cooldownMillis;
    private final boolean cancelEvent;

    public CustomItemAction(CustomItemActionType type,
                            String value,
                            Map<String, String> translatedValues,
                            int cooldownMillis,
                            boolean cancelEvent) {
        this.type = type;
        this.value = value;
        this.translatedValues = translatedValues != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(translatedValues))
                : Collections.emptyMap();
        this.cooldownMillis = cooldownMillis;
        this.cancelEvent = cancelEvent;
    }

    public CustomItemActionType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getTranslatedValue(String languageCode) {
        String val = translatedValues.get(languageCode);
        if (val == null) {
            val = translatedValues.get("en");
        }
        return val != null ? val : value;
    }

    public Map<String, String> getTranslatedValues() {
        return translatedValues;
    }

    public int getCooldownMillis() {
        return cooldownMillis;
    }

    public boolean isCancelEvent() {
        return cancelEvent;
    }
}
