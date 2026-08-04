package it.legacynetwork.language.velocity.tablist;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TabListConfigurationLoader {

    public static TabListConfiguration load(File file) {
        if (!file.exists()) {
            return disabled();
        }
        Map<String, Object> root = loadYaml(file);
        if (root == null) {
            return disabled();
        }
        boolean enabled = getBool(root, "enabled", true);
        int sendDelay = getInt(root, "send-delay.milliseconds", 250);
        int updateTicks = Math.max(1, getInt(root, "update.ticks", 40));
        boolean debug = getBool(root, "debug", false);
        String fallbackLanguage = getString(root, "fallback.language", "en");
        String fallbackServer = getString(root, "fallback.server-name", "proxy");

        Map<String, TabListLanguageSection> languages = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> langMap = (Map<String, Object>) root.get("languages");
        if (langMap != null) {
            for (Map.Entry<String, Object> entry : langMap.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> section = (Map<String, Object>) entry.getValue();
                if (section == null) {
                    continue;
                }
                List<String> header = readStringList(section, "header");
                List<String> footer = readStringList(section, "footer");
                languages.put(entry.getKey(),
                        new TabListLanguageSection(header, footer));
            }
        }
        return new TabListConfiguration(enabled, sendDelay, updateTicks, debug,
                fallbackLanguage, fallbackServer, languages);
    }

    private static TabListConfiguration disabled() {
        return new TabListConfiguration(false, 250, 40, false, "en", "proxy",
                new LinkedHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(File file) {
        try {
            Yaml yaml = new Yaml();
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8)) {
                Object loaded = yaml.load(reader);
                if (loaded instanceof Map) {
                    return (Map<String, Object>) loaded;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> readStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        if (value instanceof String) {
            List<String> result = new ArrayList<>();
            result.add((String) value);
            return result;
        }
        return new ArrayList<>();
    }

    private static boolean getBool(Map<String, Object> root, String path, boolean def) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                return def;
            }
        }
        if (current instanceof Map) {
            Object val = ((Map<?, ?>) current).get(parts[parts.length - 1]);
            if (val instanceof Boolean) {
                return (Boolean) val;
            }
        }
        return def;
    }

    private static int getInt(Map<String, Object> root, String path, int def) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                return def;
            }
        }
        if (current instanceof Map) {
            Object val = ((Map<?, ?>) current).get(parts[parts.length - 1]);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
        }
        return def;
    }

    private static String getString(Map<String, Object> root, String path, String def) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                return def;
            }
        }
        if (current instanceof Map) {
            Object val = ((Map<?, ?>) current).get(parts[parts.length - 1]);
            if (val instanceof String) {
                return (String) val;
            }
        }
        return def;
    }
}
