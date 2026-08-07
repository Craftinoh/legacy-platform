package it.legacynetwork.chickenwars.scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScoreboardPlaceholderModel {
    private final Map<String, String> values =
            new LinkedHashMap<String, String>();
    private final Map<String, List<String>> lists =
            new LinkedHashMap<String, List<String>>();

    public ScoreboardPlaceholderModel value(String key, Object value) {
        values.put(key, value == null ? "" : String.valueOf(value));
        return this;
    }

    public ScoreboardPlaceholderModel lines(String key, List<String> value) {
        List<String> safe = value == null
                ? Collections.<String>emptyList() : value;
        lists.put(key, Collections.unmodifiableList(
                new ArrayList<String>(safe)));
        return this;
    }

    String render(String source) {
        String result = source == null ? "" : source;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}",
                    entry.getValue());
        }
        return result;
    }

    List<String> expansion(String source) {
        if (source == null || source.length() < 3) {
            return null;
        }
        String key = source.trim();
        if (!key.startsWith("{") || !key.endsWith("}")) {
            return null;
        }
        return lists.get(key.substring(1, key.length() - 1));
    }
}
