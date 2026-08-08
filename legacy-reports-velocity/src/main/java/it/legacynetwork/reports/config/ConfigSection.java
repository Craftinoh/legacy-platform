package it.legacynetwork.reports.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vista tollerante su una mappa YAML.
 *
 * <p>La configurazione la scrive una persona, non un serializzatore: un valore
 * mancante, un numero scritto come testo o una sezione assente non devono far
 * cadere il plugin. Ogni lettura ha quindi un valore predefinito esplicito.</p>
 */
public final class ConfigSection {

    private static final ConfigSection EMPTY =
            new ConfigSection(Collections.emptyMap());

    private final Map<String, Object> values;

    private ConfigSection(Map<String, Object> values) {
        this.values = values == null ? Collections.emptyMap() : values;
    }

    public static ConfigSection of(Map<String, Object> values) {
        return values == null ? EMPTY : new ConfigSection(values);
    }

    public static ConfigSection empty() {
        return EMPTY;
    }

    /**
     * Sottosezione, mai nulla.
     */
    public ConfigSection section(String key) {
        Object value = values.get(key);
        return value instanceof Map ? new ConfigSection(cast(value)) : EMPTY;
    }

    /**
     * Chiavi presenti, nell'ordine del file.
     */
    public Set<String> keys() {
        return Collections.unmodifiableSet(
                new LinkedHashSet<>(values.keySet()));
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public String text(String key, String fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    public int number(String key, int fallback) {
        Object value = values.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    public long duration(String key, long fallback) {
        Object value = values.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    public boolean flag(String key, boolean fallback) {
        Object value = values.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text) || "no".equalsIgnoreCase(text)) {
            return false;
        }
        return fallback;
    }

    public List<String> list(String key) {
        Object value = values.get(key);
        List<String> entries = new ArrayList<>();
        if (value instanceof Iterable) {
            for (Object element : (Iterable<?>) value) {
                if (element != null
                        && !String.valueOf(element).trim().isEmpty()) {
                    entries.add(String.valueOf(element).trim());
                }
            }
        } else if (value != null && !String.valueOf(value).trim().isEmpty()) {
            entries.add(String.valueOf(value).trim());
        }
        return Collections.unmodifiableList(entries);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object raw) {
        return (Map<String, Object>) raw;
    }
}
