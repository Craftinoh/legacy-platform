package it.legacynetwork.language;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PlaceholderValues {
    private final Map<String, String> values;

    private PlaceholderValues(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(
                new LinkedHashMap<String, String>(values));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PlaceholderValues empty() {
        return builder().build();
    }

    public String apply(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public static final class Builder {
        private final Map<String, String> values = new LinkedHashMap<String, String>();

        public Builder put(String placeholder, Object value) {
            if (placeholder == null || value == null) {
                throw new IllegalArgumentException("Placeholder e valore non possono essere null");
            }
            values.put(placeholder, String.valueOf(value));
            return this;
        }

        public Builder player(String value) {
            return put("player", value);
        }

        public Builder language(String value) {
            return put("language", value);
        }

        public Builder server(String value) {
            return put("server", value);
        }

        public Builder online(int value) {
            return put("online", value);
        }

        public Builder rank(String value) {
            return put("rank", value);
        }

        public Builder website(String value) {
            return put("website", value);
        }

        public PlaceholderValues build() {
            return new PlaceholderValues(values);
        }
    }
}
